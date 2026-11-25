package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The survivors' bunkers (3.19.1) of the zombies' Earth-2149 and the Gamma Bomb's world, the only places their survivors
 * are found ({@link WhatIfPeople}). One stands in each square of {@link #CELL} blocks (somewhere in it, the world's seed
 * decides where), on land, built with the land ({@link UniverseChunkGenerator}):
 * <ul>
 *     <li>a square fortress {@code 43} blocks across: a thick wall of stone bricks and grey concrete buttresses, a
 *     yellow and black hazard band along its top, battlements (and, in 2149, spikes against the dead), a walkway inside
 *     reached by ladders;</li>
 *     <li>a round tower at each corner, banded in the universe's colour (red in 2149, the Gamma Bomb's green), with a
 *     signal fire on top whose smoke is seen from far off;</li>
 *     <li>a great round blast door in the south wall, iron ringed with hazard stripes, an iron door in its middle opened
 *     by the button beside it on each side (the dead and the Hulks cannot open it);</li>
 *     <li>inside: a lit yard with a crop field and its water, a generator, crates, and the hall where the survivors live:
 *     beds, a crafting table, a furnace, chests of supplies, lanterns.</li>
 * </ul>
 * The land under the walls is filled, and the hills over them are cut away. Its people come when a player first comes
 * near ({@link #tick}): three or four of the universe's eight survivors, who live there (they are not taken away when no
 * one is near), and the chests are filled. A bunker that has its people has chiseled stone bricks under the lodestone in
 * the middle of its hall (that is all that is saved).
 */
public final class Bunkers {
    /** The tag of a survivor who lives in a bunker. */
    public static final String RESIDENT = "enhanced_bunker_resident";
    /** One bunker in each square of this many blocks. */
    static final int CELL = 288;
    /** Half the width of the walls' square, and of everything the bunker holds (with its towers and the ground round it). */
    static final int HALF = 21;
    static final int REACH = 27;
    /** The radius of a corner tower, its height, and how high over the yard the hills are cut away. */
    static final double TOWER = 5.0;
    static final int TOWER_TOP = 16;
    static final int CLEAR = 44;
    /** How deep the land under the walls is filled at most. */
    static final int FOUNDATION = 28;
    /** The marker in the middle of the hall's floor (dx, dz). */
    static final int MARK_X = 0;
    static final int MARK_Z = -8;
    /** How near a player comes for a bunker's people to be there. */
    static final int PEOPLE_RANGE = 72;

    /** The height of the yard of each site (worked out once), or {@link Integer#MIN_VALUE} where the site is under water. */
    private static final Map<Long, Integer> FLOORS = new ConcurrentHashMap<>();

    private Bunkers() {
    }

    // ---- where ----

    /** The middle of a cell's bunker. */
    public static BlockPos site(long seed, int universe, int cellX, int cellZ) {
        long h = CityChunkGenerator.hash(cellX, cellZ, seed ^ universe * 0x632BE59BD9B4E019L);
        int x = cellX * CELL + 48 + (int) Math.floorMod(h, (long) (CELL - 96));
        int z = cellZ * CELL + 48 + (int) Math.floorMod(h >>> 24, (long) (CELL - 96));
        return new BlockPos(x, 0, z);
    }

    public static long siteSeed(long seed, int universe, int cellX, int cellZ) {
        return CityChunkGenerator.hash(cellX * 31L + universe, cellZ, seed);
    }

    /** The height of the bunker's yard: the land at its middle, or {@link Integer#MIN_VALUE} where that is under water. */
    static int floor(ChunkGenerator generator, WorldGenLevel level, int universe, int cellX, int cellZ, BlockPos site) {
        long key = CityChunkGenerator.hash(cellX, cellZ, level.getSeed() + universe);
        Integer known = FLOORS.get(key);
        if (known != null) {
            return known;
        }
        RandomState random = level.getLevel().getChunkSource().randomState();
        int surface = generator.getBaseHeight(site.getX(), site.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, random);
        int ground = generator.getBaseHeight(site.getX(), site.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, random);
        int y = surface > ground || ground <= generator.getSeaLevel() ? Integer.MIN_VALUE : ground - 1;
        if (FLOORS.size() > 2048) {
            FLOORS.clear();
        }
        FLOORS.put(key, y);
        return y;
    }

    // ---- built with the land ----

    /** Builds the part of every bunker that stands in this chunk (as the land is decorated). */
    static void build(ChunkGenerator generator, WorldGenLevel level, ChunkAccess chunk, int universe) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        for (int cx = Math.floorDiv(minX - REACH, CELL); cx <= Math.floorDiv(maxX + REACH, CELL); cx++) {
            for (int cz = Math.floorDiv(minZ - REACH, CELL); cz <= Math.floorDiv(maxZ + REACH, CELL); cz++) {
                BlockPos site = site(level.getSeed(), universe, cx, cz);
                if (site.getX() + REACH < minX || site.getX() - REACH > maxX || site.getZ() + REACH < minZ || site.getZ() - REACH > maxZ) {
                    continue;
                }
                int y0 = floor(generator, level, universe, cx, cz, site);
                if (y0 == Integer.MIN_VALUE) {
                    continue;
                }
                buildPart(level, site, y0, universe == Multiverse.GAMMA, minX, minZ);
            }
        }
    }

    private static void buildPart(WorldGenLevel level, BlockPos site, int y0, boolean gamma, int minX, int minZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int dx = x - site.getX();
                int dz = z - site.getZ();
                if (Math.abs(dx) > REACH || Math.abs(dz) > REACH) {
                    continue;
                }
                // The land under the walls, the yard and the towers is filled.
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= HALF + 1 || towerDistance(dx, dz) <= TOWER + 0.5) {
                    for (int y = y0 - 1; y >= y0 - FOUNDATION; y--) {
                        pos.set(x, y, z);
                        BlockState there = level.getBlockState(pos);
                        if (!there.isAir() && there.getFluidState().isEmpty() && !there.canBeReplaced()) {
                            break;
                        }
                        level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    }
                }
                for (int dy = 0; dy <= CLEAR; dy++) {
                    BlockState state = plan(dx, dy, dz, gamma);
                    pos.set(x, y0 + dy, z);
                    if (state == null) {
                        continue;
                    }
                    BlockState there = level.getBlockState(pos);
                    if (state.isAir() && there.isAir() || there == state) {
                        continue;
                    }
                    level.setBlock(pos, state, 2);
                }
            }
        }
    }

    /** How far the column is from the middle of the nearest corner tower. */
    static double towerDistance(int dx, int dz) {
        double tx = dx - (dx < 0 ? -HALF : HALF);
        double tz = dz - (dz < 0 ? -HALF : HALF);
        return Math.sqrt(tx * tx + tz * tz);
    }

    private static BlockState of(Block block) {
        return block.defaultBlockState();
    }

    /**
     * What stands at a place of the bunker ({@code dx}, {@code dz} from its middle, {@code dy} over its yard): null where
     * the land is left as it is. The door is in the south (+z) wall.
     */
    @Nullable
    public static BlockState plan(int dx, int dy, int dz, boolean gamma) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        int ring = Math.max(ax, az);
        if (ring > REACH) {
            return null;
        }
        Block accent = gamma ? Blocks.LIME_CONCRETE : Blocks.RED_CONCRETE;
        Block glass = gamma ? Blocks.LIME_STAINED_GLASS : Blocks.GLASS;
        BlockState air = Blocks.AIR.defaultBlockState();
        // ---- the corner towers ----
        double td = towerDistance(dx, dz);
        if (td <= TOWER + 0.5) {
            boolean shell = td > TOWER - 1.0;
            double angle = Math.atan2(dz - (dz < 0 ? -HALF : HALF), dx - (dx < 0 ? -HALF : HALF));
            int sector = (int) Math.floor((angle + Math.PI) / (Math.PI / 6.0));
            if (dy == 0) {
                return of(Blocks.SMOOTH_STONE);
            }
            if (dy < TOWER_TOP) {
                if (!shell) {
                    return dy == 1 && td < 0.8 ? of(Blocks.LANTERN) : air;
                }
                if ((dy == 6 || dy == 7 || dy == 12 || dy == 13) && sector % 3 == 1) {
                    return of(glass);
                }
                if (dy <= 2) {
                    return of(Blocks.POLISHED_ANDESITE);
                }
                return dy % 5 == 0 ? of(accent) : of(Blocks.LIGHT_GRAY_CONCRETE);
            }
            if (dy == TOWER_TOP) {
                return td < 0.8 ? of(Blocks.HAY_BLOCK) : of(Blocks.SMOOTH_STONE);
            }
            if (dy == TOWER_TOP + 1) {
                if (td < 0.8) {
                    return of(Blocks.CAMPFIRE);
                }
                return shell && sector % 2 == 0 ? of(Blocks.STONE_BRICKS) : air;
            }
            return air;
        }
        // ---- the buttons, in the way through on each side, beside the iron door (which they open) ----
        if ((dz == HALF + 1 || dz == HALF - 1) && dx == 0 && dy == 2) {
            return button(Direction.WEST);
        }
        // ---- the blast door, proud of the south wall ----
        if (dz == HALF + 1 && ax <= 6) {
            double r = Math.sqrt(dx * dx + (dy - 4.0) * (dy - 4.0));
            if (dy >= 1 && r <= 5.4) {
                if (r > 3.6) {
                    int stripe = (int) Math.floor((Math.atan2(dy - 4.0, dx) + Math.PI) / (Math.PI / 8.0));
                    return stripe % 2 == 0 ? of(Blocks.YELLOW_CONCRETE) : of(Blocks.BLACK_CONCRETE);
                }
                return doorway(dx, dy) ? air : of(Blocks.IRON_BLOCK);
            }
        }
        // ---- the walls ----
        if ((ring == HALF || ring == HALF - 1)) {
            int along = az == ring ? ax : az;
            if (dy == 0) {
                return of(Blocks.POLISHED_ANDESITE);
            }
            if (dz > 0 && az >= HALF - 1 && doorway(dx, dy)) {
                if (dz == HALF) {
                    return Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                            .setValue(DoorBlock.HINGE, dx < 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT)
                            .setValue(DoorBlock.HALF, dy == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                }
                return air;
            }
            if (dy <= 9) {
                if (dz == HALF && ax <= 4 && Math.sqrt(dx * dx + (dy - 4.0) * (dy - 4.0)) <= 3.6) {
                    return of(Blocks.IRON_BLOCK);
                }
                if (dy == 9) {
                    return (along / 2) % 2 == 0 ? of(Blocks.YELLOW_CONCRETE) : of(Blocks.BLACK_CONCRETE);
                }
                if (dy <= 2) {
                    return of(Blocks.POLISHED_ANDESITE);
                }
                if (along % 6 == 0) {
                    return of(Blocks.GRAY_CONCRETE);
                }
                return CityChunkGenerator.hash(dx, dy, dz) % 6 == 0 ? of(Blocks.CRACKED_STONE_BRICKS) : of(Blocks.STONE_BRICKS);
            }
            if (dy == 10 && ring == HALF) {
                return along % 2 == 0 ? of(Blocks.STONE_BRICKS) : air;
            }
            if (dy == 11 && ring == HALF && !gamma && along % 2 == 0) {
                return of(Blocks.POINTED_DRIPSTONE);
            }
            return air;
        }
        // ---- outside the walls: buttresses, the ground, the way in ----
        if (ring > HALF) {
            int along = az == ring ? ax : az;
            if (ring == HALF + 1 && along % 6 == 0 && dy >= 1 && dy <= 7 && !(dz > 0 && ax <= 6)) {
                return of(Blocks.GRAY_CONCRETE);
            }
            if (dy == 0) {
                if (dz > HALF && ax <= 1) {
                    return of(Blocks.SMOOTH_STONE);
                }
                return CityChunkGenerator.hash(dx, 7, dz) % 3 == 0 ? of(Blocks.GRAVEL) : of(Blocks.COARSE_DIRT);
            }
            return air;
        }
        // ---- the walkway and its ladders ----
        if (ring == HALF - 2) {
            boolean ladder = dz == 5 && (dx == -(HALF - 2) || dx == HALF - 2);
            if (ladder && dy >= 1 && dy <= 8) {
                return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, dx < 0 ? Direction.EAST : Direction.WEST);
            }
            if (dy == 8) {
                return of(Blocks.SMOOTH_STONE);
            }
        }
        // ---- the hall ----
        if (ax <= 8 && dz >= -13 && dz <= -3) {
            return hall(dx, dy, dz, gamma, glass, accent);
        }
        // ---- the yard ----
        if (dy == 0) {
            if (dx >= 5 && dx <= 15 && dz >= 2 && dz <= 14) {
                return dz == 8 ? of(Blocks.WATER) : Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE, 7);
            }
            if (ax <= 1 && dz >= -2) {
                return of(Blocks.SMOOTH_STONE);
            }
            return Math.floorMod(dx, 5) == 0 || Math.floorMod(dz, 5) == 0 ? of(Blocks.LIGHT_GRAY_CONCRETE) : of(Blocks.GRAY_CONCRETE);
        }
        if (dy == 1 && dx >= 5 && dx <= 15 && dz >= 2 && dz <= 14 && dz != 8) {
            return gamma ? Blocks.CARROTS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, 7)
                    : Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, 7);
        }
        // The generator.
        if (dx >= -14 && dx <= -12 && dz >= 6 && dz <= 8 && dy <= 3) {
            if (dy == 3) {
                return dx == -13 && dz == 7 ? of(Blocks.LIGHTNING_ROD) : air;
            }
            return dy == 2 && dx == -13 && dz == 7 ? of(Blocks.REDSTONE_BLOCK) : of(Blocks.IRON_BLOCK);
        }
        // Crates.
        if (dy == 1 && (dx == -17 || dx == -16) && (dz == 15 || dz == 16) || dy == 2 && dx == -17 && dz == 16) {
            return (dx + dz) % 2 == 0 ? of(Blocks.BARREL) : of(Blocks.HAY_BLOCK);
        }
        // The lamps: no yard is dark enough for the dead to come out of.
        if (lamp(dx, dz)) {
            return dy <= 2 ? of(Blocks.OAK_FENCE) : dy == 3 ? of(Blocks.LANTERN) : air;
        }
        return air;
    }

    private static final int[][] LAMPS = {{-10, 0}, {10, -1}, {-10, 12}, {3, 17}, {-3, 17}, {-15, -15}, {15, -15}, {16, 17}, {-16, -4}, {-6, 6}};

    private static boolean lamp(int dx, int dz) {
        for (int[] lamp : LAMPS) {
            if (lamp[0] == dx && lamp[1] == dz) {
                return true;
            }
        }
        return false;
    }

    /** The two blocks wide, two high way through the blast door. */
    private static boolean doorway(int dx, int dy) {
        return (dx == -1 || dx == 0) && (dy == 1 || dy == 2);
    }

    private static BlockState button(Direction facing) {
        return Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, facing);
    }

    private static BlockState hall(int dx, int dy, int dz, boolean gamma, Block glass, Block accent) {
        int ax = Math.abs(dx);
        BlockState air = Blocks.AIR.defaultBlockState();
        boolean edge = ax == 8 || dz == -13 || dz == -3;
        if (dy == 0) {
            return dx == MARK_X && dz == MARK_Z ? of(Blocks.LODESTONE) : of(Blocks.POLISHED_ANDESITE);
        }
        if (dy == 6) {
            return of(Blocks.SMOOTH_STONE);
        }
        if (dy > 6) {
            return air;
        }
        if (edge) {
            if (dz == -3 && ax <= 1 && dy <= 3) {
                return air;
            }
            int along = ax == 8 ? Math.abs(dz) : ax;
            if (dy == 3 && along % 3 == 1) {
                return of(glass);
            }
            return dy == 5 ? of(accent) : of(Blocks.GRAY_CONCRETE);
        }
        if (dy == 5 && (ax == 4 && (dz == -6 || dz == -10) || dx == 0 && dz == -8)) {
            return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        }
        if (dy != 1) {
            return air;
        }
        if ((dz == -12 || dz == -11) && (ax == 3 || ax == 6)) {
            return (gamma ? Blocks.GREEN_BED : Blocks.RED_BED).defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                    .setValue(BedBlock.PART, dz == -12 ? BedPart.HEAD : BedPart.FOOT);
        }
        if (dz == -4 && dx == -7) {
            return of(Blocks.CRAFTING_TABLE);
        }
        if (dz == -4 && dx == 7) {
            return Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH);
        }
        if (dz == -8 && ax == 7) {
            return Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, dx < 0 ? Direction.EAST : Direction.WEST);
        }
        if (dz == -10 && ax == 7) {
            return of(Blocks.BARREL);
        }
        return air;
    }

    // ---- its people ----

    /** Every five seconds of a player in 2149 or the Gamma Bomb's world: the bunkers near him that have no people yet get them. */
    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int universe = level.dimension() == Multiverse.ZOMBIES_KEY ? Multiverse.ZOMBIES
                : level.dimension() == Multiverse.GAMMA_KEY ? Multiverse.GAMMA : 0;
        if (universe == 0 || player.isSpectator() || player.tickCount % 100 != 0) {
            return;
        }
        int pcx = Math.floorDiv(player.getBlockX(), CELL);
        int pcz = Math.floorDiv(player.getBlockZ(), CELL);
        for (int cx = pcx - 1; cx <= pcx + 1; cx++) {
            for (int cz = pcz - 1; cz <= pcz + 1; cz++) {
                BlockPos site = site(level.getSeed(), universe, cx, cz);
                double ddx = site.getX() - player.getX();
                double ddz = site.getZ() - player.getZ();
                if (ddx * ddx + ddz * ddz > PEOPLE_RANGE * PEOPLE_RANGE) {
                    continue;
                }
                BlockPos mark = new BlockPos(site.getX() + MARK_X, 0, site.getZ() + MARK_Z);
                if (!level.isLoaded(mark)) {
                    continue;
                }
                BlockPos lodestone = find(level, mark);
                if (lodestone != null && !level.getBlockState(lodestone.below()).is(Blocks.CHISELED_STONE_BRICKS)) {
                    populate(level, lodestone, universe, siteSeed(level.getSeed(), universe, cx, cz));
                }
            }
        }
    }

    /** The hall's lodestone under its roof at a column, or null where no bunker stands. */
    @Nullable
    public static BlockPos find(ServerLevel level, BlockPos column) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(column.getX(), top, column.getZ());
        for (int i = 0; i < 12; i++) {
            pos.move(Direction.DOWN);
            if (level.getBlockState(pos).is(Blocks.LODESTONE)) {
                return pos.immutable();
            }
        }
        return null;
    }

    /**
     * The bunker's people come (three or four of the universe's eight survivors, each once) and its chests are filled;
     * it is marked as done. {@code lodestone}: the marker in its hall.
     */
    public static List<AsgardianEntity> populate(ServerLevel level, BlockPos lodestone, int universe, long seed) {
        Random random = new Random(seed);
        int first = universe == Multiverse.GAMMA ? WhatIfPeople.GAMMA_SURVIVORS : WhatIfPeople.ZOMBIE_SURVIVORS;
        List<Integer> persons = new ArrayList<>();
        for (int i = 0; i < WhatIfPeople.SURVIVORS_EACH; i++) {
            persons.add(first + i);
        }
        Collections.shuffle(persons, random);
        int count = 3 + random.nextInt(2);
        // Where they stand: (dx, dz) from the bunker's middle; the lodestone is at (MARK_X, MARK_Z).
        int[][] places = {{-4, -6}, {4, -9}, {-2, 6}, {9, 0}};
        int y = lodestone.getY() + 1;
        List<AsgardianEntity> people = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double x = lodestone.getX() - MARK_X + places[i][0] + 0.5;
            double z = lodestone.getZ() - MARK_Z + places[i][1] + 0.5;
            AsgardianEntity npc = WhatIfPeople.person(level, Role.SURVIVOR, persons.get(i), x, y, z, RESIDENT);
            npc.setPersistenceRequired();
            level.addFreshEntity(npc);
            people.add(npc);
        }
        for (int side : new int[]{-7, 7}) {
            BlockPos chest = lodestone.offset(side - MARK_X, 1, 0);
            BlockEntity entity = level.getBlockEntity(chest);
            if (entity instanceof Container container) {
                List<ItemStack> loot = universe == Multiverse.GAMMA
                        ? List.of(new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.GOLDEN_CARROT, 4), new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.BREAD, 6))
                        : List.of(new ItemStack(Items.BREAD, 8), new ItemStack(Items.ARROW, 16), new ItemStack(Items.TORCH, 12), new ItemStack(Items.COOKED_BEEF, 4));
                for (int slot = 0; slot < loot.size() && slot < container.getContainerSize(); slot++) {
                    container.setItem(slot * 3 + (side < 0 ? 0 : 1), loot.get(slot).copy());
                }
            }
        }
        level.setBlock(lodestone.below(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
        return people;
    }
}
