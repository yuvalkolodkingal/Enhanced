package com.kolodkingal.enhanced.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Eternity's altar (3.8, PLAN 10.3): at the centre of the Shrine of the Cosmic Entities (and, as the wishing stone, in
 * Eternity's realm). Right-clicked with a key of the Bifrost, the key floats over it and its holder is taken to
 * Eternity. Registered by the 3.8 skeleton with its block entity, facing and generated state; lane L8 owns the
 * behaviour and its renderer.
 */
public class EternityAltarBlock extends BaseEntityBlock {
    public static final MapCodec<EternityAltarBlock> CODEC = simpleCodec(EternityAltarBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /**
     * Placed by a template (a generated tower or shrine): unbreakable in Survival (Creative breaks it). A placed one
     * (the creative tab's, for builders) breaks as any metal block.
     */
    public static final BooleanProperty GENERATED = BooleanProperty.create("generated");

    public EternityAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(GENERATED, false));
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
        builder.add(FACING, GENERATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A generated one does not break in Survival. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(GENERATED) && !player.isCreative() ? 0.0F : super.getDestroyProgress(state, player, level,
                pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EternityAltarBlockEntity(pos, state);
    }
}
