package com.kolodkingal.enhanced.hydra;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * A Hydra energy bolt (3.6): Tesseract blue, no gravity, never breaks a block, passes through Hydra; a projectile (the
 * Reality Stone bubbles it, shields stop it). Registered by the 3.6 skeleton as an inert projectile (drawn as
 * nothing); lane L8 owns the class and its renderer.
 */
public class HydraBoltEntity extends Projectile {
    public HydraBoltEntity(EntityType<? extends HydraBoltEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
