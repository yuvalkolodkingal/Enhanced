package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eternity's altar's block entity (3.8): the key floating over it and its traveller while it charges. Registered by the
 * 3.8 skeleton empty; lane L8 owns the class.
 */
public class EternityAltarBlockEntity extends BlockEntity {
    public EternityAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ETERNITY_ALTAR.get(), pos, state);
    }
}
