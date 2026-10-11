package com.kolodkingal.enhanced.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A boulder of Apocalypse's sand: a block of it torn out of the ground and hurled, 1.3 blocks a tick with a little
 * gravity, 11 damage ({@code boulderDamage}) and a shove of 0.8. It shatters in a puff.
 */
public class SandBoulderEntity extends AbstractSandProjectile {
    public SandBoulderEntity(EntityType<? extends SandBoulderEntity> type, Level level) {
        super(type, level, 11.0F, 0.8F);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.012;
    }
}
