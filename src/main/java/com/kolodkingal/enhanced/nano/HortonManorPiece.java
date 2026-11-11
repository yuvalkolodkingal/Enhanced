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
 * The Horton manor's house (3.12; its look from the series in 3.12.1), built by code around its middle at ground level: a
 * red-brick house of two floors with stone quoins and bands, wings, a clock tower with a white cupola and a forecourt
 * with a fountain,
 * every room full of redstone and the machines of the Horton workshops, and on the ground floor's east side the
 * nanotech room with its storage ({@link NanoStorageBlock}).
 */
public class HortonManorPiece extends StructurePiece {
    static final int HALF_X = 12;
    static final int HALF_Z = 9;
    /** How far the forecourt and its fountain reach north of the middle. */
    static final int FRONT = HALF_Z + 16;
    private final BlockPos o;

    public HortonManorPiece(BlockPos origin) {
        super(ModStructures.HORTON_MANOR_PIECE.get(), 0, new BoundingBox(origin.getX() - HALF_X - 3, origin.getY() - 6,
                origin.getZ() - FRONT, origin.getX() + HALF_X + 3, origin.getY() + 30, origin.getZ() + HALF_Z + 2));
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

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        this.level = level;
        this.box = box;
        RandomSource rnd = RandomSource.create(o.asLong());
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState brick = Blocks.BRICKS.defaultBlockState();
        BlockState stone = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState quoin = Blocks.CALCITE.defaultBlockState();
        BlockState trim = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        // the ground under it made level: a lawn before it, and the house's own footing
        fill(-HALF_X - 3, -6, -FRONT, HALF_X + 3, -2, HALF_Z + 2, Blocks.DIRT.defaultBlockState());
        fill(-HALF_X - 3, -1, -FRONT, HALF_X + 3, -1, HALF_Z + 2, Blocks.GRASS_BLOCK.defaultBlockState());
        fill(-HALF_X, -6, -HALF_Z - 3, HALF_X, -1, HALF_Z, Blocks.STONE_BRICKS.defaultBlockState());
        fill(-HALF_X - 3, 0, -FRONT, HALF_X + 3, 30, HALF_Z + 2, air);
        // the shell, as the Horton estate in the series: red brick, stone quoins and bands, two floors
        fill(-HALF_X, 0, -HALF_Z, HALF_X, 11, HALF_Z, brick);
        // the wings at both ends, a little forward, and the entrance bay in the middle
        // (the wings stand a floor higher than the middle, as at the estate in the series)
        for (int sx : new int[] {-1, 1}) {
            fill(sx * 9, 0, -HALF_Z - 3, sx * HALF_X, 17, HALF_Z, brick);
        }
        fill(-3, 0, -HALF_Z - 2, 3, 11, -HALF_Z - 1, brick);
        for (int f : new int[] {0, 6}) {
            fill(-HALF_X + 1, f, -HALF_Z + 1, HALF_X - 1, f + 4, HALF_Z - 1, air);
            fill(-HALF_X + 1, f - 1, -HALF_Z + 1, HALF_X - 1, f - 1, HALF_Z - 1, floor);
            for (int x = -9; x <= 9; x += 6) {
                set(x, f + 4, 0, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
                set(x, f + 5, 0, Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
            // mullioned windows: pairs of tall panes in stone frames
            for (int x : new int[] {-7, 5}) {
                mullion(x, f, -HALF_Z, true);
            }
            for (int x = -11; x <= 9; x += 4) {
                mullion(x, f, HALF_Z, true);
            }
            for (int z = -6; z <= 6; z += 4) {
                mullion(-HALF_X, f, z, false);
                mullion(HALF_X, f, z, false);
            }
            for (int sx : new int[] {-1, 1}) {
                mullion(sx > 0 ? 10 : -11, f, -HALF_Z - 3, true);
            }
            if (f == 6) {
                mullion(-1, f, -HALF_Z - 2, true);
            }
        }
        // bands of stone between the floors and under the parapet, quoins at the corners
        for (int y : new int[] {0, 5, 11}) {
            band(y, stone);
        }
        for (int[] c : new int[][] {{-HALF_X, HALF_Z}, {HALF_X, HALF_Z}, {-HALF_X, -HALF_Z - 3}, {HALF_X, -HALF_Z - 3},
                {-9, -HALF_Z - 3}, {9, -HALF_Z - 3}, {-9, HALF_Z}, {9, HALF_Z}, {-3, -HALF_Z - 2}, {3, -HALF_Z - 2}}) {
            int top = Math.abs(c[0]) >= 9 ? 17 : 11;
            for (int y = 0; y <= top; y++) {
                set(c[0], y, c[1], (y & 1) == 0 ? quoin : Blocks.SMOOTH_QUARTZ.defaultBlockState());
            }
        }
        // the roof: flat behind a stone parapet, chimneys, little turrets on the corners of the wings
        fill(-HALF_X + 1, 12, -HALF_Z + 1, HALF_X - 1, 12, HALF_Z - 1, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        // (the middle's balustrade, front and back, between the wings)
        fill(-8, 12, -HALF_Z, 8, 12, -HALF_Z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        fill(-8, 12, HALF_Z, 8, 12, HALF_Z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        for (int sx : new int[] {-1, 1}) {
            int x1 = Math.min(sx * 9, sx * HALF_X);
            int x2 = Math.max(sx * 9, sx * HALF_X);
            // the wing's third row of windows, its roof and balustrade
            for (int z = -HALF_Z + 1; z <= HALF_Z - 2; z += 4) {
                mullion(sx * HALF_X, 12, z, false);
            }
            mullion(sx > 0 ? 10 : -11, 12, -HALF_Z - 3, true);
            mullion(sx > 0 ? 10 : -11, 12, HALF_Z, true);
            band(x1, -HALF_Z - 3, x2, HALF_Z, 11, Blocks.SMOOTH_STONE.defaultBlockState());
            fill(x1 + 1, 18, -HALF_Z - 2, x2 - 1, 18, HALF_Z - 1, Blocks.STONE_BRICK_SLAB.defaultBlockState());
            parapet(x1, -HALF_Z - 3, x2, HALF_Z, 18);
            // turrets on its four corners: white and brick, a grey lead dome, a gilt finial
            for (int x : new int[] {x1, x2}) {
                for (int z : new int[] {-HALF_Z - 3, HALF_Z}) {
                    fill(x, 18, z, x, 20, z, quoin);
                    set(x, 21, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
                    set(x, 22, z, Blocks.ANDESITE_WALL.defaultBlockState());
                    set(x, 23, z, Blocks.LIGHTNING_ROD.defaultBlockState());
                }
            }
            fill(sx * 6, 12, 5, sx * 6, 17, 5, brick);
            fill(sx * 6, 12, -5, sx * 6, 17, -5, brick);
            // ivy on the front, between the windows
            RandomSource ivy = RandomSource.create(o.asLong() + sx);
            for (int x : sx > 0 ? new int[] {4, 7, 8} : new int[] {-4, -5, -8}) {
                int top = 2 + ivy.nextInt(8);
                for (int y = 0; y <= top; y++) {
                    set(x, y, -HALF_Z - 1, Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.SOUTH, true));
                }
            }
        }
        // the clock tower over the entrance, and its white cupola
        fill(-2, 12, -HALF_Z - 2, 2, 21, -HALF_Z + 2, Blocks.CALCITE.defaultBlockState());
        for (int[] c : new int[][] {{-2, -HALF_Z - 2}, {2, -HALF_Z - 2}, {-2, -HALF_Z + 2}, {2, -HALF_Z + 2}}) {
            fill(c[0], 12, c[1], c[0], 21, c[1], Blocks.SMOOTH_QUARTZ.defaultBlockState());
        }
        fill(-1, 15, -HALF_Z - 2, 1, 17, -HALF_Z - 2, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        set(0, 16, -HALF_Z - 2, Blocks.BLACK_CONCRETE.defaultBlockState());
        band(-2, -HALF_Z - 2, 2, -HALF_Z + 2, 18, stone);
        for (int y = 19; y <= 20; y++) {
            set(0, y, -HALF_Z - 2, air);
            set(0, y, -HALF_Z + 2, air);
            set(-2, y, -HALF_Z, air);
            set(2, y, -HALF_Z, air);
        }
        fill(-1, 19, -HALF_Z - 1, 1, 20, -HALF_Z + 1, air);
        set(0, 20, -HALF_Z, Blocks.BELL.defaultBlockState());
        fill(-2, 22, -HALF_Z - 2, 2, 22, -HALF_Z + 2, stone);
        for (int[] c : new int[][] {{-1, -HALF_Z - 1}, {1, -HALF_Z - 1}, {-1, -HALF_Z + 1}, {1, -HALF_Z + 1}}) {
            fill(c[0], 23, c[1], c[0], 25, c[1], Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        fill(-1, 26, -HALF_Z - 1, 1, 26, -HALF_Z + 1, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        set(0, 27, -HALF_Z, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        set(0, 28, -HALF_Z, Blocks.QUARTZ_SLAB.defaultBlockState());
        set(0, 29, -HALF_Z, Blocks.LIGHTNING_ROD.defaultBlockState());
        // the door, in an arched porch in the entrance bay
        fill(-1, 0, -HALF_Z - 2, 1, 2, -HALF_Z - 1, air);
        fill(-1, 3, -HALF_Z - 2, 1, 3, -HALF_Z - 2, stone);
        fill(-1, -1, -HALF_Z - 2, 1, -1, -HALF_Z - 1, Blocks.POLISHED_ANDESITE.defaultBlockState());
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        set(0, 0, -HALF_Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(0, 1, -HALF_Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(1, 0, -HALF_Z - 1, Blocks.STONE_BUTTON.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL));
        set(-2, 1, -HALF_Z - 3, Blocks.LANTERN.defaultBlockState());
        set(2, 1, -HALF_Z - 3, Blocks.LANTERN.defaultBlockState());
        sign(2, 2, -HALF_Z - 3, Direction.NORTH, "sign.enhanced.horton.1", "sign.enhanced.horton.2");
        // the forecourt: a gravel round with a fountain, a stone path to the porch, hedges at its sides
        int fz = -HALF_Z - 10;
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= 6.5) {
                    set(x, -1, fz + z, d <= 4.5 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
                }
                if (d <= 3.5 && d > 2.5) {
                    set(x, 0, fz + z, quoin);
                    set(x, -1, fz + z, quoin);
                } else if (d <= 2.5) {
                    set(x, -1, fz + z, quoin);
                    set(x, 0, fz + z, Blocks.WATER.defaultBlockState());
                }
            }
        }
        // (in its middle a metal sculpture on a stone plinth, as at the estate; still water round it)
        set(0, 0, fz, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        set(0, 1, fz, Blocks.STONE_BRICK_WALL.defaultBlockState());
        set(0, 2, fz, Blocks.IRON_BARS.defaultBlockState());
        set(0, 3, fz, Blocks.LIGHTNING_ROD.defaultBlockState());
        fill(-1, -1, fz + 7, 1, -1, -HALF_Z - 3, stone);
        fill(-1, -1, -FRONT, 1, -1, fz - 7, stone);
        for (int z = -FRONT + 1; z <= -HALF_Z - 4; z += 2) {
            set(-HALF_X - 2, 0, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
            set(HALF_X + 2, 0, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
        }
        // the clock (an item frame on the tower's face)
        BlockPos clockAt = o.offset(0, 16, -HALF_Z - 3);
        if (box.isInside(clockAt)) {
            net.minecraft.world.entity.decoration.ItemFrame clock = new net.minecraft.world.entity.decoration.ItemFrame(
                    level.getLevel(), clockAt, Direction.NORTH);
            clock.setItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLOCK), false);
            clock.setInvisible(true);
            level.addFreshEntity(clock);
        }
        // the stairs up, in the west
        for (int i = 0; i < 6; i++) {
            set(-HALF_X + 1 + i, i, HALF_Z - 2, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            set(-HALF_X + 1 + i, 5, HALF_Z - 2, air);
            set(-HALF_X + 1 + i, 5, HALF_Z - 1, air);
        }
        // the nanotech room: the ground floor's east end, behind an iron wall, the storage in its middle
        fill(4, 0, -HALF_Z + 1, 4, 4, HALF_Z - 1, trim);
        set(4, 0, 0, air);
        set(4, 1, 0, air);
        fill(5, -1, -HALF_Z + 1, HALF_X - 1, -1, HALF_Z - 1, Blocks.BLACK_CONCRETE.defaultBlockState());
        set(8, 0, 0, ModBlocks.NANO_STORAGE.get().defaultBlockState());
        for (int[] c : new int[][] {{6, -2}, {10, -2}, {6, 2}, {10, 2}}) {
            set(c[0], 0, c[1], Blocks.LIGHTNING_ROD.defaultBlockState());
            set(c[0], 4, c[1], Blocks.CHAIN.defaultBlockState());
            set(c[0], 3, c[1], Blocks.SEA_LANTERN.defaultBlockState());
        }
        sign(5, 2, 0, Direction.EAST, "sign.enhanced.horton.nano.1", "sign.enhanced.horton.nano.2", "sign.enhanced.horton.nano.3");
        // the rest: workshops full of redstone and machines, on both floors
        BlockState[] machines = {Blocks.OBSERVER.defaultBlockState(), Blocks.PISTON.defaultBlockState(),
                Blocks.DISPENSER.defaultBlockState(), Blocks.NOTE_BLOCK.defaultBlockState(), Blocks.REDSTONE_LAMP.defaultBlockState(),
                Blocks.TARGET.defaultBlockState(), Blocks.CRAFTER.defaultBlockState(), Blocks.DROPPER.defaultBlockState(),
                Blocks.COPPER_BULB.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState()};
        for (int f : new int[] {0, 6}) {
            int east = f == 0 ? 3 : HALF_X - 1;
            for (int x = -HALF_X + 2; x <= east - 1; x += 2) {
                for (int z : new int[] {-HALF_Z + 1, HALF_Z - 1}) {
                    if (f == 0 && z == HALF_Z - 1 && x <= -HALF_X + 7) {
                        continue; // the stairs
                    }
                    set(x, f, z, machines[rnd.nextInt(machines.length)]);
                    set(x, f + 1, z, rnd.nextBoolean() ? Blocks.REPEATER.defaultBlockState() : Blocks.COMPARATOR.defaultBlockState());
                }
            }
            for (int x = -HALF_X + 3; x <= east - 2; x += 4) {
                set(x, f, -2, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                set(x, f, 2, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                set(x + 1, f, -2, Blocks.REDSTONE_WIRE.defaultBlockState());
                set(x + 1, f, 2, Blocks.REDSTONE_TORCH.defaultBlockState());
            }
        }
    }

    /** A pair of tall panes (three high) in a stone frame in the wall at {@code (x, z)} and its neighbour. */
    private void mullion(int x, int f, int z, boolean alongX) {
        BlockState stone = Blocks.SMOOTH_STONE.defaultBlockState();
        for (int i = 0; i < 2; i++) {
            int wx = alongX ? x + i : x;
            int wz = alongX ? z : z + i;
            fill(wx, f + 1, wz, wx, f + 3, wz, Blocks.GLASS_PANE.defaultBlockState());
            set(wx, f + 4, wz, stone);
        }
    }

    /** A band of {@code state} round the main block's outside at height {@code y}. */
    private void band(int y, BlockState state) {
        band(-HALF_X, -HALF_Z, HALF_X, HALF_Z, y, state);
    }

    private void band(int x1, int z1, int x2, int z2, int y, BlockState state) {
        fill(x1, y, z1, x2, y, z1, state);
        fill(x1, y, z2, x2, y, z2, state);
        fill(x1, y, z1, x1, y, z2, state);
        fill(x2, y, z1, x2, y, z2, state);
    }

    /** A stone balustrade round a rectangle's top at height {@code y}. */
    private void parapet(int x1, int z1, int x2, int z2, int y) {
        band(x1, z1, x2, z2, y, Blocks.STONE_BRICK_WALL.defaultBlockState());
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
