package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.build.CityTemplates;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The Hall of Feasts (3.7.2, Eilon's "fix Asgard's dining hall"): its seats, the feast at dusk, and the mending of a
 * hall built before 3.7.2.
 *
 * <p>What was wrong with 3.5's hall (its template is kept as {@link #LEGACY} for the mending): the boards of the long
 * tables were bottom slabs a block and a half over the floor, chest high for whoever sat on the half-block benches, on
 * fence posts whose arms reached out to nothing; the cakes, candles and pots on them floated half a block over the
 * boards; the high table was as high and had no seat at all; the long tables ran the hall's whole length, so that from
 * the doors the far side and the high table were a long walk round their ends (too long for the Asgardians'
 * pathfinding); the twelve hanging lanterns hung from the air a block under the ceiling; two banners hid windows; the
 * kitchen's door looked at the hall's blank back wall. And nobody dined: Volstagg ate standing wherever he happened to
 * be (in the aisle, on a bench), the hall's 128 benches were no seats for the Asgardians, and the Asgardians'
 * pathfinding could take them over the tables. Now the tables are a sitter's height (a top slab board a block over the
 * floor, legs of stripped spruce) with their dishes standing on them, each long table is in two halves with a cross
 * aisle from the middle door to the high table, which has six benches behind it facing the hall, the lanterns hang from
 * the ceiling, two doors lead to the kitchen; Volstagg sits at his place at the high table to eat, and at dusk ({@link
 * #FEAST_START} to {@link #FEAST_END}) the Warriors Three and Sif come to the high table and dine with him, facing the
 * hall, until the night ({@code AsgardianGoals.Dine}); nobody walks on a table ({@link #overTable}, the Asgardians'
 * pathfinding).
 *
 * <p>A city built before 3.7.2 keeps its chunks: the first time the hall's chunks are all loaded, {@link #tick} mends
 * it once ({@link #mend}): every block in which the two templates differ is set to the new one where the world still
 * has the old one (a block a player changed is left alone), as {@code SchoolRepair} does for the Mutant School.
 */
public final class FeastHall {
    /** The hall of 3.5 to 3.7.1, kept to mend a hall built from it. */
    public static final ResourceLocation LEGACY = Enhanced.id("asgard_legacy/feast_hall_3_5");
    /** The feast at dusk, in ticks of the day: from sunset until the first stars. */
    public static final long FEAST_START = 11000L;
    public static final long FEAST_END = 13500L;
    /** The persistent tag that sends an Asgardian to dine at once (the tests; {@link #invite}). */
    public static final String INVITED = "enhanced:dine_now";
    /** Outside the middle front door, where a guest coming from far off arrives when nobody sees him. */
    public static final BlockPos DOOR = new BlockPos(72, 68, 92);
    /** The guests of the high table, by seat (its six benches, north to south): two places are free. */
    private static final Role[] HIGH_TABLE = {null, Role.FANDRAL, Role.VOLSTAGG, Role.SIF, Role.HOGUN, null};

    // The template's tables and benches (local, unturned: front south; see SCRATCH's feast_hall.py).
    private static final int[] TABLES_Z = {7, 11, 19, 23};
    /** Each long table in two halves (the template's x), the cross aisle between them. */
    private static final int[][] HALVES = {{5, 17}, {23, 35}};
    private static final int HIGH_TABLE_Z = 3;

    private static volatile CityPlan.Piece piece;
    private static volatile List<CityPlan.Bench> seats;

    /**
     * What a mending did: blocks set to the new hall, blocks already right, blocks a player had changed (left alone).
     */
    public record Mend(int repaired, int alreadyRight, int skipped) {
    }

    /** The hall's piece in the plan. */
    public static CityPlan.Piece piece() {
        CityPlan.Piece p = piece;
        if (p == null) {
            for (CityPlan.Piece candidate : CityPlan.pieces()) {
                if (candidate.template().equals("feast_hall")) {
                    p = candidate;
                    break;
                }
            }
            piece = Objects.requireNonNull(p, "the plan has a Hall of Feasts");
        }
        return p;
    }

    /** The box the hall fills. */
    public static BoundingBox box() {
        return piece().box();
    }

    /** Whether {@code pos} is inside the hall (its walls included). */
    public static boolean inside(BlockPos pos) {
        return box().isInside(pos);
    }

    /**
     * Every seat of the hall, as the template puts its benches: the six of the high table first (north to south), then
     * the long tables' 112. Each is a bench with its table in front, the way its sitter faces.
     */
    public static List<CityPlan.Bench> seats() {
        List<CityPlan.Bench> s = seats;
        if (s == null) {
            CityPlan.Piece p = piece();
            List<CityPlan.Bench> out = new ArrayList<>();
            for (int x = 15; x <= 25; x += 2) {
                out.add(seat(p, x, HIGH_TABLE_Z - 1, Direction.SOUTH));
            }
            for (int tz : TABLES_Z) {
                for (int[] half : HALVES) {
                    for (int x = half[0]; x <= half[1]; x += 2) {
                        out.add(seat(p, x, tz - 1, Direction.SOUTH));
                        out.add(seat(p, x, tz + 1, Direction.NORTH));
                    }
                }
            }
            s = Collections.unmodifiableList(out);
            seats = s;
        }
        return s;
    }

    private static CityPlan.Bench seat(CityPlan.Piece p, int x, int z, Direction facing) {
        return new CityPlan.Bench(p.toWorld(new BlockPos(x, 1, z)), p.rotation().rotate(p.mirror().mirror(facing)));
    }

    /** The six seats of the high table. */
    public static List<CityPlan.Bench> highTable() {
        return seats().subList(0, HIGH_TABLE.length);
    }

    /** The place of a guest of the high table (Volstagg, Fandral, Sif, Hogun), or null. */
    @Nullable
    public static CityPlan.Bench seatOf(Role role) {
        for (int i = 0; i < HIGH_TABLE.length; i++) {
            if (HIGH_TABLE[i] == role) {
                return highTable().get(i);
            }
        }
        return null;
    }

    /** Whether {@code role} dines at the high table at dusk (Volstagg eats there at any hour too). */
    public static boolean guest(Role role) {
        return seatOf(role) != null;
    }

    /** Whether the feast is on at {@code dayTime} (dusk). */
    public static boolean feastAt(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= FEAST_START && t < FEAST_END;
    }

    public static boolean feastNow(Level level) {
        return feastAt(level.getDayTime());
    }

    /** The ticks of the feast left at {@code dayTime} (0 when it is not on). */
    public static int feastLeft(long dayTime) {
        return feastAt(dayTime) ? (int) (FEAST_END - Math.floorMod(dayTime, 24000L)) : 0;
    }

    /**
     * Where a resident of Asgard whose home is {@code home} should be at {@code dayTime}: the hall for a guest of the
     * feast while it is on, else his home.
     */
    public static BlockPos homeAt(Role role, BlockPos home, long dayTime) {
        return guest(role) && feastAt(dayTime) ? CityPlan.FEAST_HALL : home;
    }

    /** Where one coming home to {@code home} from far off, unseen, is put: the hall's front door for the hall. */
    public static BlockPos arrival(BlockPos home) {
        return home.equals(CityPlan.FEAST_HALL) ? DOOR : home;
    }

    /**
     * A dining table's board or leg: a top spruce slab or stripped spruce wood (nothing else of the city is made of
     * them).
     */
    public static boolean isTable(BlockState state) {
        return state.is(Blocks.SPRUCE_SLAB) && state.getValue(SlabBlock.TYPE) == SlabType.TOP
                || state.is(Blocks.STRIPPED_SPRUCE_WOOD);
    }

    /**
     * Whether a mob standing at (x, y, z) would stand on a table, or on a dish on one: the Asgardians' pathfinding
     * never goes there.
     */
    public static boolean overTable(BlockGetter level, int x, int y, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, y - 1, z);
        return isTable(level.getBlockState(at)) || isTable(level.getBlockState(at.setY(y - 2)));
    }

    /** Whether {@code bench} is a seat at a table: an Asgard bench with a table in front, the way it faces. */
    public static boolean diningSeat(BlockGetter level, BlockPos bench) {
        BlockState state = level.getBlockState(bench);
        return state.is(ModBlocks.ASGARD_BENCH.get())
                && isTable(level.getBlockState(bench.relative(state.getValue(HorizontalDirectionalBlock.FACING))));
    }

    /** Sends an Asgardian to his seat at the table now (his goal takes it on its next look; the tests use it). */
    public static void invite(net.minecraft.world.entity.Entity npc) {
        ModAttachments.PERSISTENT_DATA.get(npc).putBoolean(INVITED, true);
    }

    // ---- mending a hall built before 3.7.2 ----

    /** Once, when the whole hall is loaded and not mended yet (Asgard's level tick, every 100 ticks). */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 100L == 37L && AsgardDimension.is(level)) {
            mendOnce(level);
        }
    }

    /**
     * Mends the hall if it is not mended yet and all of it is loaded, and records that it is; returns what was done, or
     * null (mended before, or not all loaded).
     */
    @Nullable
    public static Mend mendOnce(ServerLevel level) {
        AsgardState state = AsgardState.get(level.getServer());
        if (state.feastHallMended() || !loaded(level)) {
            return null;
        }
        Mend done = mend(level);
        state.setFeastHallMended(true);
        if (done.repaired() > 0 || done.skipped() > 0) {
            Enhanced.LOGGER.info("Asgard: the Hall of Feasts is mended ({} blocks set, {} already right, {} left as a "
                    + "player made them)", done.repaired(), done.alreadyRight(), done.skipped());
        }
        return done;
    }

    /** Whether every chunk of the hall is loaded. */
    public static boolean loaded(ServerLevel level) {
        BoundingBox b = box();
        for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
            for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Mends the hall where it is loaded: every block in which the legacy template and the new one differ is set to the
     * new block where the world still holds the legacy one (or is already right); others are left alone.
     */
    public static Mend mend(ServerLevel level) {
        Optional<StructureTemplate> before = level.getStructureManager().get(LEGACY);
        Optional<StructureTemplate> after = level.getStructureManager().get(piece().id());
        if (before.isEmpty() || after.isEmpty()) {
            Enhanced.LOGGER.warn("Asgard: the Hall of Feasts cannot be mended: a template is missing");
            return new Mend(0, 0, 0);
        }
        Diff diff = diff(before.get(), after.get());
        int repaired = 0;
        int right = 0;
        int skipped = 0;
        for (BlockPos pos : diff.positions()) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState was = diff.was(pos);
            BlockState now = diff.now(pos);
            BlockState in = level.getBlockState(pos);
            if (same(in, now)) {
                right++;
            } else if (!same(in, was)) {
                skipped++;
            } else {
                level.setBlock(pos, now, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                CompoundTag data = diff.data(pos);
                BlockEntity entity = data == null ? null : level.getBlockEntity(pos);
                if (entity != null) {
                    entity.loadWithComponents(data, level.registryAccess());
                    entity.setChanged();
                    level.sendBlockUpdated(pos, now, now, Block.UPDATE_CLIENTS);
                }
                repaired++;
            }
        }
        return new Mend(repaired, right, skipped);
    }

    /** The two halls in the world, where they differ. */
    public record Diff(Map<BlockPos, BlockState> before, Map<BlockPos, BlockState> after, Map<BlockPos,
            CompoundTag> afterData,
                       Set<BlockPos> positions) {
        public BlockState was(BlockPos pos) {
            return before.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        public BlockState now(BlockPos pos) {
            return after.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Nullable
        public CompoundTag data(BlockPos pos) {
            return afterData.get(pos);
        }
    }

    /** Where the legacy hall and the new one differ, in the world (turned as the plan's piece). */
    public static Diff diff(StructureTemplate before, StructureTemplate after) {
        CityPlan.Piece p = piece();
        CityTemplates.Placed a = CityTemplates.place(p, before.save(new CompoundTag()));
        CityTemplates.Placed b = CityTemplates.place(p, after.save(new CompoundTag()));
        Map<BlockPos, BlockState> was = new HashMap<>();
        Map<BlockPos, CompoundTag> wasData = new HashMap<>();
        for (int i = 0; i < a.states().length; i++) {
            BlockPos pos = new BlockPos(a.xs()[i], a.ys()[i], a.zs()[i]);
            was.put(pos, a.states()[i]);
            if (a.data()[i] != null) {
                wasData.put(pos, a.data()[i]);
            }
        }
        Map<BlockPos, BlockState> now = new HashMap<>();
        Map<BlockPos, CompoundTag> nowData = new HashMap<>();
        for (int i = 0; i < b.states().length; i++) {
            BlockPos pos = new BlockPos(b.xs()[i], b.ys()[i], b.zs()[i]);
            now.put(pos, b.states()[i]);
            if (b.data()[i] != null) {
                nowData.put(pos, b.data()[i]);
            }
        }
        Set<BlockPos> all = new LinkedHashSet<>(was.keySet());
        all.addAll(now.keySet());
        Set<BlockPos> differ = new LinkedHashSet<>();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : all) {
            if (!was.getOrDefault(pos, air).equals(now.getOrDefault(pos, air)) || !Objects.equals(wasData.get(pos),
                    nowData.get(pos))) {
                differ.add(pos);
            }
        }
        return new Diff(was, now, nowData, differ);
    }

    /**
     * Whether the world's block is the template's: the same state, or for a block that joins its neighbours (a fence, a
     * wall, a pane, stairs) the same block (the world's joined it to what is round it).
     */
    public static boolean same(BlockState world, BlockState template) {
        Block block = template.getBlock();
        if (block instanceof CrossCollisionBlock || block instanceof WallBlock || block instanceof StairBlock
                || block instanceof FenceGateBlock) {
            return world.is(block);
        }
        return world.equals(template);
    }

    private FeastHall() {
    }
}
