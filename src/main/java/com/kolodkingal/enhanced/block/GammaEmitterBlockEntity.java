package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Gamma Emitter's block entity (3.6): its warm-up, beam and cooldown. Registered by the 3.6 skeleton empty; lane
 * L7 owns the class.
 */
public class GammaEmitterBlockEntity extends BlockEntity {
    public GammaEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAMMA_EMITTER.get(), pos, state);
    }
}
