package com.kolodkingal.enhanced.ultron;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Vibranium Key that a Sentry lifts to start Sokovia's rise. A stub: what it does is built by the Ultron lane
 * (DESIGN section 16); until then it is only registered and cannot be summoned.
 */
public class VibraniumKeyEntity extends Entity {
    public VibraniumKeyEntity(EntityType<? extends VibraniumKeyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
