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
 * The Horton manor's house (3.12), built by code around its middle at ground level: two floors of white stone and iron,
 * every room full of redstone and the machines of the Horton workshops, and on the ground floor's east side the
 * nanotech room with its storage ({@link NanoStorageBlock}).
 */
public class HortonManorPiece extends StructurePiece {
    static final int HALF_X = 12;
    static final int HALF_Z = 9;
    private final BlockPos o;

    public HortonManorPiece(BlockPos origin) {
        super(ModStructures.HORTON_MANOR_PIECE.get(), 0, new BoundingBox(origin.getX() - HALF_X - 2, origin.getY() - 6,
                origin.getZ() - HALF_Z - 4, origin.getX() + HALF_X + 2, origin.getY() + 16, origin.getZ() + HALF_Z + 2));
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
        BlockState wall = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState trim = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        // the ground under it, made level
        fill(-HALF_X - 2, -6, -HALF_Z - 4, HALF_X + 2, -1, HALF_Z + 2, Blocks.STONE_BRICKS.defaultBlockState());
        fill(-HALF_X - 2, 0, -HALF_Z - 4, HALF_X + 2, 15, HALF_Z + 2, air);
        // the shell: two floors
        fill(-HALF_X, 0, -HALF_Z, HALF_X, 11, HALF_Z, wall);
        for (int f : new int[] {0, 6}) {
            fill(-HALF_X + 1, f, -HALF_Z + 1, HALF_X - 1, f + 4, HALF_Z - 1, air);
            fill(-HALF_X + 1, f - 1, -HALF_Z + 1, HALF_X - 1, f - 1, HALF_Z - 1, floor);
            for (int x = -HALF_X + 2; x <= HALF_X - 2; x += 3) {
                fill(x, f + 1, -HALF_Z, x, f + 2, -HALF_Z, Blocks.GLASS_PANE.defaultBlockState());
                fill(x, f + 1, HALF_Z, x, f + 2, HALF_Z, Blocks.GLASS_PANE.defaultBlockState());
            }
            fill(-HALF_X, f - 1, -HALF_Z, HALF_X, f - 1, -HALF_Z, trim);
            fill(-HALF_X, f - 1, HALF_Z, HALF_X, f - 1, HALF_Z, trim);
            for (int x = -9; x <= 9; x += 6) {
                set(x, f + 4, 0, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
                set(x, f + 5, 0, Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
        }
        // the roof
        for (int i = 0; i <= 5; i++) {
            fill(-HALF_X - 1 + i, 12 + i, -HALF_Z - 1 + i, HALF_X + 1 - i, 12 + i, HALF_Z + 1 - i, Blocks.GRAY_CONCRETE.defaultBlockState());
        }
        // the door and a path
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        set(0, 0, -HALF_Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(0, 1, -HALF_Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(1, 0, -HALF_Z - 1, Blocks.STONE_BUTTON.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL));
        fill(-1, -1, -HALF_Z - 4, 1, -1, -HALF_Z - 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        sign(0, 3, -HALF_Z - 1, Direction.NORTH, "sign.enhanced.horton.1", "sign.enhanced.horton.2");
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
