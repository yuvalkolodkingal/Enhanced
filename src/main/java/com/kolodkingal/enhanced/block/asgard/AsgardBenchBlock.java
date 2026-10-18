package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.asgard.SeatEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A stone bench of Asgard (3.5): right-click to sit on it ({@link SeatEntity}); Asgardians sit on them too. */
public class AsgardBenchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AsgardBenchBlock> CODEC = simpleCodec(AsgardBenchBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 2, 16, 8, 14);
    private static final VoxelShape SHAPE_X = Block.box(2, 0, 0, 14, 8, 16);
    /** Where a sitter's seat is, above the block's bottom. */
    public static final double SEAT_HEIGHT = 0.15;

    public AsgardBenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE : SHAPE_X;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && !player.isShiftKeyDown()) {
            SeatEntity.sit(player, Vec3.atBottomCenterOf(pos).add(0, SEAT_HEIGHT, 0), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
