package com.kolodkingal.enhanced.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A cocoon of Apocalypse's sand around a creature he entombed. A stub: what it does is built by the Apocalypse lane
 * (DESIGN section 17); until then it is only registered and cannot be summoned.
 */
public class SandCocoonEntity extends Entity {
    public SandCocoonEntity(EntityType<? extends SandCocoonEntity> type, Level level) {
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
