package com.kolodkingal.enhanced.block.asgard;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Bifrost mechanism (3.5): a gold plate in the Observatory's floor under the Hofund socket; above it turns a gold ring
 * (its block entity's renderer), which spins up whenever the Bifrost opens or lands within {@code 24} blocks.
 */
public class BifrostMechanismBlock extends BaseEntityBlock {
    public static final MapCodec<BifrostMechanismBlock> CODEC = simpleCodec(BifrostMechanismBlock::new);

    public BifrostMechanismBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BifrostMechanismBlockEntity(pos, state);
    }
}
