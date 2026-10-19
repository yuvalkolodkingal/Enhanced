package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.finders.CerebroRoom;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Cerebro console on the central platform of the lab's Cerebro (3.7): a curved desk with the helmet on its stand. A
 * telepath right-clicks it (or presses N there): the amplified Cerebro (lane L5's {@link CerebroRoom}). Registered by
 * the 3.7 skeleton with its block entity; lane L4 owns the block and its look, lane L5 what it does.
 */
public class CerebroConsoleBlock extends BaseEntityBlock {
    public static final MapCodec<CerebroConsoleBlock> CODEC = simpleCodec(CerebroConsoleBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public CerebroConsoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A right-click: the amplified Cerebro (lane L5's {@link CerebroRoom#useConsole}). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        return CerebroRoom.useConsole(server, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CerebroConsoleBlockEntity(pos, state);
    }
}
