package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A suit display's block entity (3.8): the {@code SuitKind} it shows and who has taken one ({@code Map<UUID,
 * Integer>}). Registered by the 3.8 skeleton empty; lane L6 owns the class.
 */
public class SuitDisplayBlockEntity extends BlockEntity {
    public SuitDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_DISPLAY.get(), pos, state);
    }
}
