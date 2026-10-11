package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * An illusion of Thanos that the Reality Stone throws up: identical to him, it walks at the players and jabs for a few
 * points of damage. It has one point of health: anything that touches it dissolves it, and it dissolves by itself after
 * forty seconds, with the fight and with him. It drops nothing and is never saved.
 */
public class ThanosPhantomEntity extends Monster {
    /** An illusion does not last longer than this (ticks). */
    public static final int LIFETIME = 800;

    private int life;

    public ThanosPhantomEntity(EntityType<? extends ThanosPhantomEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 3.5)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.4);
    }

    /** Sets its jab to the damage of the Titan that made it (his stones and his rage included). */
    void prepare(ThanosEntity thanos) {
        AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(ThanosBossConfig.PHANTOM_DAMAGE.get() * thanos.damageScale());
        }
        setHealth(1.0F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && ++life > LIFETIME) {
            dissolve();
        }
    }

    /** One touch is enough. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !isAlive()) {
            return false;
        }
        dissolve();
        return true;
    }

    /** It comes apart in red light. */
    public void dissolve() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.color(), 1.5F), getX(), getY() + 1.6, getZ(), 24, 0.5, 1.4, 0.5, 0.08);
            ThanosSounds.DISSOLVE.play(level, position());
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        // An illusion is never saved.
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
    }
}
