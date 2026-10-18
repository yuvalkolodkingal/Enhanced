package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.odin.Coronation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;

/**
 * The Throne of Asgard (3.5), on the dais of the throne room ({@code CityPlan.THRONE}). Sitting on it is lane L1's (the seat
 * entity); a player who sits down is told to {@link Coronation#onSit} (lane L3: the coronation of Odin). The 3.5 skeleton
 * registers it so that the hook has its caller: here the click itself stands for sitting down until lane L1 seats him.
 */
public class OdinThroneBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<OdinThroneBlock> CODEC = simpleCodec(OdinThroneBlock::new);

    public OdinThroneBlock(Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            Coronation.onSit(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
