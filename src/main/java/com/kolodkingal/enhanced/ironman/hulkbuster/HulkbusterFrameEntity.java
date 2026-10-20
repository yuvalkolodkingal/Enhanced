package com.kolodkingal.enhanced.ironman.hulkbuster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Hulkbuster's frame that Veronica drops (3.8, PLAN 6.4): it falls from the sky, opens, takes Iron Man in and
 * closes round him (its renderer draws L7's open/close timeline from its synced age). Registered by the 3.8 skeleton as
 * a plain entity (drawn as a placeholder box); lane L4 owns the class.
 */
public class HulkbusterFrameEntity extends Entity {
    public HulkbusterFrameEntity(EntityType<? extends HulkbusterFrameEntity> type, Level level) {
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
