package com.kolodkingal.enhanced.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An X-door of the lower lab (3.7, lane L4): a double automatic door of steel, each leaf (this block, {@link #HINGE}
 * left or right) a 1 x 2 door showing half of a big X; the two leaves slide apart into the frame when a player comes
 * near and close behind him ({@link LabDoors}). {@link #FACING} is the axis of the wall (the door lies across it), and
 * the left leaf is the one on the left of one who stands on the side opposite to the facing and looks towards it (so
 * its partner is to the facing's clockwise side). Open, a leaf is a thin sliver at its outer edge and lets everyone
 * through. Placed in Creative, a leaf adds its upper half; broken, the whole door goes.
 */
public class XDoorBlock extends Block {
    public static final MapCodec<XDoorBlock> CODEC = simpleCodec(XDoorBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;

    private static final Map<Direction, VoxelShape> CLOSED = LabShapes.turned(new double[]{0, 0, 6, 16, 16, 10});
    private static final Map<Direction, VoxelShape> OPEN_LEFT = LabShapes.turned(new double[]{0, 0, 6, 2, 16, 10});
    private static final Map<Direction, VoxelShape> OPEN_RIGHT = LabShapes.turned(new double[]{14, 0, 6, 16, 16,
            10});

    public XDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF,
                DoubleBlockHalf.LOWER).setValue(OPEN, false).setValue(HINGE, DoorHingeSide.LEFT));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN, HINGE);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING))).cycle(HINGE);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState left = level.getBlockState(pos.relative(facing.getCounterClockWise()));
        DoorHingeSide hinge = left.getBlock() instanceof XDoorBlock && left.getValue(FACING) == facing
                && left.getValue(HINGE) == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        return defaultBlockState().setValue(FACING, facing).setValue(HINGE, hinge);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            for (BlockPos p : cells(level, pos)) {
                if (!p.equals(pos)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return CLOSED.get(state.getValue(FACING));
        }
        return (state.getValue(HINGE) == DoorHingeSide.LEFT ? OPEN_LEFT : OPEN_RIGHT).get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : CLOSED.get(state.getValue(FACING));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            LabDoors.recheck(level, pos);
        }
    }

    /**
     * The blocks of the door a leaf at {@code pos} belongs to: the left leaf's lower and upper half, then the right
     * leaf's (a leaf without its partner is a door of its own).
     */
    public static List<BlockPos> cells(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<BlockPos> out = new ArrayList<>();
        if (!(state.getBlock() instanceof XDoorBlock)) {
            return out;
        }
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        Direction facing = state.getValue(FACING);
        boolean left = state.getValue(HINGE) == DoorHingeSide.LEFT;
        BlockPos other = lower.relative(left ? facing.getClockWise() : facing.getCounterClockWise());
        BlockState partner = level.getBlockState(other);
        boolean paired = partner.getBlock() instanceof XDoorBlock && partner.getValue(FACING) == facing
                && partner.getValue(HINGE) != state.getValue(HINGE);
        BlockPos leftLower = left || !paired ? lower : other;
        out.add(leftLower);
        out.add(leftLower.above());
        if (paired) {
            BlockPos rightLower = left ? other : lower;
            out.add(rightLower);
            out.add(rightLower.above());
        }
        out.removeIf(p -> !(level.getBlockState(p).getBlock() instanceof XDoorBlock));
        return out;
    }
}
