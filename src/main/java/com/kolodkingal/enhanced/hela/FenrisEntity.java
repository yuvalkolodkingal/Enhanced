package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Fenris (3.5): Hela's giant wolf, 2.4 blocks high, dark grey-black fur, scars and green-glowing eyes. He bites
 * ({@code fenris_bite}, 10), pounces on a target up to {@link #POUNCE_RANGE} blocks away and howls ({@link
 * #HOWL_RADIUS} blocks: Slowness I for four seconds to everything on the other side). In the boss's fight he has a boss
 * bar of his own; called by the hero Hela he fights for her for a minute. His actions are synced ({@link #action()}) for
 * the model's poses.
 */
public class FenrisEntity extends ArmyMob {
    public static final int RISE_TICKS = 30;
    public static final double POUNCE_RANGE = 6.0;
    public static final double HOWL_RADIUS = 10.0;
    public static final float BITE = 10.0F;
    /** How long each of his actions is drawn. */
    public static final int POUNCE_TICKS = 14;
    public static final int HOWL_TICKS = 40;
    public static final int BITE_TICKS = 8;

    /** What he does: the {@code ACT_*} constant, and the game time it began. */
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(FenrisEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ACTION_AT = SynchedEntityData.defineId(FenrisEntity.class, EntityDataSerializers.LONG);
    public static final byte ACT_NONE = 0;
    public static final byte ACT_POUNCE = 1;
    public static final byte ACT_HOWL = 2;
    public static final byte ACT_BITE = 3;

    public FenrisEntity(EntityType<? extends FenrisEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140.0)
                .add(Attributes.ATTACK_DAMAGE, BITE)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACT_NONE);
        builder.define(DATA_ACTION_AT, 0L);
    }

    @Override
    public int riseTicks() {
        return RISE_TICKS;
    }

    /** Sets his health for a fight ({@code [hela_boss].fenrisHealth}). */
    public void setBody(double health) {
        var max = getAttribute(Attributes.MAX_HEALTH);
        if (max != null) {
            max.setBaseValue(health);
        }
        setHealth((float) health);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HowlGoal(this));
        goalSelector.addGoal(2, new PounceGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.3));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerTargetGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
        addArmyTargets(3);
    }

    // ---- his actions ----

    public byte action() {
        return entityData.get(DATA_ACTION);
    }

    /** Ticks (with the partial tick) since his action began. */
    public float actionTime(float partial) {
        return Math.max(0.0F, level().getGameTime() + partial - entityData.get(DATA_ACTION_AT));
    }

    private void act(byte action) {
        entityData.set(DATA_ACTION, action);
        entityData.set(DATA_ACTION_AT, level().getGameTime());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && action() != ACT_NONE) {
            float t = actionTime(0.0F);
            int length = switch (action()) {
                case ACT_POUNCE -> POUNCE_TICKS;
                case ACT_HOWL -> HOWL_TICKS;
                default -> BITE_TICKS;
            };
            if (t >= length) {
                act(ACT_NONE);
            }
        }
    }

    /** His bite: {@code fenris_bite}, the attack damage of his attribute. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity victim) || rising()) {
            return false;
        }
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        LivingEntity owner = ownerId() != null ? owner() : this;
        boolean hit = HelaRules.hurt(victim, ModDamageTypes.FENRIS_BITE, owner, this, damage);
        if (hit) {
            act(ACT_BITE);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.FENRIS_BITE.get(), SoundSource.HOSTILE, 1.2F, 0.9F + random.nextFloat() * 0.2F);
            Vec3 push = victim.position().subtract(position());
            if (push.horizontalDistanceSqr() > 1.0E-4) {
                victim.knockback(0.6, -push.x, -push.z);
            }
        }
        return hit;
    }

    /** The howl: Slowness I for four seconds to everything he may hurt within {@link #HOWL_RADIUS}. */
    public void howl() {
        act(ACT_HOWL);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.FENRIS_HOWL.get(), SoundSource.HOSTILE, 2.5F, 1.0F);
        LivingEntity owner = ownerId() != null ? owner() : this;
        if (owner == null) {
            return;
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HOWL_RADIUS),
                e -> e.distanceToSqr(this) <= HOWL_RADIUS * HOWL_RADIUS && HelaRules.canHurt(owner, e))) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        }
    }

    /** The pounce: he leaps at {@code target}. */
    public void pounce(LivingEntity target) {
        act(ACT_POUNCE);
        Vec3 to = target.position().subtract(position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        double length = Math.max(0.1, flat.length());
        double reach = Math.min(length, POUNCE_RANGE + 1.0);
        setDeltaMovement(flat.normalize().scale(reach * 0.21).add(0.0, 0.48, 0.0));
        hasImpulse = true;
        level().playSound(null, getX(), getY(), getZ(), ModSounds.FENRIS_GROWL.get(), SoundSource.HOSTILE, 1.4F, 0.8F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.FENRIS_GROWL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.FENRIS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FENRIS_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Every eight to twelve seconds with a target near: he throws back his head and howls. */
    static final class HowlGoal extends Goal {
        private final FenrisEntity wolf;
        private long nextAt;

        HowlGoal(FenrisEntity wolf) {
            this.wolf = wolf;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = wolf.getTarget();
            long now = wolf.level().getGameTime();
            if (wolf.rising() || target == null || now < nextAt || wolf.distanceToSqr(target) > HOWL_RADIUS * HOWL_RADIUS) {
                return false;
            }
            if (nextAt == 0L) {
                nextAt = now + 60; // (not the moment he arrives)
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            nextAt = wolf.level().getGameTime() + 160 + wolf.random.nextInt(80);
            wolf.getNavigation().stop();
            wolf.howl();
        }

        @Override
        public boolean canContinueToUse() {
            return wolf.action() == ACT_HOWL && wolf.actionTime(0.0F) < 20;
        }
    }

    /** With a target 3 to {@link #POUNCE_RANGE} blocks away, on the ground, every four to six seconds: he leaps. */
    static final class PounceGoal extends Goal {
        private final FenrisEntity wolf;
        private long nextAt;

        PounceGoal(FenrisEntity wolf) {
            this.wolf = wolf;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = wolf.getTarget();
            if (wolf.rising() || target == null || !wolf.onGround() || wolf.level().getGameTime() < nextAt) {
                return false;
            }
            double d = wolf.distanceToSqr(target);
            return d >= 3.0 * 3.0 && d <= POUNCE_RANGE * POUNCE_RANGE && wolf.hasLineOfSight(target);
        }

        @Override
        public void start() {
            nextAt = wolf.level().getGameTime() + 80 + wolf.random.nextInt(40);
            LivingEntity target = wolf.getTarget();
            if (target != null) {
                wolf.getLookControl().setLookAt(target);
                wolf.pounce(target);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !wolf.onGround() && wolf.actionTime(0.0F) < POUNCE_TICKS;
        }

        @Override
        public void tick() {
            LivingEntity target = wolf.getTarget();
            if (target != null && wolf.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
                wolf.doHurtTarget(target);
            }
        }
    }
}
