package com.kolodkingal.enhanced.falcon;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * One of Falcon's energy darts (3.8, PLAN 8.4): bright red, no gravity, never breaks a block; a projectile ({@code
 * #minecraft:is_projectile}). Registered by the 3.8 skeleton as an inert projectile (drawn as nothing); lane L5 owns
 * the class and its renderer.
 */
public class FalconDartEntity extends Projectile {
    public FalconDartEntity(EntityType<? extends FalconDartEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
