package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a plot of Vision's mind (3.12) around its origin {@code O} (ground: the grass at O.y - 1, feet at O.y): an island
 * of 110 by 100 blocks, and in its middle Vision's house of three floors (31 by 23), with an elevator in its south-east
 * corner that also goes down to two floors under the ground; a little mausoleum to the south-west.
 *
 * <ul>
 * <li>ground floor (feet O.y): the hall (Dum-E), the living room, the kitchen with its fridge (E.D.I.T.H.), the cinema
 * and its projector, F.R.I.D.A.Y. across from the cinema's door, the elevator;</li>
 * <li>upper floor (+6): Ultron's room, with its balcony, its televisions, the hats to try on and his gift;</li>
 * <li>top floor (+12): the riddle robots and, behind their gate, the library;</li>
 * <li>below (-8): the little television room where Ultron sits;</li>
 * <li>bottom (-18): a long corridor of candles to the 1930s workshop of the Human Torch.</li>
 * </ul>
 * All positions are relative to {@code O}; {@link #people} says where the mind's people stand.
 */
public final class MindBuilder {
    public static final int HALF_X = 15;
    public static final int HALF_Z = 11;
    /** Feet levels of the floors, relative to the origin. */
    public static final int GROUND = 0;
    public static final int UPPER = 6;
    public static final int TOP = 12;
    public static final int BELOW = -8;
    public static final int BOTTOM = -18;
    /** Where the player stands in the elevator, on every floor (x, z relative). */
    static final int LIFT_X = 12;
    static final int LIFT_Z = 5;

    record Spot(MindNpcEntity.Role role, Vec3 at, float yaw) {
    }

    private final ServerLevel level;
    private final BlockPos o;

    private MindBuilder(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.o = origin;
    }

    /** Where a visitor arrives: on the path north of the front door, facing it. */
    public static BlockPos arrival(BlockPos origin) {
        return origin.offset(0, 0, -20);
    }

    /** Where the elevator leaves the player on a floor ({@link #GROUND} ... {@link #BOTTOM}). */
    public static BlockPos liftStop(BlockPos origin, int floor) {
        return origin.offset(LIFT_X, floor, LIFT_Z);
    }

    /** Where the corridor of the bottom floor begins (the elevator's door). */
    public static BlockPos bottomArrival(BlockPos origin) {
        return origin.offset(LIFT_X, BOTTOM, LIFT_Z);
    }

    /** The cinema's screen: the bottom-left block of the painting the projector shows (on the west wall, facing east). */
    public static BlockPos screen(BlockPos origin) {
        return origin.offset(-13, GROUND + 1, 6);
    }

    /** The Human Torch's armchair, where he sits once his flames are out. */
    public static Vec3 armchair(BlockPos origin) {
        return Vec3.atBottomCenterOf(origin.offset(-17, BOTTOM, 10));
    }

    /** Where Dr. Horton appears: at his easel, by the painting. */
    public static Vec3 easel(BlockPos origin) {
        return Vec3.atBottomCenterOf(origin.offset(-19, BOTTOM, 2));
    }

    /** The Human Torch's place, in the middle of the workshop. */
    public static Vec3 torch(BlockPos origin) {
        return Vec3.atBottomCenterOf(origin.offset(-15, BOTTOM, 5));
    }

    /** The mind's people (the ones still there for this player). */
    static List<Spot> people(BlockPos o, MindProgress progress) {
        List<Spot> out = new ArrayList<>();
        out.add(new Spot(MindNpcEntity.Role.DUM_E, Vec3.atBottomCenterOf(o.offset(2, GROUND, -8)), 180.0F));
        out.add(new Spot(MindNpcEntity.Role.EDITH, Vec3.atBottomCenterOf(o.offset(8, GROUND, -7)), 0.0F));
        out.add(new Spot(MindNpcEntity.Role.FRIDAY, Vec3.atBottomCenterOf(o.offset(3, GROUND, 5)), 90.0F));
        out.add(new Spot(MindNpcEntity.Role.ROBOT, Vec3.atBottomCenterOf(o.offset(-11, TOP, 4)), 0.0F));
        out.add(new Spot(MindNpcEntity.Role.ROBOT, Vec3.atBottomCenterOf(o.offset(-8, TOP, 4)), 0.0F));
        out.add(new Spot(MindNpcEntity.Role.ROBOT, Vec3.atBottomCenterOf(o.offset(-2, TOP, 4)), 0.0F));
        if (!progress.torchDone()) {
            out.add(new Spot(MindNpcEntity.Role.ULTRON, Vec3.atBottomCenterOf(o.offset(6, BELOW, 5)), 90.0F));
            out.add(new Spot(MindNpcEntity.Role.TORCH, torch(o), 90.0F));
        }
        return out;
    }

    /** Builds the plot if it is not built yet, and brings it to the player's progress (the gate, the floors below). */
    static void ensure(ServerLevel level, BlockPos origin, MindProgress progress) {
        MindBuilder b = new MindBuilder(level, origin);
        level.getChunk(origin);
        if (!level.getBlockState(origin.offset(0, -40, 0)).is(Blocks.BEDROCK)
                || !level.getBlockState(origin.offset(1, -40, 0)).is(Blocks.LODESTONE)) {
            // (3.12.1: a house of 3.12, built before the house took its look from the series, is built again)
            for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null,
                    new net.minecraft.world.phys.AABB(origin).inflate(20, 40, 16),
                    e -> e instanceof ArmorStand || e instanceof ItemFrame)) {
                e.discard();
            }
            b.build();
        }
        if (progress.gateOpen()) {
            b.openGate();
        }
        if (progress.torchDone()) {
            b.removeBelow();
        }
    }

    static void openGate(ServerLevel level, BlockPos origin) {
        new MindBuilder(level, origin).openGate();
    }

    static void removeBelow(ServerLevel level, BlockPos origin) {
        new MindBuilder(level, origin).removeBelow();
    }

    // ---- block helpers (relative to the origin) ----

    private BlockPos at(int x, int y, int z) {
        return o.offset(x, y, z);
    }

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(at(x, y, z), state, 2 | 16);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    /** A box's shell (walls, floor and ceiling), its inside air. */
    private void room(int x1, int y1, int z1, int x2, int y2, int z2, BlockState wall, BlockState floor, BlockState ceiling) {
        fill(x1, y1, z1, x2, y2, z2, wall);
        fill(x1, y1, z1, x2, y1, z2, floor);
        fill(x1, y2, z1, x2, y2, z2, ceiling);
        fill(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1, Blocks.AIR.defaultBlockState());
    }

    private static BlockState stairs(BlockState stairs, Direction facing) {
        return stairs.setValue(StairBlock.FACING, facing);
    }

    private static BlockState facing(BlockState state, Direction dir) {
        return state.setValue(HorizontalDirectionalBlock.FACING, dir);
    }

    private void door(int x, int y, int z, Direction facing) {
        BlockState d = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing);
        set(x, y, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private void opening(int x, int y, int z, int dx, int dz, int height) {
        fill(x, y, z, x + dx, y + height - 1, z + dz, Blocks.AIR.defaultBlockState());
    }

    private void lantern(int x, int y, int z) {
        set(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private void sign(int x, int y, int z, Direction facing, String... keys) {
        set(x, y, z, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        if (level.getBlockEntity(at(x, y, z)) instanceof SignBlockEntity sign) {
            Component[] lines = new Component[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = i < keys.length ? Component.translatable(keys[i]) : Component.empty();
            }
            sign.setText(new SignText(lines, lines, DyeColor.BLACK, false), true);
            sign.setWaxed(true);
        }
    }

    private void barrel(int x, int y, int z, Direction facing, ItemStack... items) {
        set(x, y, z, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, facing));
        if (level.getBlockEntity(at(x, y, z)) instanceof BarrelBlockEntity barrel) {
            for (int i = 0; i < items.length; i++) {
                barrel.setItem(i, items[i]);
            }
        }
    }

    private void candles(int x, int y, int z, int count, boolean white) {
        BlockState c = (white ? Blocks.WHITE_CANDLE : Blocks.YELLOW_CANDLE).defaultBlockState()
                .setValue(CandleBlock.CANDLES, Math.max(1, Math.min(4, count))).setValue(CandleBlock.LIT, true);
        set(x, y, z, c);
    }

    // ---- the plot ----

    private void build() {
        island();
        house();
        ground();
        upper();
        top();
        below();
        bottom();
        mausoleum();
        set(0, -40, 0, Blocks.BEDROCK.defaultBlockState()); // (built)
        set(1, -40, 0, Blocks.LODESTONE.defaultBlockState()); // (in the look of 3.12.2)
    }

    private void island() {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = -55; x <= 54; x++) {
            for (int z = -50; z <= 49; z++) {
                // a rounded island: its edge falls away below
                double ex = x / 55.0;
                double ez = z / 50.0;
                double edge = ex * ex + ez * ez;
                if (edge > 1.0) {
                    continue;
                }
                int depth = (int) Math.round(6 + 14 * (1.0 - edge));
                set(x, -1, z, grass);
                fill(x, -4, z, x, -2, z, dirt);
                fill(x, -4 - depth, z, x, -5, z, stone);
            }
        }
        // the rock under the house deep enough for the floors below
        fill(-24, -24, -13, 17, -5, 14, stone);
        // the front garden: a white path to the door between hedges and lamps, a balustrade with a gate, a few trees
        fill(-1, -1, -34, 1, -1, -16, Blocks.POLISHED_DIORITE.defaultBlockState());
        fill(-2, -1, -34, -2, -1, -16, Blocks.SMOOTH_STONE.defaultBlockState());
        fill(2, -1, -34, 2, -1, -16, Blocks.SMOOTH_STONE.defaultBlockState());
        BlockState hedge = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        for (int z = -32; z <= -18; z += 2) {
            set(-3, 0, z, hedge);
            set(3, 0, z, hedge);
        }
        for (int z : new int[] {-30, -22}) {
            for (int x : new int[] {-4, 4}) {
                fill(x, 0, z, x, 2, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
                set(x, 3, z, Blocks.LANTERN.defaultBlockState());
            }
        }
        for (int x = -24; x <= 24; x++) {
            if (Math.abs(x) > 2) {
                set(x, 0, -36, Blocks.DIORITE_WALL.defaultBlockState());
            }
        }
        for (int x : new int[] {-3, 3}) {
            fill(x, 0, -36, x, 1, -36, Blocks.QUARTZ_PILLAR.defaultBlockState());
            set(x, 2, -36, Blocks.LANTERN.defaultBlockState());
        }
        RandomSource random = RandomSource.create(o.asLong());
        int[][] trees = {{-14, -26}, {14, -27}, {-38, 8}, {36, 14}, {-20, 34}, {20, 36}, {40, -12}, {-42, -14}, {-28, -26}, {28, -24}};
        for (int[] t : trees) {
            tree(t[0], t[1], 4 + random.nextInt(3));
        }
    }

    /** A round, full oak, as the trees on the lawn round the house in the series. */
    private void tree(int x, int z, int height) {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        int top = height + 2;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = height - 2; dy <= top + 1; dy++) {
                    double ry = (dy - (height + 0.5)) / 2.6;
                    if (dx * dx / 9.0 + dz * dz / 9.0 + ry * ry <= 1.0) {
                        set(x + dx, dy, z + dz, leaves);
                    }
                }
            }
        }
        fill(x, 0, z, x, height, z, Blocks.OAK_LOG.defaultBlockState());
    }

    /** The height of the house's walls: the three floors of the rooms and a fourth under the roof. */
    private static final int EAVES = 23;

    /**
     * The house's outside, after the house of the series: a tall pale Victorian house of four floors under a red
     * mansard roof with dormers; an eight-sided bay on its front left from the ground, a wider one on its front right
     * from the first floor on brackets; a porch of pillars at the door carrying Ultron's balcony, a bay over it; a low
     * wing to the right with a great arched window and a balustrade; a round tower on its west side; and, rising from
     * the roof at the back right, a round turret with a red cone and a weathervane.
     */
    private void house() {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState wall = Blocks.CALCITE.defaultBlockState();
        BlockState trim = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState floor = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        // (an older house's roof and whatever stood round it)
        fill(-HALF_X - 6, EAVES + 1, -HALF_Z - 6, HALF_X + 12, 44, HALF_Z + 6, air);
        fill(-HALF_X - 6, 0, -HALF_Z - 6, HALF_X + 12, EAVES, -HALF_Z - 1, air);
        fill(HALF_X + 1, 0, -HALF_Z, HALF_X + 12, EAVES, HALF_Z + 4, air);
        fill(-HALF_X - 6, 0, -HALF_Z, -HALF_X - 1, EAVES, HALF_Z + 4, air);
        fill(-HALF_X, -1, -HALF_Z, HALF_X, EAVES, HALF_Z, wall);
        // the plinth, the corners, the bands between the floors and the cornice
        ring(-1, Blocks.POLISHED_ANDESITE.defaultBlockState());
        for (int y : new int[] {5, 11, 17, EAVES}) {
            ring(y, trim);
        }
        for (int sx : new int[] {-HALF_X, HALF_X}) {
            for (int sz : new int[] {-HALF_Z, HALF_Z}) {
                fill(sx, 0, sz, sx, EAVES - 1, sz, pillar);
            }
        }
        cornice(EAVES + 1, -HALF_X - 1, -HALF_Z - 1, HALF_X + 1, HALF_Z + 1);
        // the rooms' floors (the fourth, under the roof, is an empty attic) and their windows
        for (int f : new int[] {GROUND, UPPER, TOP, 18}) {
            fill(-HALF_X + 1, f, -HALF_Z + 1, HALF_X - 1, f + 4, HALF_Z - 1, air);
            fill(-HALF_X + 1, f - 1, -HALF_Z + 1, HALF_X - 1, f - 1, HALF_Z - 1, floor);
            for (int x = -HALF_X + 2; x <= HALF_X - 2; x += 3) {
                if (f != GROUND || Math.abs(x) > 2) {
                    window(x, f, -HALF_Z, true);
                }
                window(x, f, HALF_Z, true);
            }
            for (int z = -HALF_Z + 3; z <= HALF_Z - 3; z += 4) {
                window(-HALF_X, f, z, false);
                window(HALF_X, f, z, false);
            }
            if (f != 18) {
                for (int x = -10; x <= 10; x += 10) {
                    lantern(x, f + 4, -5);
                    lantern(x, f + 4, 5);
                }
            }
        }
        // the bays: left from the ground, right (wider) from the first floor on brackets
        bay(-10, 2, -1);
        bay(9, 3, UPPER);
        // the porch: pillars before the door carrying the balcony of Ultron's room
        for (int x : new int[] {-3, 3}) {
            fill(x, 0, -HALF_Z - 3, x, 4, -HALF_Z - 3, pillar);
            set(x, -1, -HALF_Z - 3, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        }
        fill(-3, 5, -HALF_Z - 3, 3, 5, -HALF_Z - 1, trim);
        fill(-3, GROUND - 1, -HALF_Z - 3, 3, GROUND - 1, -HALF_Z - 1, Blocks.POLISHED_DIORITE.defaultBlockState());
        for (int x = -2; x <= 2; x++) {
            set(x, GROUND - 1, -HALF_Z - 4, stairs(Blocks.QUARTZ_STAIRS.defaultBlockState(), Direction.SOUTH));
        }
        door(0, GROUND, -HALF_Z, Direction.SOUTH);
        door(-1, GROUND, -HALF_Z, Direction.SOUTH);
        fill(-2, GROUND, -HALF_Z, -2, GROUND + 2, -HALF_Z, trim);
        fill(1, GROUND, -HALF_Z, 1, GROUND + 2, -HALF_Z, trim);
        fill(-2, GROUND + 3, -HALF_Z, 1, GROUND + 3, -HALF_Z, trim);
        lantern(-2, GROUND + 4, -HALF_Z - 2);
        lantern(2, GROUND + 4, -HALF_Z - 2);
        // the middle bay over the balcony, up to the roof, its parapet stepped
        fill(-3, TOP, -HALF_Z - 1, 3, EAVES, -HALF_Z - 1, wall);
        for (int f : new int[] {TOP, 18}) {
            fill(-1, f + 1, -HALF_Z - 1, 1, f + 3, -HALF_Z - 1, Blocks.GLASS_PANE.defaultBlockState());
            fill(-1, f + 4, -HALF_Z - 1, 1, f + 4, -HALF_Z - 1, trim);
            fill(-1, f, -HALF_Z, 1, f + 3, -HALF_Z, air);
        }
        for (int y : new int[] {17, EAVES}) {
            fill(-3, y, -HALF_Z - 1, 3, y, -HALF_Z - 1, trim);
        }
        fill(-3, EAVES + 1, -HALF_Z - 1, 3, EAVES + 1, -HALF_Z - 1, trim);
        fill(-2, EAVES + 2, -HALF_Z - 1, 2, EAVES + 2, -HALF_Z - 1, trim);
        set(0, EAVES + 3, -HALF_Z - 1, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        wing();
        roundTower(-HALF_X - 2, -2);
        roof();
        turret(8, 4);
        // the elevator's shaft: a column of dark glass in the south-east corner, a panel on every floor
        for (int f : new int[] {GROUND, UPPER, TOP}) {
            liftNook(f);
        }
    }

    /** A band of {@code state} round the house's outside at height {@code y}. */
    private void ring(int y, BlockState state) {
        fill(-HALF_X, y, -HALF_Z, HALF_X, y, -HALF_Z, state);
        fill(-HALF_X, y, HALF_Z, HALF_X, y, HALF_Z, state);
        fill(-HALF_X, y, -HALF_Z, -HALF_X, y, HALF_Z, state);
        fill(HALF_X, y, -HALF_Z, HALF_X, y, HALF_Z, state);
    }

    /** An overhanging cornice of upside-down quartz stairs round a rectangle, at height {@code y}. */
    private void cornice(int y, int x1, int z1, int x2, int z2) {
        BlockState st = Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
        for (int x = x1; x <= x2; x++) {
            set(x, y, z1, st.setValue(StairBlock.FACING, Direction.SOUTH));
            set(x, y, z2, st.setValue(StairBlock.FACING, Direction.NORTH));
        }
        for (int z = z1 + 1; z < z2; z++) {
            set(x1, y, z, st.setValue(StairBlock.FACING, Direction.EAST));
            set(x2, y, z, st.setValue(StairBlock.FACING, Direction.WEST));
        }
    }

    /** A window two high in a white frame (sill and lintel) in the wall at {@code (x, z)}. */
    private void window(int x, int f, int z, boolean alongX) {
        BlockState trim = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        fill(x, f + 1, z, x, f + 2, z, Blocks.GLASS_PANE.defaultBlockState());
        set(x, f, z, trim);
        set(x, f + 3, z, trim);
    }

    /**
     * An eight-sided bay before the front wall, centred on {@code sx}, {@code half} blocks to each side, from height
     * {@code from} (the ground, or the first floor on a row of brackets) up to the eaves, open to the rooms behind it,
     * with a cornice and a flat top.
     */
    private void bay(int sx, int half, int from) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState wall = Blocks.CALCITE.defaultBlockState();
        BlockState trim = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        int front = -HALF_Z - 3;
        fill(sx - half, from, front, sx + half, EAVES, -HALF_Z - 1, wall);
        fill(sx - half, from, front, sx - half, EAVES, front, air);
        fill(sx + half, from, front, sx + half, EAVES, front, air);
        for (int y : new int[] {from, 5, 11, 17, EAVES}) {
            if (y < from) {
                continue;
            }
            BlockState band = y == -1 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : trim;
            fill(sx - half + 1, y, front, sx + half - 1, y, front, band);
            fill(sx - half, y, front + 1, sx - half, y, -HALF_Z - 1, band);
            fill(sx + half, y, front + 1, sx + half, y, -HALF_Z - 1, band);
        }
        if (from > 0) {
            // the brackets under it
            BlockState bracket = Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
            for (int x = sx - half + 1; x <= sx + half - 1; x++) {
                set(x, from - 1, front, bracket.setValue(StairBlock.FACING, Direction.SOUTH));
            }
            set(sx - half, from - 1, front + 1, bracket.setValue(StairBlock.FACING, Direction.EAST));
            set(sx + half, from - 1, front + 1, bracket.setValue(StairBlock.FACING, Direction.WEST));
        }
        for (int f : new int[] {GROUND, UPPER, TOP, 18}) {
            if (f < from) {
                continue;
            }
            fill(sx - half + 1, f, front + 1, sx + half - 1, f + 4, -HALF_Z, air);
            fill(sx - half + 1, f - 1, front + 1, sx + half - 1, f - 1, -HALF_Z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            for (int x = sx - half + 1; x <= sx + half - 1; x += 2) {
                fill(x, f + 1, front, x, f + 3, front, pane);
            }
            fill(sx - half, f + 1, front + 1, sx - half, f + 3, front + 1, pane);
            fill(sx + half, f + 1, front + 1, sx + half, f + 3, front + 1, pane);
        }
        cornice(EAVES + 1, sx - half, front, sx + half, -HALF_Z - 1);
        fill(sx - half + 1, EAVES + 1, front + 1, sx + half - 1, EAVES + 1, -HALF_Z - 1, trim);
    }

    /** The low wing to the right: one floor, a great arched window, a flat roof with a balustrade. */
    private void wing() {
        BlockState wall = Blocks.CALCITE.defaultBlockState();
        BlockState trim = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        int x1 = HALF_X + 1;
        int x2 = HALF_X + 10;
        int z1 = -HALF_Z + 2;
        int z2 = HALF_Z - 4;
        fill(x1, -1, z1, x2, 6, z2, wall);
        fill(x1, 0, z1 + 1, x2 - 1, 4, z2 - 1, Blocks.AIR.defaultBlockState());
        fill(x1, -1, z1, x2, -1, z2, Blocks.POLISHED_ANDESITE.defaultBlockState());
        fill(x1, 5, z1, x2, 5, z1, trim);
        fill(x2, 5, z1, x2, 5, z2, trim);
        // the arched window on its front
        int cx = (x1 + x2) / 2;
        fill(cx - 2, 1, z1, cx + 2, 3, z1, Blocks.GLASS_PANE.defaultBlockState());
        fill(cx - 1, 4, z1, cx + 1, 4, z1, Blocks.GLASS_PANE.defaultBlockState());
        set(cx - 2, 4, z1, stairs(Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.HALF,
                net.minecraft.world.level.block.state.properties.Half.TOP), Direction.WEST));
        set(cx + 2, 4, z1, stairs(Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.HALF,
                net.minecraft.world.level.block.state.properties.Half.TOP), Direction.EAST));
        set(cx, 5, z1, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        fill(x2, 1, z1 + 2, x2, 3, z2 - 2, Blocks.GLASS_PANE.defaultBlockState());
        // the balustrade on its roof
        for (int x = x1; x <= x2; x++) {
            set(x, 7, z1, Blocks.DIORITE_WALL.defaultBlockState());
            set(x, 7, z2, Blocks.DIORITE_WALL.defaultBlockState());
        }
        for (int z = z1; z <= z2; z++) {
            set(x2, 7, z, Blocks.DIORITE_WALL.defaultBlockState());
        }
    }

    /** A round tower against the west wall, with windows all the way up and a cornice under the eaves. */
    private void roundTower(int cx, int cz) {
        BlockState wall = Blocks.CALCITE.defaultBlockState();
        BlockState trim = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 3.2 || cx + dx >= -HALF_X) {
                    continue;
                }
                boolean rim = d > 2.2;
                for (int y = -1; y <= EAVES; y++) {
                    if (!rim) {
                        set(cx + dx, y, cz + dz, y == -1 || y == EAVES ? wall : Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    boolean band = y == 5 || y == 11 || y == 17 || y == EAVES;
                    boolean glass = !band && (y % 6 == 1 || y % 6 == 2 || y % 6 == 3) && (dx == 0 || dz == 0) && y > 0;
                    set(cx + dx, y, cz + dz, band ? trim : glass ? Blocks.GLASS_PANE.defaultBlockState() : wall);
                }
                set(cx + dx, EAVES + 1, cz + dz, trim);
            }
        }
    }

    /** The mansard roof: steep red sides with lit dormers, a flat top with an iron railing round it. */
    private void roof() {
        BlockState brick = Blocks.BRICKS.defaultBlockState();
        BlockState st = Blocks.BRICK_STAIRS.defaultBlockState();
        int base = EAVES + 1;
        int[] inset = {0, 0, 1, 2, 3};
        for (int i = 0; i < inset.length; i++) {
            int y = base + i;
            int x1 = -HALF_X + inset[i];
            int x2 = HALF_X - inset[i];
            int z1 = -HALF_Z + inset[i];
            int z2 = HALF_Z - inset[i];
            fill(x1, y, z1, x2, y, z2, brick);
            if (i == 1 || i == inset.length - 1) {
                continue;
            }
            for (int x = x1; x <= x2; x++) {
                set(x, y, z1, stairs(st, Direction.SOUTH));
                set(x, y, z2, stairs(st, Direction.NORTH));
            }
            for (int z = z1 + 1; z < z2; z++) {
                set(x1, y, z, stairs(st, Direction.EAST));
                set(x2, y, z, stairs(st, Direction.WEST));
            }
        }
        int top = base + inset.length - 1;
        int t = inset[inset.length - 1];
        for (int x = -HALF_X + t; x <= HALF_X - t; x++) {
            set(x, top + 1, -HALF_Z + t, Blocks.IRON_BARS.defaultBlockState());
            set(x, top + 1, HALF_Z - t, Blocks.IRON_BARS.defaultBlockState());
        }
        for (int z = -HALF_Z + t; z <= HALF_Z - t; z++) {
            set(-HALF_X + t, top + 1, z, Blocks.IRON_BARS.defaultBlockState());
            set(HALF_X - t, top + 1, z, Blocks.IRON_BARS.defaultBlockState());
        }
        // two dormers on the front (as in the series), more behind, lit from within
        for (int[] d : new int[][] {{-6, -HALF_Z}, {5, -HALF_Z}, {-6, HALF_Z}, {6, HALF_Z}}) {
            int x = d[0];
            int z = d[1];
            int in = z < 0 ? 1 : -1;
            fill(x - 1, base + 1, z, x + 1, base + 2, z, Blocks.SMOOTH_QUARTZ.defaultBlockState());
            fill(x, base + 1, z, x, base + 2, z, Blocks.GLASS_PANE.defaultBlockState());
            fill(x, base + 1, z + in, x, base + 2, z + in, Blocks.GLOWSTONE.defaultBlockState());
            fill(x - 1, base + 3, z, x + 1, base + 3, z, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        }
    }

    /** The turret at the back right of the roof: round and white, windows lit, a red cone and a weathervane. */
    private void turret(int cx, int cz) {
        BlockState wall = Blocks.CALCITE.defaultBlockState();
        BlockState trim = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int base = EAVES + 2;
        int top = base + 10;
        fill(cx - 2, base, cz - 2, cx + 2, top, cz + 2, wall);
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                fill(cx + dx, base, cz + dz, cx + dx, top, cz + dz, air);
            }
        }
        fill(cx - 1, base + 1, cz - 1, cx + 1, top - 1, cz + 1, air);
        set(cx, base + 1, cz, Blocks.GLOWSTONE.defaultBlockState());
        for (int y : new int[] {base + 5, base + 6}) {
            set(cx, y, cz - 2, Blocks.GLASS_PANE.defaultBlockState());
            set(cx, y, cz + 2, Blocks.GLASS_PANE.defaultBlockState());
            set(cx - 2, y, cz, Blocks.GLASS_PANE.defaultBlockState());
            set(cx + 2, y, cz, Blocks.GLASS_PANE.defaultBlockState());
        }
        for (int y : new int[] {base + 4, top - 1}) {
            fill(cx - 1, y, cz - 2, cx + 1, y, cz - 2, trim);
            fill(cx - 1, y, cz + 2, cx + 1, y, cz + 2, trim);
            fill(cx - 2, y, cz - 1, cx - 2, y, cz + 1, trim);
            fill(cx + 2, y, cz - 1, cx + 2, y, cz + 1, trim);
        }
        cornice(top, cx - 2, cz - 2, cx + 2, cz + 2);
        // the red cone, steep
        BlockState st = Blocks.BRICK_STAIRS.defaultBlockState();
        int y = top + 1;
        fill(cx - 2, y, cz - 2, cx + 2, y, cz + 2, Blocks.BRICKS.defaultBlockState());
        for (int d = -2; d <= 2; d++) {
            set(cx + d, y, cz - 2, stairs(st, Direction.SOUTH));
            set(cx + d, y, cz + 2, stairs(st, Direction.NORTH));
            set(cx - 2, y, cz + d, stairs(st, Direction.EAST));
            set(cx + 2, y, cz + d, stairs(st, Direction.WEST));
        }
        fill(cx - 1, y + 1, cz - 1, cx + 1, y + 2, cz + 1, Blocks.BRICKS.defaultBlockState());
        for (int d = -1; d <= 1; d++) {
            set(cx + d, y + 2, cz - 1, stairs(st, Direction.SOUTH));
            set(cx + d, y + 2, cz + 1, stairs(st, Direction.NORTH));
            set(cx - 1, y + 2, cz + d, stairs(st, Direction.EAST));
            set(cx + 1, y + 2, cz + d, stairs(st, Direction.WEST));
        }
        set(cx, y + 3, cz, Blocks.BRICKS.defaultBlockState());
        set(cx, y + 4, cz, Blocks.BRICK_WALL.defaultBlockState());
        // the weathervane: an iron cross
        set(cx, y + 5, cz, Blocks.IRON_BARS.defaultBlockState());
        set(cx, y + 6, cz, Blocks.IRON_BARS.defaultBlockState());
        set(cx - 1, y + 6, cz, Blocks.IRON_BARS.defaultBlockState());
        set(cx + 1, y + 6, cz, Blocks.IRON_BARS.defaultBlockState());
        set(cx, y + 7, cz, Blocks.LIGHTNING_ROD.defaultBlockState());
    }

    /** The elevator on a floor: the stop, its panel on the east wall, a sign. */
    private void liftNook(int f) {
        BlockState frame = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        fill(LIFT_X - 1, f, LIFT_Z - 2, HALF_X - 1, f + 3, LIFT_Z + 2, frame);
        fill(LIFT_X - 1, f, LIFT_Z - 1, HALF_X - 2, f + 2, LIFT_Z + 1, Blocks.AIR.defaultBlockState());
        fill(LIFT_X - 1, f - 1, LIFT_Z - 1, HALF_X - 2, f - 1, LIFT_Z + 1, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        set(HALF_X - 1, f + 1, LIFT_Z, facing(ModBlocks.MIND_ELEVATOR.get().defaultBlockState(), Direction.WEST));
        set(HALF_X - 1, f + 3, LIFT_Z, Blocks.SEA_LANTERN.defaultBlockState());
    }

    private void ground() {
        int f = GROUND;
        BlockState inner = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        BlockState plaster = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        // the wall between the front rooms and the back ones (z = 0), and the hall's two side walls
        fill(-HALF_X + 1, f, 0, HALF_X - 1, f + 4, 0, plaster);
        fill(-5, f, -HALF_Z + 1, -5, f + 4, -1, plaster);
        fill(5, f, -HALF_Z + 1, 5, f + 4, -1, plaster);
        opening(-5, f, -6, 0, 1, 3);
        opening(5, f, -6, 0, 1, 3);
        opening(-1, f, 0, 2, 0, 3);
        // the hall: a red carpet from the door, a chandelier
        fill(-1, f, -HALF_Z + 1, 0, f, -1, Blocks.RED_CARPET.defaultBlockState());
        set(0, f + 4, -6, Blocks.CHAIN.defaultBlockState());
        // the living room (west): sofas facing a fireplace, books, a rug
        fill(-14, f, -10, -6, f, -2, Blocks.BROWN_CARPET.defaultBlockState());
        fill(-14, f, -7, -14, f + 2, -4, Blocks.BRICKS.defaultBlockState());
        set(-13, f, -6, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        set(-13, f, -5, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        fill(-13, f + 1, -6, -13, f + 1, -5, Blocks.IRON_BARS.defaultBlockState());
        for (int z = -7; z <= -4; z++) {
            set(-9, f, z, stairs(Blocks.DARK_OAK_STAIRS.defaultBlockState(), Direction.EAST));
        }
        fill(-14, f, -10, -14, f + 2, -9, Blocks.BOOKSHELF.defaultBlockState());
        fill(-14, f, -2, -14, f + 2, -1, Blocks.BOOKSHELF.defaultBlockState());
        set(-7, f, -9, Blocks.POTTED_FERN.defaultBlockState());
        // the kitchen (east): counters, a stove, a sink, a table, the fridge (milk inside)
        for (int x = 7; x <= 14; x++) {
            set(x, f, -10, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        set(9, f, -10, facing(Blocks.SMOKER.defaultBlockState(), Direction.SOUTH));
        set(11, f, -10, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        fill(13, f + 1, -10, 13, f + 1, -10, Blocks.IRON_BLOCK.defaultBlockState());
        barrel(13, f, -10, Direction.SOUTH, new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.APPLE, 6), new ItemStack(Items.BREAD, 4));
        set(14, f, -10, Blocks.IRON_BLOCK.defaultBlockState());
        set(14, f + 1, -10, Blocks.IRON_BLOCK.defaultBlockState());
        sign(13, f + 2, -9, Direction.SOUTH, "sign.enhanced.mind.fridge");
        set(10, f, -5, Blocks.DARK_OAK_FENCE.defaultBlockState());
        set(10, f + 1, -5, Blocks.AIR.defaultBlockState());
        set(9, f, -5, stairs(Blocks.DARK_OAK_STAIRS.defaultBlockState(), Direction.EAST));
        set(11, f, -5, stairs(Blocks.DARK_OAK_STAIRS.defaultBlockState(), Direction.WEST));
        // the cinema (south-west): dark walls, a white screen on the west, rows of seats, the projector at the back
        // (as in the series: red walls, red seats, a little amber lamp at the end of every row)
        room(-14, f - 1, 0, -2, f + 5, 10, Blocks.RED_WOOL.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                Blocks.RED_TERRACOTTA.defaultBlockState());
        fill(-13, f, 1, -3, f, 9, Blocks.RED_CARPET.defaultBlockState());
        fill(-14, f, 2, -14, f + 3, 8, Blocks.WHITE_CONCRETE.defaultBlockState());
        for (int x : new int[] {-10, -8, -6}) {
            for (int z = 3; z <= 8; z++) {
                set(x, f, z, stairs(Blocks.RED_NETHER_BRICK_STAIRS.defaultBlockState(), Direction.EAST));
            }
        }
        for (int x : new int[] {-10, -8, -6}) {
            for (int z : new int[] {2, 9}) {
                set(x, f, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
                set(x, f + 1, z, Blocks.ORANGE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1)
                        .setValue(CandleBlock.LIT, true));
            }
        }
        set(-4, f, 6, Blocks.DARK_OAK_FENCE.defaultBlockState());
        set(-4, f + 1, 6, facing(ModBlocks.CINEMA_PROJECTOR.get().defaultBlockState(), Direction.WEST));
        barrel(-4, f, 9, Direction.UP, new ItemStack(ModItems.POPCORN.get(), 32));
        sign(-4, f + 1, 9, Direction.WEST, "sign.enhanced.mind.popcorn");
        opening(-2, f, 5, 0, 0, 2);
        door(-2, f, 5, Direction.EAST);
        // the back hall: F.R.I.D.A.Y. across from the cinema's door, the way to the elevator
        fill(-1, f, 1, 4, f, 10, Blocks.BLUE_CARPET.defaultBlockState());
        fill(5, f, 1, 5, f + 4, HALF_Z - 1, plaster);
        opening(5, f, 5, 0, 0, 3);
    }

    private void upper() {
        int f = UPPER;
        BlockState plaster = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        // the elevator's room is walled off from Ultron's room, with a doorway
        fill(5, f, -HALF_Z + 1, 5, f + 4, HALF_Z - 1, plaster);
        opening(5, f, 5, 0, 0, 3);
        // Ultron's room: grey and red, a long rug
        fill(-14, f, -10, 4, f, 10, Blocks.GRAY_CARPET.defaultBlockState());
        fill(-6, f, -4, 0, f, 4, Blocks.RED_CARPET.defaultBlockState());
        // brick walls, as Ultron's attic in the series (the windows left as they are)
        for (int x = -14; x <= 4; x++) {
            for (int y = f; y <= f + 4; y++) {
                for (int z : new int[] {-HALF_Z + 1, HALF_Z - 1}) {
                    boolean window = level.getBlockState(at(x, y, z < 0 ? -HALF_Z : HALF_Z)).is(Blocks.GLASS_PANE)
                            || level.getBlockState(at(x, y, z < 0 ? -HALF_Z : HALF_Z)).isAir();
                    if (!window && !(z < 0 && Math.abs(x) <= 2) && level.getBlockState(at(x, y, z)).isAir()) {
                        set(x, y, z, Blocks.BRICKS.defaultBlockState());
                    }
                }
            }
        }
        // the balcony: a door shaped like the Mind Stone, its glass blue, to the terrace over the portico
        BlockState rim = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState gem = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState();
        // (rows from the floor up: how far the stone reaches to each side; the doorway is the two lowest rows' middle)
        int[] half = {1, 2, 2, 1, 0};
        for (int r = 0; r < half.length; r++) {
            int y = f + r;
            fill(-half[r] - 1, y, -HALF_Z, half[r] + 1, y, -HALF_Z, rim);
            fill(-half[r], y, -HALF_Z, half[r], y, -HALF_Z, gem);
            if (r < 2) {
                fill(-1, y, -HALF_Z, 1, y, -HALF_Z, Blocks.AIR.defaultBlockState());
            }
        }
        set(0, f + half.length, -HALF_Z, rim);
        fill(-3, f - 1, -HALF_Z - 3, 3, f - 1, -HALF_Z - 1, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        fill(-3, f, -HALF_Z - 3, 3, f, -HALF_Z - 3, Blocks.DIORITE_WALL.defaultBlockState());
        fill(-3, f, -HALF_Z - 3, -3, f, -HALF_Z - 1, Blocks.DIORITE_WALL.defaultBlockState());
        fill(3, f, -HALF_Z - 3, 3, f, -HALF_Z - 1, Blocks.DIORITE_WALL.defaultBlockState());
        // the televisions: a wall of old screens glowing cyan, stacked on the west wall
        for (int z = -9; z <= 5; z++) {
            for (int y = f; y <= f + 3; y++) {
                boolean screen = (z & 1) == 0 && y <= f + 2;
                set(-14, y, z, screen ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState());
                set(-13, y, z, screen ? Blocks.CYAN_STAINED_GLASS.defaultBlockState()
                        : y == f + 3 ? Blocks.AIR.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState());
            }
        }
        set(-9, f, -2, stairs(Blocks.DARK_OAK_STAIRS.defaultBlockState(), Direction.EAST));
        set(-9, f, -1, stairs(Blocks.DARK_OAK_STAIRS.defaultBlockState(), Direction.EAST));
        // the hats: three stands to try them on, a mirror
        fill(-14, f + 1, 8, -14, f + 3, 10, Blocks.GLASS.defaultBlockState());
        int i = 0;
        int[] colors = {0x2B2B2B, 0x6B4A2B, 0x7A1E22};
        for (int x : new int[] {-12, -10, -8}) {
            ArmorStand stand = new ArmorStand(level, o.getX() + x + 0.5, o.getY() + f, o.getZ() + 9.5);
            ItemStack hat = new ItemStack(Items.LEATHER_HELMET);
            hat.set(DataComponents.DYED_COLOR, new DyedItemColor(colors[i++], false));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, hat);
            stand.setYRot(180.0F);
            level.addFreshEntity(stand);
        }
        // the gift: a little table, the hat on it, a note
        set(-3, f, 3, Blocks.DARK_OAK_FENCE.defaultBlockState());
        set(-3, f + 1, 3, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        ItemFrame frame = new ItemFrame(level, at(-3, f + 1, 3).above(), Direction.UP);
        frame.setItem(new ItemStack(ModItems.ULTRON_HAT.get()), false);
        frame.setInvisible(true);
        level.addFreshEntity(frame);
        set(-2, f, 3, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        sign(-2, f + 1, 3, Direction.NORTH, "sign.enhanced.mind.gift.1", "sign.enhanced.mind.gift.2",
                "sign.enhanced.mind.gift.3", "sign.enhanced.mind.gift.4");
    }

    private void top() {
        int f = TOP;
        BlockState plaster = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        fill(5, f, -HALF_Z + 1, 5, f + 4, HALF_Z - 1, plaster);
        opening(5, f, 5, 0, 0, 3);
        // the robots' hall (south) and, behind iron bars, the library (north)
        fill(-14, f, 1, 4, f, 10, Blocks.LIGHT_GRAY_CARPET.defaultBlockState());
        fill(-HALF_X + 1, f, 0, 4, f + 4, 0, Blocks.IRON_BARS.defaultBlockState());
        fill(-HALF_X + 1, f + 3, 0, 4, f + 4, 0, plaster);
        for (int x = -14; x <= 4; x += 3) {
            set(x, f, 10, Blocks.OBSERVER.defaultBlockState());
            set(x, f + 1, 10, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        }
        sign(-6, f + 2, 1, Direction.SOUTH, "sign.enhanced.mind.gate.1", "sign.enhanced.mind.gate.2");
        // the library: shelves and chests of books full of codes
        fill(-14, f, -10, -14, f + 2, -1, Blocks.BOOKSHELF.defaultBlockState());
        fill(-13, f, -10, 3, f + 2, -10, Blocks.BOOKSHELF.defaultBlockState());
        RandomSource random = RandomSource.create(o.asLong() * 31 + 7);
        int n = 0;
        for (int x = -12; x <= 2; x += 3) {
            set(x, f, -8, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
            if (level.getBlockEntity(at(x, f, -8)) instanceof ChestBlockEntity chest) {
                for (int slot = 0; slot < 6; slot++) {
                    chest.setItem(slot * 4 + random.nextInt(3), MindBooks.book(random, n++));
                }
            }
        }
        set(-6, f, -4, Blocks.LECTERN.defaultBlockState());
    }

    /** The open gate: the bars of the robots' doorway gone. */
    private void openGate() {
        fill(-6, TOP, 0, -5, TOP + 2, 0, Blocks.AIR.defaultBlockState());
    }

    private void below() {
        int f = BELOW;
        // the little television room (east), the elevator in its corner
        room(1, f - 1, 0, HALF_X, f + 4, HALF_Z, Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState());
        fill(2, f, 1, 10, f, 10, Blocks.GREEN_CARPET.defaultBlockState());
        fill(2, f + 1, 4, 2, f + 2, 6, Blocks.BLACK_CONCRETE.defaultBlockState());
        set(2, f, 5, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        for (int z = 4; z <= 6; z++) {
            set(6, f, z, stairs(Blocks.SPRUCE_STAIRS.defaultBlockState(), Direction.EAST));
        }
        set(6, f, 3, Blocks.SPRUCE_SLAB.defaultBlockState());
        set(6, f, 7, Blocks.SPRUCE_SLAB.defaultBlockState());
        lantern(5, f + 3, 5);
        liftNook(f);
    }

    private void bottom() {
        int f = BOTTOM;
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        // the elevator's room, then a long corridor west
        room(9, f - 1, 1, HALF_X, f + 4, 9, stone, Blocks.STONE_BRICKS.defaultBlockState(), stone);
        liftNook(f);
        room(-11, f - 1, 3, 10, f + 3, 7, stone, Blocks.POLISHED_ANDESITE.defaultBlockState(), stone);
        opening(9, f, 4, 2, 2, 3);
        for (int x = -10; x <= 8; x += 2) {
            candles(x, f, 4, 1 + Math.floorMod(x, 3), true);
            candles(x + 1, f, 6, 1 + Math.floorMod(x + 1, 3), false);
        }
        // the 1930s workshop: candles everywhere, wooden benches with glass and instruments, copper coils, wires
        room(-23, f - 1, -2, -11, f + 5, 13, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                Blocks.DARK_OAK_PLANKS.defaultBlockState());
        opening(-11, f, 4, 0, 2, 3);
        RandomSource random = RandomSource.create(o.asLong() + 1939);
        for (int x = -21; x <= -13; x += 4) {
            for (int z : new int[] {-1, 12}) {
                set(x, f, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                set(x + 1, f, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                set(x, f + 1, z, Blocks.BREWING_STAND.defaultBlockState());
                set(x + 1, f + 1, z, random.nextBoolean() ? Blocks.COMPARATOR.defaultBlockState()
                        : Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
            }
        }
        for (int z = 0; z <= 11; z += 3) {
            set(-22, f, z, Blocks.COPPER_BLOCK.defaultBlockState());
            set(-22, f + 1, z, Blocks.LIGHTNING_ROD.defaultBlockState());
            set(-12, f, z, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        }
        fill(-21, f, 0, -13, f, 0, Blocks.REDSTONE_WIRE.defaultBlockState());
        for (int i = 0; i < 40; i++) {
            int x = -22 + random.nextInt(11);
            int z = -1 + random.nextInt(14);
            if (level.getBlockState(at(x, f, z)).isAir() && level.getBlockState(at(x, f, z).below()).isSolid()
                    && Math.abs(x + 15) + Math.abs(z - 5) > 2) {
                candles(x, f, z, 1 + random.nextInt(4), random.nextBoolean());
            }
            int hx = -22 + random.nextInt(11);
            int hz = -1 + random.nextInt(14);
            if (level.getBlockState(at(hx, f + 1, hz)).isAir() && level.getBlockState(at(hx, f, hz)).is(Blocks.DARK_OAK_PLANKS)) {
                candles(hx, f + 1, hz, 1 + random.nextInt(3), true);
            }
        }
        // the armchair, and the easel by the painting (the painting goes on the north part of the west wall)
        set(-17, f, 10, stairs(Blocks.CRIMSON_STAIRS.defaultBlockState(), Direction.WEST));
        set(-18, f, 10, Blocks.CRIMSON_SLAB.defaultBlockState());
        set(-16, f, 10, Blocks.CRIMSON_SLAB.defaultBlockState());
        set(-20, f, 2, Blocks.LOOM.defaultBlockState());
        MindPaintings.place(level, at(-22, f + 2, 7), Direction.EAST, MindPaintings.HUMAN_TORCH);
        lantern(-17, f + 4, 5);
    }

    /** The floors under the ground, after the Source Code: filled with rock, as if they had never been. */
    private void removeBelow() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        fill(1, BELOW - 1, 0, HALF_X, BELOW + 4, HALF_Z, stone);
        fill(-23, BOTTOM - 1, -2, HALF_X, BOTTOM + 5, 13, stone);
        level.getEntitiesOfClass(net.minecraft.world.entity.decoration.Painting.class,
                net.minecraft.world.phys.AABB.encapsulatingFullBlocks(at(-24, BOTTOM - 1, -3), at(HALF_X + 1, BOTTOM + 6, 14)))
                .forEach(net.minecraft.world.entity.Entity::discard);
    }

    /** South-west of the house: a little mausoleum with two graves. */
    private void mausoleum() {
        int x0 = -32;
        int z0 = 22;
        BlockState wall = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        room(x0, -1, z0, x0 + 6, 4, z0 + 6, wall, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICK_SLAB.defaultBlockState());
        fill(x0 + 1, 5, z0 + 1, x0 + 5, 5, z0 + 5, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        opening(x0 + 3, 0, z0, 0, 0, 2);
        for (int x : new int[] {x0 + 2, x0 + 4}) {
            set(x, 0, z0 + 4, Blocks.STONE_BRICK_SLAB.defaultBlockState());
            set(x, 0, z0 + 5, Blocks.STONE_BRICKS.defaultBlockState());
            set(x, 1, z0 + 5, Blocks.STONE_BRICK_WALL.defaultBlockState());
            sign(x, 1, z0 + 4, Direction.NORTH, "sign.enhanced.mind.grave.1", "sign.enhanced.mind.grave.2");
            candles(x, 1, z0 + 3, 1, true);
        }
    }

}
