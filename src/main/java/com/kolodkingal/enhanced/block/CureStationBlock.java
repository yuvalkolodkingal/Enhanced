package com.kolodkingal.enhanced.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Hank's Cure Station in the lab's medical bay (3.7): right-clicked with a glass bottle it gives one Mutant Cure (once
 * per player per {@code [school_lab] cureStationCooldown}); with an empty hand it says what the Cure does. Registered
 * by the 3.7 skeleton; lane L4 owns the class.
 */
public class CureStationBlock extends Block {
    public static final MapCodec<CureStationBlock> CODEC = simpleCodec(CureStationBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public CureStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
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
}
