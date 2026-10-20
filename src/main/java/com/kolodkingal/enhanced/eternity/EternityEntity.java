package com.kolodkingal.enhanced.eternity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Eternity (3.8, PLAN 10.4): the giant seated, meditating figure of the realm, one per realm, kept by {@code
 * EternityRealm}. Not a living thing: no health, no AI, invulnerable, not pushable, saved with its chunk. Registered by
 * the 3.8 skeleton as a plain entity (drawn as a placeholder box); lane L8 owns the class and its look.
 */
public class EternityEntity extends Entity {
    public EternityEntity(EntityType<? extends EternityEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
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
