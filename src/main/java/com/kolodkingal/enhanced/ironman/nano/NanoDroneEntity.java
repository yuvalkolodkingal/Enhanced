package com.kolodkingal.enhanced.ironman.nano;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * One of the Mark 85's nanotech drones (3.8, PLAN 6.2): it forms behind his shoulder, follows him and fires strong
 * lasers; no health (a hit makes it flicker), it ends with him. Registered by the 3.8 skeleton as a plain entity (drawn
 * as a placeholder box); lane L4 owns the class.
 */
public class NanoDroneEntity extends Entity {
    public NanoDroneEntity(EntityType<? extends NanoDroneEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
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
