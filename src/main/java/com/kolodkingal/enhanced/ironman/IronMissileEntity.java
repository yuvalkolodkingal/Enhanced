package com.kolodkingal.enhanced.ironman;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * A rocket or mini-missile of Iron Man's suits (3.8): the Mark 1's arm rocket, the Mark 3's shoulder missiles, the Mark
 * 85's homing ones ({@link MissileGuidance}), and, as a second entity type of the same class, Falcon's Wrist Rocket
 * ({@code falcon_rocket}). Registered by the 3.8 skeleton as an inert projectile (drawn as nothing); lane L3 owns the
 * class (its flight, boom and damage), lane L4 the guidance.
 */
public class IronMissileEntity extends Projectile {
    public IronMissileEntity(EntityType<? extends IronMissileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
