package com.kolodkingal.enhanced.eternity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

/**
 * Eternity (3.8, PLAN 10.4): the giant seated, meditating figure of the realm, one per realm, kept by {@link
 * EternityRealm} at {@link EternityRealm#ETERNITY}, facing south. Not a living thing: no health, no AI, invulnerable,
 * not pushable, never moved, no gravity. He is never saved with his chunk ({@link #shouldBeSaved}): the realm makes him
 * when a traveller arrives and none sits there, and lets him go when the realm is empty, so there are never two. How he
 * looks (his starfield body, his helmet and horns, his breath, the head that turns toward the traveller) is the
 * client's ({@code client/eternity/EternityRenderer}).
 */
public class EternityEntity extends Entity {
    public EternityEntity(EntityType<? extends EternityEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    /** Never saved: the realm makes him again for the next traveller. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 320.0 * 320.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
