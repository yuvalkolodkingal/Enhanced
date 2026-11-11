package com.kolodkingal.enhanced.nano;

import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The Horton manor (3.12; 3.12.1 after Eilon's design): a long red-brick Jacobean house round its middle at ground
 * level, of three floors. Pale stone quoins, bands and mullioned windows; two pavilions at the ends with stepped
 * Dutch gables, smaller gabled bays either side of a clock tower with an arched door and a green copper dome with a
 * weathervane; a dark roof with tall chimneys behind a stone balustrade. Before it, a formal garden: a cobbled walk
 * from the tower to the gate between four hedge knot gardens, stone lamp posts and benches, two fountains, cube-cut
 * trees and beds of flowers. Inside, every room full of redstone and the machines of the Horton workshops, and on the
 * ground floor east of the hall the nanotech room with its storage ({@link NanoStorageBlock}) at (8, 0, 0).
 */
public class HortonManorPiece extends StructurePiece {
    static final int HALF_X = 30;
    static final int HALF_Z = 9;
    /** How far the garden reaches north of the middle. */
    static final int FRONT = HALF_Z + 34;
    private static final int[] FLOORS = {0, 6, 12};
    private final BlockPos o;

    public HortonManorPiece(BlockPos origin) {
        super(ModStructures.HORTON_MANOR_PIECE.get(), 0, new BoundingBox(origin.getX() - HALF_X - 3, origin.getY() - 6,
                origin.getZ() - FRONT, origin.getX() + HALF_X + 3, origin.getY() + 36, origin.getZ() + HALF_Z + 2));
        this.o = origin;
    }

    public HortonManorPiece(CompoundTag tag) {
        this(BlockPos.of(tag.getLong("O")));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("O", o.asLong());
    }

    private WorldGenLevel level;
    private BoundingBox box;

    private void set(int x, int y, int z, BlockState state) {
        BlockPos pos = o.offset(x, y, z);
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
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

    /** Both halves of the house: {@code x1..x2} and its mirror {@code -x2..-x1}. */
    private void fillBoth(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        fill(x1, y1, z1, x2, y2, z2, state);
        fill(-x1, y1, z1, -x2, y2, z2, state);
    }

    /** Whether {@code (x, z)} is in the house's walls (its plan from above, the tower up to the eaves). */
    private static boolean solid(int x, int z) {
        int ax = Math.abs(x);
        return ax <= HALF_X && z >= -HALF_Z && z <= HALF_Z
                || ax >= 22 && ax <= HALF_X && z >= -HALF_Z - 4 && z <= HALF_Z
                || ax >= 7 && ax <= 11 && z == -HALF_Z - 1
                || ax <= 3 && z >= -HALF_Z - 4 && z <= -HALF_Z + 2;
    }

    private static boolean outside(int x, int z) {
        return solid(x, z) && (!solid(x + 1, z) || !solid(x - 1, z) || !solid(x, z + 1) || !solid(x, z - 1));
    }

    private static boolean corner(int x, int z) {
        boolean ex = !solid(x + 1, z) || !solid(x - 1, z);
        boolean ez = !solid(x, z + 1) || !solid(x, z - 1);
        return solid(x, z) && ex && ez;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        this.level = level;
        this.box = box;
        RandomSource rnd = RandomSource.create(o.asLong());
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState brick = Blocks.BRICKS.defaultBlockState();
        BlockState stone = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        BlockState cut = Blocks.CUT_SANDSTONE.defaultBlockState();
        BlockState trim = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();

        // ---- the ground: a lawn, the house's footing, the air above ----
        fill(-HALF_X - 3, -6, -FRONT, HALF_X + 3, -2, HALF_Z + 2, Blocks.DIRT.defaultBlockState());
        fill(-HALF_X - 3, -1, -FRONT, HALF_X + 3, -1, HALF_Z + 2, Blocks.GRASS_BLOCK.defaultBlockState());
        fill(-HALF_X, -6, -HALF_Z - 4, HALF_X, -1, HALF_Z, Blocks.STONE_BRICKS.defaultBlockState());
        fill(-HALF_X - 3, 0, -FRONT, HALF_X + 3, 36, HALF_Z + 2, air);

        // ---- the shell: every wall, then the stone at its corners and between its floors ----
        for (int x = -HALF_X; x <= HALF_X; x++) {
            for (int z = -HALF_Z - 4; z <= HALF_Z; z++) {
                if (!solid(x, z)) {
                    continue;
                }
                fill(x, 0, z, x, 17, z, brick);
                if (corner(x, z)) {
                    for (int y = 0; y <= 17; y++) {
                        set(x, y, z, (y & 1) == 0 ? cut : stone);
                    }
                } else if (outside(x, z)) {
                    set(x, 0, z, stone);
                    set(x, 5, z, stone);
                    set(x, 11, z, stone);
                    set(x, 17, z, stone);
                }
            }
        }
        // the rooms inside: the long range, the pavilions, the bays' window seats
        for (int f : FLOORS) {
            fill(-HALF_X + 1, f, -HALF_Z + 1, HALF_X - 1, f + 4, HALF_Z - 1, air);
            fillBoth(23, f, -HALF_Z - 3, 29, f + 4, -HALF_Z, air);
            fillBoth(8, f, -HALF_Z, 10, f + 4, -HALF_Z, air);
            fill(-HALF_X + 1, f - 1, -HALF_Z + 1, HALF_X - 1, f - 1, HALF_Z - 1, floor);
            fillBoth(23, f - 1, -HALF_Z - 3, 29, f - 1, -HALF_Z, floor);
            for (int x = -24; x <= 24; x += 8) {
                set(x, f + 4, 0, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
                set(x, f + 5, 0, Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
            // the windows: tall panes in pale stone frames, two by two
            for (int x : new int[] {5, 13, 16, 19}) {
                windowBoth(x, f, -HALF_Z);
            }
            windowBoth(8, f, -HALF_Z - 1);
            windowBoth(23, f, -HALF_Z - 4);
            windowBoth(27, f, -HALF_Z - 4);
            for (int x = 1; x <= 25; x += 4) {
                windowBoth(x, f, HALF_Z);
            }
            for (int z : new int[] {-HALF_Z - 2, -HALF_Z + 4, -1, 4}) {
                for (int sx : new int[] {-HALF_X, HALF_X}) {
                    fill(sx, f + 1, z, sx, f + 3, z + 1, Blocks.GLASS_PANE.defaultBlockState());
                    fill(sx, f + 4, z, sx, f + 4, z + 1, stone);
                }
            }
        }

        // ---- the roofs ----
        BlockState roof = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState roofStair = Blocks.DARK_OAK_STAIRS.defaultBlockState();
        // the long range: a ridge along it, behind a stone balustrade
        for (int i = 0; i <= HALF_Z; i++) {
            int y = 18 + i;
            fill(-21, y, -HALF_Z + i, 21, y, HALF_Z - i, roof);
            for (int x = -21; x <= 21; x++) {
                set(x, y, -HALF_Z + i, roofStair.setValue(StairBlock.FACING, Direction.SOUTH));
                set(x, y, HALF_Z - i, roofStair.setValue(StairBlock.FACING, Direction.NORTH));
            }
        }
        for (int x = -21; x <= 21; x++) {
            if (Math.abs(x) > 3) {
                set(x, 18, solid(x, -HALF_Z - 1) ? -HALF_Z - 1 : -HALF_Z, Blocks.SANDSTONE_WALL.defaultBlockState());
            }
        }
        // the pavilions: their own ridges, front to back, ending in stepped gables
        for (int s : new int[] {-1, 1}) {
            for (int i = 0; i <= 4; i++) {
                int y = 18 + i;
                int x1 = s > 0 ? 22 + i : -30 + i;
                int x2 = s > 0 ? 30 - i : -22 - i;
                fill(x1, y, -HALF_Z - 4, x2, y, HALF_Z, roof);
                for (int z = -HALF_Z - 4; z <= HALF_Z; z++) {
                    set(x1, y, z, roofStair.setValue(StairBlock.FACING, Direction.EAST));
                    set(x2, y, z, roofStair.setValue(StairBlock.FACING, Direction.WEST));
                }
            }
            for (int z : new int[] {-HALF_Z - 4, HALF_Z}) {
                dutchGable(s * 26, z, 4);
            }
        }
        // the bays' small gables
        for (int s : new int[] {-1, 1}) {
            dutchGable(s * 9, -HALF_Z - 1, 2);
        }
        // chimneys
        for (int[] c : new int[][] {{14, 3}, {-14, 3}, {19, -3}, {-19, -3}, {24, 6}, {-24, 6}, {28, 6}, {-28, 6}}) {
            fill(c[0], 18, c[1], c[0], 27, c[1], brick);
            set(c[0], 28, c[1], Blocks.BRICK_WALL.defaultBlockState());
        }

        // ---- the clock tower, its door, its dome ----
        int tz = -HALF_Z - 1;
        fill(-3, 18, -HALF_Z - 4, 3, 26, -HALF_Z + 2, brick);
        for (int x : new int[] {-3, 3}) {
            for (int z : new int[] {-HALF_Z - 4, -HALF_Z + 2}) {
                for (int y = 18; y <= 26; y++) {
                    set(x, y, z, (y & 1) == 0 ? cut : stone);
                }
            }
        }
        for (int y : new int[] {18, 26}) {
            fill(-3, y, -HALF_Z - 4, 3, y, -HALF_Z - 4, stone);
            fill(-3, y, -HALF_Z + 2, 3, y, -HALF_Z + 2, stone);
            fill(-3, y, -HALF_Z - 4, -3, y, -HALF_Z + 2, stone);
            fill(3, y, -HALF_Z - 4, 3, y, -HALF_Z + 2, stone);
        }
        for (int f : new int[] {6, 12}) {
            fill(-1, f + 1, -HALF_Z - 4, 1, f + 3, -HALF_Z - 4, Blocks.GLASS_PANE.defaultBlockState());
            fill(-1, f + 4, -HALF_Z - 4, 1, f + 4, -HALF_Z - 4, stone);
        }
        // the clock
        fill(-1, 21, -HALF_Z - 4, 1, 23, -HALF_Z - 4, Blocks.CALCITE.defaultBlockState());
        set(0, 22, -HALF_Z - 4, Blocks.BLACK_CONCRETE.defaultBlockState());
        fill(-3, 27, -HALF_Z - 4, 3, 27, -HALF_Z + 2, Blocks.SANDSTONE_WALL.defaultBlockState());
        fill(-2, 27, -HALF_Z - 3, 2, 27, -HALF_Z + 1, stone);
        // the belfry: eight-sided, open on four sides
        int cz = tz;
        fill(-2, 28, cz - 2, 2, 30, cz + 2, cut);
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                fill(dx, 28, cz + dz, dx, 30, cz + dz, air);
            }
        }
        fill(-1, 28, cz - 1, 1, 30, cz + 1, air);
        set(0, 29, cz - 2, air);
        set(0, 29, cz + 2, air);
        set(-2, 29, cz, air);
        set(2, 29, cz, air);
        set(0, 30, cz, Blocks.BELL.defaultBlockState());
        // the green copper dome and the weathervane
        BlockState copper = Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState();
        BlockState copperStair = Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS.defaultBlockState();
        fill(-2, 31, cz - 2, 2, 31, cz + 2, copper);
        for (int d = -1; d <= 1; d++) {
            set(d, 31, cz - 2, copperStair.setValue(StairBlock.FACING, Direction.SOUTH));
            set(d, 31, cz + 2, copperStair.setValue(StairBlock.FACING, Direction.NORTH));
            set(-2, 31, cz + d, copperStair.setValue(StairBlock.FACING, Direction.EAST));
            set(2, 31, cz + d, copperStair.setValue(StairBlock.FACING, Direction.WEST));
        }
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                set(dx, 31, cz + dz, air);
            }
        }
        fill(-1, 32, cz - 1, 1, 32, cz + 1, copper);
        set(0, 32, cz - 1, copperStair.setValue(StairBlock.FACING, Direction.SOUTH));
        set(0, 32, cz + 1, copperStair.setValue(StairBlock.FACING, Direction.NORTH));
        set(-1, 32, cz, copperStair.setValue(StairBlock.FACING, Direction.EAST));
        set(1, 32, cz, copperStair.setValue(StairBlock.FACING, Direction.WEST));
        set(0, 33, cz, copper);
        set(0, 34, cz, Blocks.IRON_BARS.defaultBlockState());
        set(0, 35, cz, Blocks.LIGHTNING_ROD.defaultBlockState());
        // the arched door through the tower's foot
        fill(-1, 0, -HALF_Z - 4, 1, 2, -HALF_Z - 1, air);
        set(0, 3, -HALF_Z - 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
        set(-1, 3, -HALF_Z - 4, cut);
        set(1, 3, -HALF_Z - 4, cut);
        fill(-1, -1, -HALF_Z - 4, 1, -1, -HALF_Z - 1, floor);
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        set(0, 0, -HALF_Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(0, 1, -HALF_Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(1, 0, -HALF_Z - 1, Blocks.STONE_BUTTON.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL));
        set(-1, 2, -HALF_Z - 1, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));
        sign(2, 2, -HALF_Z - 5, Direction.NORTH, "sign.enhanced.horton.1", "sign.enhanced.horton.2");

        // ---- inside: the stairs, the nanotech room, the workshops ----
        BlockState quartzStair = Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        for (int i = 0; i < 6; i++) {
            set(-HALF_X + 1 + i, i, HALF_Z - 2, quartzStair);
            set(-HALF_X + 1 + i, 5, HALF_Z - 2, air);
            set(-HALF_X + 1 + i, 5, HALF_Z - 1, air);
            set(-HALF_X + 7 + i, 6 + i, HALF_Z - 2, quartzStair);
            set(-HALF_X + 7 + i, 11, HALF_Z - 2, air);
            set(-HALF_X + 7 + i, 11, HALF_Z - 1, air);
        }
        // the nanotech room: east of the hall, behind iron walls, the storage in its middle
        fill(4, 0, -HALF_Z + 1, 4, 4, HALF_Z - 1, trim);
        fill(12, 0, -HALF_Z + 1, 12, 4, HALF_Z - 1, trim);
        set(4, 0, 0, air);
        set(4, 1, 0, air);
        fill(5, -1, -HALF_Z + 1, 11, -1, HALF_Z - 1, Blocks.BLACK_CONCRETE.defaultBlockState());
        set(8, 0, 0, ModBlocks.NANO_STORAGE.get().defaultBlockState());
        for (int[] c : new int[][] {{6, -2}, {10, -2}, {6, 2}, {10, 2}}) {
            set(c[0], 0, c[1], Blocks.LIGHTNING_ROD.defaultBlockState());
            set(c[0], 4, c[1], Blocks.CHAIN.defaultBlockState());
            set(c[0], 3, c[1], Blocks.SEA_LANTERN.defaultBlockState());
        }
        sign(5, 2, 0, Direction.EAST, "sign.enhanced.horton.nano.1", "sign.enhanced.horton.nano.2", "sign.enhanced.horton.nano.3");
        // the workshops: machines along the long walls of every floor
        BlockState[] machines = {Blocks.OBSERVER.defaultBlockState(), Blocks.PISTON.defaultBlockState(),
                Blocks.DISPENSER.defaultBlockState(), Blocks.NOTE_BLOCK.defaultBlockState(), Blocks.REDSTONE_LAMP.defaultBlockState(),
                Blocks.TARGET.defaultBlockState(), Blocks.CRAFTER.defaultBlockState(), Blocks.DROPPER.defaultBlockState(),
                Blocks.COPPER_BULB.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState()};
        for (int f : FLOORS) {
            for (int x = -HALF_X + 2; x <= HALF_X - 2; x += 2) {
                if (f == 0 && x >= 3 && x <= 13 || Math.abs(x) <= 2) {
                    continue; // the nanotech room, the hall
                }
                for (int z : new int[] {-HALF_Z + 1, HALF_Z - 1}) {
                    if (z == HALF_Z - 1 && x <= -HALF_X + 13 && f < 12) {
                        continue; // the stairs
                    }
                    set(x, f, z, machines[rnd.nextInt(machines.length)]);
                    set(x, f + 1, z, rnd.nextBoolean() ? Blocks.REPEATER.defaultBlockState() : Blocks.COMPARATOR.defaultBlockState());
                }
            }
            for (int x = -HALF_X + 3; x <= HALF_X - 4; x += 4) {
                if (f == 0 && x >= 2 && x <= 13 || Math.abs(x) <= 2) {
                    continue;
                }
                set(x, f, -2, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                set(x, f, 2, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                set(x + 1, f, -2, Blocks.REDSTONE_WIRE.defaultBlockState());
                set(x + 1, f, 2, Blocks.REDSTONE_TORCH.defaultBlockState());
            }
        }

        garden(rnd);

        // the clock (an item frame on the tower's face)
        BlockPos clockAt = o.offset(0, 22, -HALF_Z - 5);
        if (box.isInside(clockAt)) {
            net.minecraft.world.entity.decoration.ItemFrame clock = new net.minecraft.world.entity.decoration.ItemFrame(
                    level.getLevel(), clockAt, Direction.NORTH);
            clock.setItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLOCK), false);
            clock.setInvisible(true);
            level.addFreshEntity(clock);
        }
    }

    /** A pair of windows (two panes wide, three high, pale stone above) at {@code x, x + 1} and its mirror. */
    private void windowBoth(int x, int f, int z) {
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        BlockState stone = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        for (int s : new int[] {1, -1}) {
            int a = s > 0 ? x : -x - 1;
            fill(a, f + 1, z, a + 1, f + 3, z, pane);
            fill(a, f + 4, z, a + 1, f + 4, z, stone);
        }
    }

    /**
     * A stepped (Dutch) gable on the wall at {@code z}, centred on {@code cx}, {@code half} blocks to each side at its
     * foot: brick with pale stone copings and finials, over the roof behind it.
     */
    private void dutchGable(int cx, int z, int half) {
        BlockState brick = Blocks.BRICKS.defaultBlockState();
        BlockState stone = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        int y = 18;
        int w = half;
        while (w >= 0) {
            for (int step = 0; step < 2 && w >= 0; step++) {
                fill(cx - w, y, z, cx + w, y, z, brick);
                set(cx - w, y, z, stone);
                set(cx + w, y, z, stone);
                y++;
            }
            if (w > 0) {
                set(cx - w, y, z, Blocks.SANDSTONE_WALL.defaultBlockState());
                set(cx + w, y, z, Blocks.SANDSTONE_WALL.defaultBlockState());
            }
            w--;
        }
        set(cx, y, z, Blocks.SANDSTONE_WALL.defaultBlockState());
        set(cx, y + 1, z, Blocks.LIGHTNING_ROD.defaultBlockState());
        // a window high in the gable
        if (half >= 3) {
            set(cx, 20, z, Blocks.GLASS_PANE.defaultBlockState());
            set(cx, 21, z, Blocks.GLASS_PANE.defaultBlockState());
        }
    }

    /** The formal garden before the house. */
    private void garden(RandomSource rnd) {
        BlockState hedge = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        BlockState[] cobble = {Blocks.COBBLESTONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(),
                Blocks.STONE.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState()};
        // the walk before the house, the walk to the gate, the walk across the garden
        for (int x = -HALF_X - 1; x <= HALF_X + 1; x++) {
            for (int z = -HALF_Z - 7; z <= -HALF_Z - 5; z++) {
                set(x, -1, z, cobble[rnd.nextInt(cobble.length)]);
            }
            for (int z = -29; z <= -28; z++) {
                set(x, -1, z, cobble[rnd.nextInt(cobble.length)]);
            }
        }
        for (int z = -FRONT; z <= -HALF_Z - 5; z++) {
            for (int x = -2; x <= 2; x++) {
                set(x, -1, z, cobble[rnd.nextInt(cobble.length)]);
            }
        }
        // four knot gardens of clipped hedge
        for (int s : new int[] {-1, 1}) {
            knot(s, 5, 20, -26, -18, hedge, rnd);
            knot(s, 5, 20, -40, -31, hedge, rnd);
        }
        // the lamp posts along the walk, and benches facing it
        for (int z : new int[] {-17, -25, -32, -40}) {
            for (int x : new int[] {-3, 3}) {
                set(x, -1, z, Blocks.STONE_BRICKS.defaultBlockState());
                set(x, 0, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                set(x, 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                set(x, 2, z, Blocks.LANTERN.defaultBlockState());
            }
        }
        for (int z : new int[] {-22, -36}) {
            for (int dz = 0; dz < 2; dz++) {
                set(4, 0, z + dz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
                set(-4, 0, z + dz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            }
        }
        // a fountain on each side
        for (int s : new int[] {-1, 1}) {
            int fx = s * 25;
            int fz = -28;
            fill(fx - 2, -1, fz - 2, fx + 2, -1, fz + 2, Blocks.STONE_BRICKS.defaultBlockState());
            fill(fx - 2, 0, fz - 2, fx + 2, 0, fz + 2, Blocks.STONE_BRICK_SLAB.defaultBlockState());
            fill(fx - 1, 0, fz - 1, fx + 1, 0, fz + 1, Blocks.WATER.defaultBlockState());
            fill(fx, 0, fz, fx, 1, fz, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            set(fx, 2, fz, Blocks.SEA_LANTERN.defaultBlockState());
        }
        // cube-cut trees, beds of flowers, a hedge round it all
        for (int[] t : new int[][] {{16, -12}, {-16, -12}, {29, -22}, {-29, -22}, {29, -37}, {-29, -37}}) {
            topiary(t[0], t[1]);
        }
        BlockState[] flowers = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
                Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.AZURE_BLUET.defaultBlockState(), Blocks.RED_TULIP.defaultBlockState(), Blocks.WHITE_TULIP.defaultBlockState()};
        for (int s : new int[] {-1, 1}) {
            for (int x = 12; x <= 21; x++) {
                if (rnd.nextInt(3) > 0) {
                    set(s * x, 0, -HALF_Z - 2, flowers[rnd.nextInt(flowers.length)]);
                }
            }
            for (int x = 23; x <= 31; x++) {
                for (int z : new int[] {-26, -25, -33, -32}) {
                    if (rnd.nextInt(3) > 0) {
                        set(s * x, 0, z, flowers[rnd.nextInt(flowers.length)]);
                    }
                }
            }
            for (int z = -FRONT; z <= -HALF_Z - 8; z++) {
                set(s * (HALF_X + 2), 0, z, hedge);
            }
        }
        for (int x = -HALF_X - 2; x <= HALF_X + 2; x++) {
            if (Math.abs(x) > 2) {
                set(x, 0, -FRONT, hedge);
            }
        }
    }

    /** A knot garden of hedges in {@code x1..x2} (mirrored by {@code s}) and {@code z1..z2}, flowers in its beds. */
    private void knot(int s, int x1, int x2, int z1, int z2, BlockState hedge, RandomSource rnd) {
        BlockState[] flowers = {Blocks.POPPY.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
                Blocks.CORNFLOWER.defaultBlockState(), Blocks.WHITE_TULIP.defaultBlockState()};
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean ring = x == x1 || x == x2 || z == z1 || z == z2;
                boolean inner = (x == x1 + 2 || x == x2 - 2) && z >= z1 + 2 && z <= z2 - 2
                        || (z == z1 + 2 || z == z2 - 2) && x >= x1 + 2 && x <= x2 - 2;
                boolean cross = (x == cx || z == cz) && x > x1 + 2 && x < x2 - 2 && z > z1 + 2 && z < z2 - 2;
                boolean gate = (z == cz && (x == x1 || x == x1 + 2)) || (x == cx && (z == z1 || z == z2));
                if ((ring || inner || cross) && !gate) {
                    set(s * x, 0, z, hedge);
                } else if (!ring && rnd.nextInt(7) == 0) {
                    set(s * x, 0, z, flowers[rnd.nextInt(flowers.length)]);
                }
            }
        }
    }

    /** A tree clipped to a cube, with a smaller cube on top. */
    private void topiary(int x, int z) {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        fill(x - 2, 2, z - 2, x + 2, 4, z + 2, leaves);
        fill(x - 1, 5, z - 1, x + 1, 6, z + 1, leaves);
        fill(x, 0, z, x, 3, z, Blocks.OAK_LOG.defaultBlockState());
    }

    private void sign(int x, int y, int z, Direction facing, String... keys) {
        set(x, y, z, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        BlockPos pos = o.offset(x, y, z);
        if (box.isInside(pos) && level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            Component[] lines = new Component[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = i < keys.length ? Component.translatable(keys[i]) : Component.empty();
            }
            sign.setText(new SignText(lines, lines, DyeColor.BLACK, false), true);
            sign.setWaxed(true);
        }
    }
}
