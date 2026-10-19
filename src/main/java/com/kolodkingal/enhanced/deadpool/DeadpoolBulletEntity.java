package com.kolodkingal.enhanced.deadpool;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * One of Deadpool's cartoon bullets (3.7): a bright tracer, no gravity, never breaks a block; a projectile (the Reality
 * Stone bubbles it, shields stop it; {@code #minecraft:is_projectile}). Registered by the 3.7 skeleton as an inert
 * projectile (drawn as nothing); lane L3 owns the class and its renderer.
 */
public class DeadpoolBulletEntity extends Projectile {
    public DeadpoolBulletEntity(EntityType<? extends DeadpoolBulletEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
