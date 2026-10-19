package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The stone pedestal's block entity (3.6): the players who took its stone ({@code player} mode). Registered by the 3.6
 * skeleton empty; lane L1 owns the class.
 */
public class StonePedestalBlockEntity extends BlockEntity {
    public StonePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_PEDESTAL.get(), pos, state);
    }
}
