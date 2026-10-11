package com.kolodkingal.enhanced.thanos;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * Thanos's Double-Edged Sword, thrown. A stub: its flight and its damage are built by the Thanos lane (DESIGN section
 * 15); until then it is only registered and cannot be summoned.
 */
public class ThanosSwordEntity extends Projectile {
    public ThanosSwordEntity(EntityType<? extends ThanosSwordEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
