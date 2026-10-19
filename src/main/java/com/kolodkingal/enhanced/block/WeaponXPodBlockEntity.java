package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Weapon X Pod's block entity (3.7): who is inside and how far the treatment is (lane L3), its light and bubbles
 * (lane L4's renderer). Registered by the 3.7 skeleton empty.
 */
public class WeaponXPodBlockEntity extends BlockEntity {
    public WeaponXPodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEAPON_X_POD.get(), pos, state);
    }
}
