package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Gungnir's stand's block entity (3.5): it keeps nothing; it is there so that the client draws the spear on the stand. */
public class GungnirStandBlockEntity extends BlockEntity {
    public GungnirStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUNGNIR_STAND.get(), pos, state);
    }
}
