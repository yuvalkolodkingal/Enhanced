package com.kolodkingal.enhanced.captain;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * Captain America's thrown Vibranium Shield (3.8, PLAN 3.4): it flies along his look, hits, ricochets, turns home and
 * flies back to the hand it left (the flight home mirrors {@code ThrownWeaponEntity}'s). Registered by the 3.8 skeleton
 * as an inert projectile (drawn as nothing); lane L1 owns the class and its renderer.
 */
public class ThrownShieldEntity extends Projectile {
    public ThrownShieldEntity(EntityType<? extends ThrownShieldEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
