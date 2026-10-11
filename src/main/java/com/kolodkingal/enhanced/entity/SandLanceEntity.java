package com.kolodkingal.enhanced.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A lance of Apocalypse's sand: one of the five thrown in a flat fan by his raised hand, 1.9 blocks a tick, no gravity,
 * 6 damage ({@code lanceDamage}). A thin ochre streak with a trail of grains (the client draws it).
 */
public class SandLanceEntity extends AbstractSandProjectile {
    public SandLanceEntity(EntityType<? extends SandLanceEntity> type, Level level) {
        super(type, level, 6.0F, 0.0F);
    }
}
