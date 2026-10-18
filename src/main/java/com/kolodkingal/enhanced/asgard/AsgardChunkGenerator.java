package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.asgard.build.Palette;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The chunk generator of Asgard, {@code enhanced:asgard} (registered in {@code ModChunkGenerators}; the codec has only
 * its biome source). The land is {@link AsgardTerrain}'s (filled column by column straight into the chunk's sections);
 * the city is {@link CityBuild}'s, placed in {@link #applyBiomeDecoration} (the feature step): builders, templates, the
 * fixed blocks, each chunk on its own. No carvers, no structures, no mobs, nothing of the world's seed: Asgard is the
 * same in every world.
 */
public class AsgardChunkGenerator extends ChunkGenerator {
    public static final MapCodec<AsgardChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
    ).apply(i, i.stable(AsgardChunkGenerator::new)));

    /**
     * The skeleton's placeholder platform had this radius round the landing; the Observatory now stands there, and
     * nothing is built further than this south of the socket (it looks out over the void).
     */
    public static final int PLATFORM_RADIUS = 16;

    /** Time spent generating (nanoseconds): the land, the city, the worst city chunk; and how many chunks. */
    public static final AtomicLong LAND_NANOS = new AtomicLong();
    public static final AtomicLong CITY_NANOS = new AtomicLong();
    public static final AtomicLong CHUNKS = new AtomicLong();
    public static final AtomicLong WORST_NANOS = new AtomicLong();

    private static final EnumSet<Heightmap.Types> HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public AsgardChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ---- the land ----

    /**
     * The block of a column at height y (null: air). {@code steep}: how much higher or lower the steepest neighbour is
     * (a steep face of the wild land is bare stone). Pure: the tests and the base columns use it too.
     */
    @Nullable
    public static BlockState block(AsgardTerrain.Column c, int x, int y, int z, int steep) {
        switch (c.kind()) {
            case VOID -> {
                return null;
            }
            case FALLS -> {
                return y <= c.top() && y >= c.bottom() ? Palette.falls() : null;
            }
            case LIP -> {
                if (y > c.top() || y < c.bottom()) {
                    return null;
                }
                return y >= c.top() - 2 ? Palette.bricks() : y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
            }
            default -> {
            }
        }
        if (y > c.top()) {
            return y <= c.waterTop() ? Blocks.WATER.defaultBlockState() : null;
        }
        if (y < c.bottom()) {
            return null;
        }
        int depth = c.top() - y;
        if (y < 0) {
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        if (c.kind() == AsgardTerrain.Kind.SEA) {
            return depth < 2 ? Blocks.SAND.defaultBlockState() : depth < 4 ? Blocks.GRAVEL.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        CityPlan.Pad pad = c.pad();
        if (pad != null) {
            if (depth == 0) {
                return switch (pad.surface()) {
                    case PAVED -> Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0 ? Palette.bricks() : Palette.polished();
                    case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
                    case SAND -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                    case QUAY -> Blocks.STONE_BRICKS.defaultBlockState();
                };
            }
            if (pad.surface() == CityPlan.Surface.GRASS && depth < 3) {
                return Blocks.DIRT.defaultBlockState();
            }
            return depth < 8 ? Palette.stone() : Blocks.STONE.defaultBlockState();
        }
        if (c.top() < CityPlan.SEA_LEVEL) {
            return depth < 2 ? Blocks.SAND.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (steep > 3) {
            return depth < 1 && steep < 6 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        return depth == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : depth < 4 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    /** How steep the land is at the cached column (i, k): the largest difference to a neighbour's top. */
    private static int steep(AsgardTerrain.Column[] cols, int i, int k) {
        AsgardTerrain.Column c = cols[i * 18 + k];
        int most = 0;
        for (int[] s : SIDES) {
            AsgardTerrain.Column n = cols[(i + s[0]) * 18 + k + s[1]];
            most = Math.max(most, n.solid() ? Math.abs(n.top() - c.top()) : 8);
        }
        return most;
    }

    /** Fills the chunk with the land of Asgard (no city), straight into its sections. */
    public static void fillLand(ChunkAccess chunk) {
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        AsgardTerrain.Column[] cols = new AsgardTerrain.Column[18 * 18];
        for (int i = 0; i < 18; i++) {
            for (int k = 0; k < 18; k++) {
                cols[i * 18 + k] = AsgardTerrain.column(x0 + i - 1, z0 + k - 1);
            }
        }
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        for (int i = 1; i <= 16; i++) {
            for (int k = 1; k <= 16; k++) {
                AsgardTerrain.Column c = cols[i * 18 + k];
                if (c.kind() == AsgardTerrain.Kind.VOID) {
                    continue;
                }
                int x = x0 + i - 1;
                int z = z0 + k - 1;
                int steep = c.kind() == AsgardTerrain.Kind.LAND && c.pad() == null ? steep(cols, i, k) : 0;
                int from = Math.max(minY, c.bottom());
                int to = Math.min(maxY, Math.max(c.top(), c.waterTop()));
                for (int y = from; y <= to; y++) {
                    BlockState state = block(c, x, y, z, steep);
                    if (state != null) {
                        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                        section.setBlockState(x & 15, y & 15, z & 15, state);
                    }
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, HEIGHTMAPS);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        long start = System.nanoTime();
        fillLand(chunk);
        LAND_NANOS.addAndGet(System.nanoTime() - start);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

    /** The city (nothing of the biome's: it has nothing; no structures): {@link CityBuild#place}. */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        long start = System.nanoTime();
        StructureTemplateManager templates = level.getServer() == null ? null : level.getServer().getStructureManager();
        CityBuild.place(chunk, templates);
        long spent = System.nanoTime() - start;
        CITY_NANOS.addAndGet(spent);
        CHUNKS.incrementAndGet();
        WORST_NANOS.accumulateAndGet(spent, Math::max);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return CityPlan.SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        AsgardTerrain.Column c = AsgardTerrain.column(x, z);
        if (c.kind() == AsgardTerrain.Kind.VOID) {
            return level.getMinBuildHeight();
        }
        int top = c.top();
        if (type.isOpaque().test(Blocks.WATER.defaultBlockState()) && c.waterTop() > top) {
            top = c.waterTop();
        }
        return top + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] column = new BlockState[height.getHeight()];
        Arrays.fill(column, Blocks.AIR.defaultBlockState());
        AsgardTerrain.Column c = AsgardTerrain.column(x, z);
        if (c.kind() != AsgardTerrain.Kind.VOID) {
            for (int y = Math.max(height.getMinBuildHeight(), c.bottom()); y <= Math.max(c.top(), c.waterTop()); y++) {
                BlockState state = block(c, x, y, z, 0);
                if (state != null && y < height.getMaxBuildHeight()) {
                    column[y - height.getMinBuildHeight()] = state;
                }
            }
        }
        return new NoiseColumn(height.getMinBuildHeight(), column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        AsgardTerrain.Column c = AsgardTerrain.column(pos.getX(), pos.getZ());
        info.add("Asgard: " + c.kind() + (c.pad() == null ? "" : " (" + c.pad().name() + ")") + " top " + c.top());
    }
}
