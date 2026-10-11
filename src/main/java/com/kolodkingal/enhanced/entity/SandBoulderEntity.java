package com.kolodkingal.enhanced.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

/**
 * A boulder of Apocalypse's sand. A stub: its flight and its damage are built by the Apocalypse lane (DESIGN section
 * 17); until then it is only registered and cannot be summoned.
 */
public class SandBoulderEntity extends ThrowableProjectile {
    public SandBoulderEntity(EntityType<? extends SandBoulderEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
