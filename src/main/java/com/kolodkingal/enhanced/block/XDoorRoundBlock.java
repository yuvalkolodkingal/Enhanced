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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The round X-door of Cerebro (3.7, lane L4): a 3 x 3 steel door of nine parts ({@link #PART} 0 to 8: column {@code
 * part % 3}, row {@code part / 3}, left to right as seen from the side opposite to {@link #FACING}, bottom to top),
 * each a ninth of a round iris with a big X across it. When a player comes near ({@link LabDoors}) the iris opens: the
 * square steel frame round it stays and the round middle is gone (the open parts let everyone through), and it shuts
 * behind him. Placed in Creative, one item puts the whole door with the clicked block as its bottom middle; broken, the
 * whole door goes.
 */
public class XDoorRoundBlock extends Block {
    public static final MapCodec<XDoorRoundBlock> CODEC = simpleCodec(XDoorRoundBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public XDoorRoundBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 4).setValue(OPEN,
                false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos().relative(facing.getClockWise(), -1);
        for (int part = 0; part < 9; part++) {
            BlockPos p = origin.relative(facing.getClockWise(), part % 3).above(part / 3);
            if (part != 1 && !level.getBlockState(p).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, 1);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        Direction facing = state.getValue(FACING);
        BlockPos origin = pos.relative(facing.getClockWise(), -1);
        for (int part = 0; part < 9; part++) {
            if (part != 1) {
                level.setBlock(origin.relative(facing.getClockWise(), part % 3).above(part / 3),
                        state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
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
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            LabDoors.recheck(level, pos);
        }
    }

    /** The nine blocks of the door a part at {@code pos} belongs to, part 0 first (the ones still there). */
    public static List<BlockPos> cells(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<BlockPos> out = new ArrayList<>();
        if (!(state.getBlock() instanceof XDoorRoundBlock)) {
            return out;
        }
        int part = state.getValue(PART);
        Direction right = state.getValue(FACING).getClockWise();
        BlockPos origin = pos.relative(right, -(part % 3)).below(part / 3);
        for (int p = 0; p < 9; p++) {
            BlockPos at = origin.relative(right, p % 3).above(p / 3);
            BlockState there = level.getBlockState(at);
            if (there.getBlock() instanceof XDoorRoundBlock && there.getValue(PART) == p) {
                out.add(at);
            }
        }
        return out;
    }
}
