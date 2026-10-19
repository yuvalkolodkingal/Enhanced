package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Weapon X rack's block entity (3.7): who has taken an injection from it. Registered by the 3.7 skeleton empty;
 * lane L4 owns the class.
 */
public class AdamantiumRackBlockEntity extends BlockEntity {
    public AdamantiumRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADAMANTIUM_RACK.get(), pos, state);
    }
}
