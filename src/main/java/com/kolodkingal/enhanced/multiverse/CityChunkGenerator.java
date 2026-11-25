package com.kolodkingal.enhanced.multiverse;

import com.mojang.serialization.Codec;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The cities of two What If universes (3.19.1), made block by block straight into the chunk (as Asgard's land is), with
 * no noise, no carvers and no features: flat ground at {@link #GROUND}, streets on a grid of city blocks {@link #BLOCK}
 * wide, and in each city block four lots, each with what its hash says stands there.
 * <ul>
 *     <li><b>{@code hydra}</b>, What If... Hydra Won?: a grey industrial city under Hydra. Concrete blocks of flats with
 *     red stripes down their faces, factories with smoking chimneys, small fenced Hydra compounds (a bunker, a red flag;
 *     not the Mind Stone outposts), Hydra skull statues in the squares, and the Citadel, Hydra's black tower, in the
 *     city block at the origin, where Red Skull and Captain Hydra wait ({@code HydraCity}). Once both have fallen the
 *     city is freed: chunks made after that are made in the colours of a free city ({@link #FREED}), and the ones made
 *     before are turned slowly round the players.</li>
 *     <li><b>{@code ultron}</b>, What If... Ultron Won?: the same city, dead. Ruined towers with jagged tops and holes in
 *     their walls, cobwebs, cracked streets with rubble and craters, the bones of the people who lived there, and no
 *     life at all but Ultron's Sentries ({@code UltronWorld}).</li>
 * </ul>
 */
public class CityChunkGenerator extends ChunkGenerator {
    public static final MapCodec<CityChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            Codec.STRING.fieldOf("city").forGetter(CityChunkGenerator::city)
    ).apply(i, i.stable(CityChunkGenerator::new)));

    public static final String HYDRA = "hydra";
    public static final String ULTRON = "ultron";

    /** The height of the ground (its top block). */
    public static final int GROUND = 64;
    /** The width of a city block with its street, of the street (pavements included), and of a lot. */
    public static final int BLOCK = 48;
    public static final int STREET = 7;
    public static final int LOT = 20;
    /** The Citadel: the city block at the origin, its tower's height, and where its door is. */
    public static final int CITADEL_HEIGHT = 92;
    public static final BlockPos CITADEL_DOOR = new BlockPos(27, GROUND + 1, 9);
    /** Where a traveller arrives: a street a little way from the Citadel. */
    public static final BlockPos ARRIVAL = new BlockPos(99, GROUND + 1, 3);

    /** Whether the Hydra city is free: set from {@code HydraCity}'s saved state when the server starts and when it is freed. */
    public static volatile boolean hydraFreed;

    /** Hydra's colours and what they become in a free city. */
    public static final Map<Block, BlockState> FREED = new HashMap<>();

    static {
        FREED.put(Blocks.RED_CONCRETE, Blocks.WHITE_CONCRETE.defaultBlockState());
        FREED.put(Blocks.RED_STAINED_GLASS, Blocks.GLASS.defaultBlockState());
        FREED.put(Blocks.GRAY_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        FREED.put(Blocks.RED_WOOL, Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
        FREED.put(Blocks.BLACK_CONCRETE, Blocks.GRAY_CONCRETE.defaultBlockState());
        FREED.put(Blocks.IRON_BARS, Blocks.AIR.defaultBlockState());
        FREED.put(Blocks.WITHER_SKELETON_SKULL, Blocks.TORCH.defaultBlockState());
        FREED.put(Blocks.CAMPFIRE, Blocks.AIR.defaultBlockState());
        FREED.put(Blocks.GRAY_CONCRETE, Blocks.GRASS_BLOCK.defaultBlockState());
        FREED.put(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.STONE_BRICKS.defaultBlockState());
        FREED.put(Blocks.BLACKSTONE, Blocks.SMOOTH_STONE.defaultBlockState());
        FREED.put(Blocks.COAL_BLOCK, Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static final EnumSet<Heightmap.Types> HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);

    private static final List<Block> HYDRA_WALLS = List.of(Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.STONE_BRICKS,
            Blocks.SMOOTH_STONE, Blocks.POLISHED_ANDESITE, Blocks.CYAN_TERRACOTTA, Blocks.GRAY_TERRACOTTA);
    private static final List<Block> RUIN_WALLS = List.of(Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE,
            Blocks.ANDESITE, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.GRAY_CONCRETE_POWDER, Blocks.TUFF);

    private final String city;

    public CityChunkGenerator(BiomeSource biomeSource, String city) {
        super(biomeSource);
        this.city = city;
    }

    public String city() {
        return city;
    }

    private boolean ruined() {
        return ULTRON.equals(city);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ---- the plan ----

    enum Kind {
        /** The road, a pavement, open ground in a lot. */
        ROAD, PAVEMENT, YARD,
        /** A building's wall, inside it (floors every four blocks), a compound's fence, a chimney, a statue, a flag pole. */
        WALL, INSIDE, FENCE, STACK, STATUE, POLE
    }

    /**
     * One column of the city.
     *
     * @param kind  what stands there
     * @param top   the highest block (the roof of a building, the ground otherwise)
     * @param wall  what the building is made of
     * @param along where along its wall a wall column is (windows go between pillars)
     * @param seed  the lot's hash (the column's own for the ruins)
     * @param lot   0 a tower, 1 a factory, 2 a compound, 3 a square, 4 the Citadel
     */
    record Column(Kind kind, int top, Block wall, int along, long seed, int lot, int x, int z) {
    }

    static long hash(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + c * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    private static int bits(long seed, int shift, int range) {
        return (int) Math.floorMod(seed >>> shift, (long) range);
    }

    Column column(int x, int z) {
        int bx = Math.floorDiv(x, BLOCK);
        int bz = Math.floorDiv(z, BLOCK);
        int lx = Math.floorMod(x, BLOCK);
        int lz = Math.floorMod(z, BLOCK);
        if (lx < STREET || lz < STREET) {
            boolean inX = lx < STREET;
            boolean inZ = lz < STREET;
            boolean pavement = inX && !inZ ? lx == 0 || lx == STREET - 1 : !inX && (lz == 0 || lz == STREET - 1);
            return new Column(pavement ? Kind.PAVEMENT : Kind.ROAD, GROUND, Blocks.STONE, 0, hash(x, z, 1), -1, x, z);
        }
        int ix = lx - STREET;
        int iz = lz - STREET;
        if (bx == 0 && bz == 0) {
            return citadel(x, z, ix, iz);
        }
        int lotX = ix < LOT ? 0 : 1;
        int lotZ = iz < LOT ? 0 : 1;
        int px = ix - lotX * (LOT + 1);
        int pz = iz - lotZ * (LOT + 1);
        long seed = hash(bx * 2L + lotX, bz * 2L + lotZ, ruined() ? 7 : 3);
        int roll = bits(seed, 3, 20);
        int lot = roll < 2 ? 2 : roll < 5 ? 1 : roll < 7 ? 3 : 0;
        if (px < 0 || pz < 0 || px >= LOT || pz >= LOT) {
            return new Column(Kind.YARD, GROUND, Blocks.STONE, 0, seed, lot, x, z);
        }
        List<Block> walls = ruined() ? RUIN_WALLS : HYDRA_WALLS;
        Block wall = walls.get(bits(seed, 8, walls.size()));
        switch (lot) {
            case 1 -> { // a factory, its chimney in a corner
                if (px >= 14 && px <= 15 && pz >= 14 && pz <= 15) {
                    return new Column(Kind.STACK, GROUND + 26 + bits(seed, 13, 8), Blocks.BRICKS, 0, seed, lot, x, z);
                }
                return building(px, pz, 1, 18, GROUND + 9, ruined() ? Blocks.CRACKED_STONE_BRICKS : Blocks.BRICKS, seed, lot, x, z);
            }
            case 2 -> { // a Hydra compound: a fence, a bunker, a flag
                if (px == 0 || pz == 0 || px == LOT - 1 || pz == LOT - 1) {
                    boolean gate = pz == 0 && px >= 8 && px <= 11;
                    return new Column(gate ? Kind.YARD : Kind.FENCE, GROUND + 3, Blocks.RED_CONCRETE, px + pz, seed, lot, x, z);
                }
                if (px == 3 && pz == 3) {
                    return new Column(Kind.POLE, GROUND + 9, Blocks.IRON_BARS, 0, seed, lot, x, z);
                }
                return building(px, pz, 6, 13, GROUND + 5, Blocks.POLISHED_BLACKSTONE_BRICKS, seed, lot, x, z);
            }
            case 3 -> { // a square with a Hydra skull on a column
                if (px >= 9 && px <= 10 && pz >= 9 && pz <= 10) {
                    return new Column(Kind.STATUE, GROUND + 6, Blocks.POLISHED_BLACKSTONE, 0, seed, lot, x, z);
                }
                return new Column(Kind.YARD, GROUND, Blocks.STONE, 0, seed, lot, x, z);
            }
            default -> {
                int height = GROUND + 12 + bits(seed, 16, 9) * 4;
                return building(px, pz, 2, 17, height, wall, seed, lot, x, z);
            }
        }
    }

    private Column building(int px, int pz, int from, int to, int top, Block wall, long seed, int lot, int x, int z) {
        if (px < from || px > to || pz < from || pz > to) {
            return new Column(Kind.YARD, GROUND, Blocks.STONE, 0, seed, lot, x, z);
        }
        if (ruined()) {
            // Jagged tops: each column broken off at its own height, lower towards the middle of a fallen wall.
            int broken = bits(hash(x, z, 11), 0, Math.max(1, (top - GROUND) / 2 + 1));
            top = Math.max(GROUND + 2, top - broken - bits(seed, 20, 6));
        }
        boolean edge = px == from || px == to || pz == from || pz == to;
        int along = px == from || px == to ? pz - from : px - from;
        return new Column(edge ? Kind.WALL : Kind.INSIDE, top, wall, along, seed, lot, x, z);
    }

    private Column citadel(int x, int z, int ix, int iz) {
        long seed = hash(0, 0, 99);
        int from = 4;
        int to = 36;
        if (ix < from || ix > to || iz < from || iz > to) {
            return new Column(Kind.YARD, GROUND, Blocks.STONE, 0, seed, 4, x, z);
        }
        // Stepped: the tower narrows twice on its way up.
        int inset = Math.min(Math.min(ix - from, to - ix), Math.min(iz - from, to - iz));
        int top = inset < 4 ? GROUND + 40 : inset < 9 ? GROUND + 66 : GROUND + CITADEL_HEIGHT;
        if (ruined()) {
            top = Math.max(GROUND + 6, top - bits(hash(x, z, 13), 0, 30));
        }
        boolean edge = inset == 0 || inset == 4 || inset == 9;
        int along = (ix - from) + (iz - from);
        return new Column(edge ? Kind.WALL : Kind.INSIDE, top, ruined() ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.BLACK_CONCRETE,
                along, seed, 4, x, z);
    }

    // ---- the blocks ----

    @Nullable
    BlockState block(Column c, int y, int minY) {
        if (y == minY) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (y > c.top() && !(y == c.top() + 1 && (c.kind() == Kind.STATUE || c.kind() == Kind.STACK))) {
            return null;
        }
        if (y < GROUND - 4) {
            return Blocks.STONE.defaultBlockState();
        }
        if (y < GROUND) {
            return Blocks.DIRT.defaultBlockState();
        }
        BlockState state = ruined() ? ruin(c, y) : hydra(c, y);
        if (state != null && !ruined() && hydraFreed) {
            BlockState free = FREED.get(state.getBlock());
            if (free != null) {
                state = y > GROUND && free.is(Blocks.GRASS_BLOCK) ? Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState() : free;
                if (state.isAir()) {
                    return null;
                }
            }
        }
        return state;
    }

    @Nullable
    private BlockState hydra(Column c, int y) {
        if (y == GROUND) {
            return switch (c.kind()) {
                case ROAD -> (Math.floorMod(c.x(), BLOCK) == STREET / 2 || Math.floorMod(c.z(), BLOCK) == STREET / 2)
                        && Math.floorMod(c.x() + c.z(), 6) < 3 ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
                case PAVEMENT -> Blocks.SMOOTH_STONE.defaultBlockState();
                case YARD -> c.lot() == 2 || c.lot() == 4 ? Blocks.GRAY_CONCRETE.defaultBlockState()
                        : bits(c.seed() ^ hash(c.x(), c.z(), 5), 0, 9) == 0 ? Blocks.COAL_BLOCK.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState();
                default -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            };
        }
        int level = y - GROUND;
        switch (c.kind()) {
            case FENCE -> {
                return c.along() % 4 == 0 ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState();
            }
            case POLE -> {
                return Blocks.IRON_BARS.defaultBlockState();
            }
            case STACK -> {
                return y == c.top() + 1 ? Blocks.CAMPFIRE.defaultBlockState() : level % 8 == 0 ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
            }
            case STATUE -> {
                return y == c.top() + 1 ? Blocks.WITHER_SKELETON_SKULL.defaultBlockState() : y == c.top() ? Blocks.RED_CONCRETE.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            }
            case INSIDE -> {
                if (y == c.top()) {
                    return c.lot() == 2 ? Blocks.BLACKSTONE.defaultBlockState() : c.wall().defaultBlockState();
                }
                return level % 4 == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : null;
            }
            case WALL -> {
                if (level <= 2 && door(c)) {
                    return null;
                }
                if (y == c.top()) {
                    return c.lot() == 2 ? Blocks.RED_CONCRETE.defaultBlockState() : c.wall().defaultBlockState();
                }
                boolean stripe = c.lot() == 4 ? c.along() % 6 == 0 : c.lot() == 0 && (c.along() == 7 || c.along() == 8);
                if (stripe) {
                    return Blocks.RED_CONCRETE.defaultBlockState();
                }
                boolean window = level % 4 >= 2 && c.along() % 3 != 0 && c.lot() != 2;
                if (window) {
                    return c.lot() == 4 ? Blocks.RED_STAINED_GLASS.defaultBlockState() : Blocks.GRAY_STAINED_GLASS.defaultBlockState();
                }
                return c.wall().defaultBlockState();
            }
            default -> {
                return null;
            }
        }
    }

    /** A door: the middle of the wall that faces the street on the low side, on the ground floor. */
    private static boolean door(Column c) {
        int lz = Math.floorMod(c.z(), BLOCK) - STREET;
        int lzInLot = lz < LOT ? lz : lz - LOT - 1;
        if (c.lot() == 4) {
            return lz == 4 && Math.abs(Math.floorMod(c.x(), BLOCK) - 27) <= 2;
        }
        int from = c.lot() == 1 ? 1 : c.lot() == 2 ? 6 : 2;
        int lx = Math.floorMod(c.x(), BLOCK) - STREET;
        int lxInLot = lx < LOT ? lx : lx - LOT - 1;
        return lzInLot == from && (lxInLot == 9 || lxInLot == 10);
    }

    @Nullable
    private BlockState ruin(Column c, int y) {
        long h = hash(c.x(), y, c.z());
        if (y == GROUND) {
            if (crater(c.x(), c.z())) {
                return Blocks.COARSE_DIRT.defaultBlockState();
            }
            return switch (c.kind()) {
                case ROAD -> bits(h, 0, 5) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : bits(h, 4, 7) == 0
                        ? Blocks.GRAVEL.defaultBlockState() : Blocks.BLACK_CONCRETE_POWDER.defaultBlockState();
                case PAVEMENT -> bits(h, 0, 3) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
                case YARD -> bits(h, 0, 3) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                default -> Blocks.COBBLESTONE.defaultBlockState();
            };
        }
        int level = y - GROUND;
        switch (c.kind()) {
            case ROAD, PAVEMENT, YARD -> {
                // Rubble, and the bones of the dead where they fell.
                long col = hash(c.x(), c.z(), 17);
                if (crater(c.x(), c.z())) {
                    return null;
                }
                if (level == 1) {
                    int r = bits(col, 0, 160);
                    return r < 3 ? Blocks.BONE_BLOCK.defaultBlockState() : r < 6 ? Blocks.SKELETON_SKULL.defaultBlockState()
                            : r < 9 ? Blocks.RED_CARPET.defaultBlockState() : r < 16 ? Blocks.COBBLESTONE.defaultBlockState()
                            : r < 20 ? Blocks.GRAVEL.defaultBlockState() : r < 22 ? Blocks.COBWEB.defaultBlockState() : null;
                }
                if (level == 2 && bits(col, 0, 160) < 3 && bits(col, 9, 2) == 0) {
                    return Blocks.SKELETON_SKULL.defaultBlockState();
                }
                return null;
            }
            case STACK -> {
                return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            }
            case STATUE -> {
                return y > c.top() ? Blocks.SKELETON_SKULL.defaultBlockState() : Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            }
            case FENCE, POLE -> {
                return level == 1 || bits(h, 0, 3) == 0 ? Blocks.IRON_BARS.defaultBlockState() : null;
            }
            case INSIDE -> {
                if (level % 4 == 0 && bits(h, 0, 4) != 0) {
                    return bits(h, 3, 9) == 0 ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
                }
                if (level % 4 == 1 && bits(h, 5, 40) == 0) {
                    return Blocks.SKELETON_SKULL.defaultBlockState();
                }
                return bits(h, 7, 14) == 0 ? Blocks.COBWEB.defaultBlockState() : null;
            }
            case WALL -> {
                if (level <= 2 && door(c)) {
                    return null;
                }
                if (bits(h, 0, 7) == 0) {
                    return null; // a hole blown through the wall
                }
                boolean window = level % 4 >= 2 && c.along() % 3 != 0;
                if (window) {
                    return bits(h, 3, 4) == 0 ? Blocks.GRAY_STAINED_GLASS_PANE.defaultBlockState() : null;
                }
                return bits(h, 6, 5) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : c.wall().defaultBlockState();
            }
            default -> {
                return null;
            }
        }
    }

    /** A crater where a bomb fell: one in some city blocks, eight blocks round. */
    static boolean crater(int x, int z) {
        int bx = Math.floorDiv(x, BLOCK);
        int bz = Math.floorDiv(z, BLOCK);
        long h = hash(bx, bz, 23);
        if (bits(h, 0, 4) != 0) {
            return false;
        }
        int cx = bx * BLOCK + bits(h, 4, BLOCK);
        int cz = bz * BLOCK + bits(h, 12, BLOCK);
        int dx = x - cx;
        int dz = z - cz;
        return dx * dx + dz * dz <= 36 + bits(h, 20, 30);
    }

    // ---- making the chunk ----

    public void fillCity(ChunkAccess chunk) {
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        for (int i = 0; i < 16; i++) {
            for (int k = 0; k < 16; k++) {
                int x = x0 + i;
                int z = z0 + k;
                Column c = column(x, z);
                int to = Math.min(maxY, c.top() + 1);
                boolean crater = ruined() && crater(x, z);
                for (int y = minY; y <= to; y++) {
                    BlockState state = crater && y > GROUND - 3 && y <= GROUND && c.kind().ordinal() <= Kind.YARD.ordinal()
                            ? (y == GROUND - 2 ? Blocks.COARSE_DIRT.defaultBlockState() : null) : block(c, y, minY);
                    if (state != null) {
                        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                        section.setBlockState(i, y & 15, k, state);
                    }
                }
                // The compound's flag: two red blocks off the pole's top.
                if (!ruined() && c.kind() == Kind.POLE) {
                    BlockState red = hydraFreed ? Blocks.LIGHT_BLUE_WOOL.defaultBlockState() : Blocks.RED_WOOL.defaultBlockState();
                    for (int f = 1; f <= 2 && i + f < 16; f++) {
                        for (int dy = 0; dy < 2; dy++) {
                            chunk.getSection(chunk.getSectionIndex(c.top() - dy)).setBlockState(i + f, (c.top() - dy) & 15, k, red);
                        }
                    }
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, HEIGHTMAPS);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        fillCity(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

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
        return GROUND - 1;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        Column c = column(x, z);
        return (ruined() && crater(x, z) && c.kind().ordinal() <= Kind.YARD.ordinal() ? GROUND - 2 : c.top()) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] column = new BlockState[height.getHeight()];
        Arrays.fill(column, Blocks.AIR.defaultBlockState());
        Column c = column(x, z);
        int minY = height.getMinBuildHeight();
        for (int y = minY; y <= Math.min(c.top() + 1, height.getMaxBuildHeight() - 1); y++) {
            BlockState state = block(c, y, minY);
            if (state != null) {
                column[y - minY] = state;
            }
        }
        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        Column c = column(pos.getX(), pos.getZ());
        info.add("City (" + city + "): " + c.kind() + " lot " + c.lot() + " top " + c.top());
    }
}
