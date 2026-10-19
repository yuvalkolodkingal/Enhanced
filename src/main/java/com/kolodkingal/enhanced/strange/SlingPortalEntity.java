package com.kolodkingal.enhanced.strange;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * One ring of a pair of Sling-Ring portals (3.6, Doctor Strange's N and the Sling Ring): a vertical ring of orange
 * sparks; whoever passes through comes out of its twin. Never saved. Registered by the 3.6 skeleton as an inert entity
 * (drawn as nothing); lane L3 owns the class and its renderer.
 */
public class SlingPortalEntity extends Entity {
    public SlingPortalEntity(EntityType<? extends SlingPortalEntity> type, Level level) {
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
