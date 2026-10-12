package com.kolodkingal.enhanced.ultron;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Vibranium Key: the film's Doomsday Trigger, a steel gear drum with three toothed spokes set in a crater of cracked
 * stone, that Ultron's Sentries lift to start Sokovia's rise. It is an entity, not a block, and does nothing by itself:
 * it emerges from the ground at the lift's warning (rising over {@link #EMERGE_TICKS}), spins and glows while the lift
 * lasts, and powers down when Ultron falls. The clients draw it from the two game times it keeps in its synced data, so
 * that every client sees the same stage of it. It cannot be hurt, picked, pushed or walked into, is never saved, and
 * belongs to the fight that made it.
 */
public class VibraniumKeyEntity extends Entity {
    /** The key rises out of the ground over this many ticks. */
    public static final int EMERGE_TICKS = 60;
    /** The glow fades over this many ticks when it powers down. */
    public static final int POWER_DOWN_TICKS = 60;

    private static final EntityDataAccessor<Long> DATA_EMERGED_AT = SynchedEntityData.defineId(VibraniumKeyEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_POWERED_DOWN_AT = SynchedEntityData.defineId(VibraniumKeyEntity.class, EntityDataSerializers.LONG);

    public VibraniumKeyEntity(EntityType<? extends VibraniumKeyEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setInvulnerable(true);
    }

    /** The game time it began to emerge, or -1 if it has not. */
    public long emergedAt() {
        return entityData.get(DATA_EMERGED_AT);
    }

    /** The game time it began to power down, or -1 if it has not. */
    public long poweredDownAt() {
        return entityData.get(DATA_POWERED_DOWN_AT);
    }

    /** Starts the rise out of the ground (at the lift's warning). */
    public void emerge() {
        if (emergedAt() < 0) {
            entityData.set(DATA_EMERGED_AT, level().getGameTime());
        }
    }

    /**
     * Puts the key in a stage for the model viewer ({@code /enhanced_model}): it began to rise {@code age} ticks ago, and
     * if {@code down} is given it began to power down that many ticks ago. Client only.
     */
    public void showStage(int age, int down) {
        entityData.set(DATA_EMERGED_AT, level().getGameTime() - age);
        entityData.set(DATA_POWERED_DOWN_AT, down < 0 ? -1L : level().getGameTime() - down);
    }

    /** Starts the glow's fade (at the victory). */
    public void powerDown() {
        if (poweredDownAt() < 0) {
            entityData.set(DATA_POWERED_DOWN_AT, level().getGameTime());
        }
    }

    /** How far it has risen, from 0 (buried) to 1 (up), at the game time {@code now}. */
    public float emergence(long now) {
        long start = emergedAt();
        return start < 0 ? 0.0F : Math.min(1.0F, Math.max(0.0F, (now - start) / (float) EMERGE_TICKS));
    }

    /** How brightly it glows, from 0 to 1, at the game time {@code now}. */
    public float glow(long now) {
        long down = poweredDownAt();
        float lit = emergence(now);
        if (down < 0) {
            return lit;
        }
        return lit * Math.max(0.0F, 1.0F - (now - down) / (float) POWER_DOWN_TICKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EMERGED_AT, -1L);
        builder.define(DATA_POWERED_DOWN_AT, -1L);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
