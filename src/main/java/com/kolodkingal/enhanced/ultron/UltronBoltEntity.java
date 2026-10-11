package com.kolodkingal.enhanced.ultron;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * A bolt of Ultron's plasma. A stub: its flight and its damage are built by the Ultron lane (DESIGN section 16); until
 * then it is only registered and cannot be summoned.
 */
public class UltronBoltEntity extends Projectile {
    public UltronBoltEntity(EntityType<? extends UltronBoltEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
