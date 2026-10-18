package com.kolodkingal.enhanced.asgard;

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
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The chunk generator of Asgard, {@code enhanced:asgard} (registered in {@code ModChunkGenerators}).
 *
 * <p><b>The 3.5 skeleton's placeholder</b>: a void with one flat platform at the Observatory (radius
 * {@link #PLATFORM_RADIUS} round {@link CityPlan#ARRIVAL}, blocks at y 72 and 73, so a traveller stands at y 74): enough for
 * the Bifrost and the gate to be tested. Lane L1 replaces the body of this class with the realm (the floating land, the sea,
 * the city of {@code CityPlan}); the codec and its id stay.
 */
public class AsgardChunkGenerator extends ChunkGenerator {
    public static final MapCodec<AsgardChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
    ).apply(i, i.stable(AsgardChunkGenerator::new)));

    /** The placeholder platform: its radius, and its two layers. */
    public static final int PLATFORM_RADIUS = 16;
    public static final int PLATFORM_BOTTOM = 72;
    public static final int PLATFORM_TOP = 73;

    public AsgardChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** Whether the placeholder platform covers the column ({@code x}, {@code z}). */
    public static boolean onPlatform(int x, int z) {
        double dx = x + 0.5 - CityPlan.ARRIVAL.x;
        double dz = z + 0.5 - CityPlan.ARRIVAL.z;
        return dx * dx + dz * dz <= PLATFORM_RADIUS * PLATFORM_RADIUS;
    }

    private static BlockState layer(int y, int x, int z) {
        boolean ring = Math.abs(Math.hypot(x + 0.5 - CityPlan.ARRIVAL.x, z + 0.5 - CityPlan.ARRIVAL.z) - (PLATFORM_RADIUS - 1)) < 1.0;
        return y == PLATFORM_TOP ? (ring ? Blocks.GOLD_BLOCK : Blocks.SMOOTH_QUARTZ).defaultBlockState()
                : Blocks.QUARTZ_BLOCK.defaultBlockState();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!onPlatform(x0 + x, z0 + z)) {
                    continue;
                }
                for (int y = PLATFORM_BOTTOM; y <= PLATFORM_TOP; y++) {
                    BlockState state = layer(y, x0 + x, z0 + z);
                    chunk.setBlockState(at.set(x, y, z), state, false);
                    ocean.update(x, y, z, state);
                    surface.update(x, y, z, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

    /** Nothing of the biome's (it has nothing) and no structures: the city is placed by the generator itself (lane L1). */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
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
        return onPlatform(x, z) ? PLATFORM_TOP + 1 : level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] column = new BlockState[height.getHeight()];
        Arrays.fill(column, Blocks.AIR.defaultBlockState());
        if (onPlatform(x, z)) {
            for (int y = PLATFORM_BOTTOM; y <= PLATFORM_TOP; y++) {
                column[y - height.getMinBuildHeight()] = layer(y, x, z);
            }
        }
        return new NoiseColumn(height.getMinBuildHeight(), column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Asgard (placeholder platform)");
    }
}
