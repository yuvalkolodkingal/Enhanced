package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ultron, the boss: Prime, and the bodies he is uploaded into when one is destroyed. It is <em>one</em> entity that
 * changes bodies ({@link #bodyIndex}, with {@link UltronBodies} saying what each is), so that his boss bar, his aim and
 * the fight never start over: when a body would die, the fight takes over and moves him into a Sentry
 * ({@link UltronFight#onBodyDestroyed}), and he really dies only on {@code /kill} or when the fight is over.
 *
 * <p>The body hovers (no gravity, no pathfinding) and is steered by {@link UltronBrain}, which chooses the six attacks
 * of {@link UltronAttack} and runs them with their tells. What the clients need to animate it is synced: the body, the
 * state ({@link UltronState}) with the game time it began, and the id of what it is aimed at.
 *
 * <p>What makes it a boss that cannot be cheesed: one hit takes at most {@code maxHitFraction} of its health (before
 * armour), explosions are halved, a staggered body takes 30 percent more, every mob effect but Instant Damage and Instant
 * Health is refused (it is a machine), fire, drowning, Poison and Wither do nothing, Quicksilver Time does not slow it
 * ({@link BossHooks#slowFactor}), and it cannot die while the fight says it must not (Prime keeps 10 percent until the
 * island is up, and a body that is being replaced or is in its long death cannot be hurt at all). It is never saved, and
 * never despawns.
 */
public class UltronEntity extends Monster implements BossHooks {
    /** One blow of a stagger does this much more. */
    public static final float STAGGER_BONUS = 1.3F;

    private static final EntityDataAccessor<Integer> DATA_BODY = SynchedEntityData.defineId(UltronEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(UltronEntity.class, EntityDataSerializers.BYTE);
    /** The game time the current state began, so that every client animates a tell the same way. */
    private static final EntityDataAccessor<Long> DATA_STATE_TICK = SynchedEntityData.defineId(UltronEntity.class, EntityDataSerializers.LONG);
    /** The id of the creature he is aimed at (the clients draw his glint and aim lines to it), or -1. */
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(UltronEntity.class, EntityDataSerializers.INT);

    /** The most health a creature can have: the vanilla attribute's limit. */
    private static final float HEALTH_LIMIT = 1024.0F;

    private final UltronBrain brain = new UltronBrain();
    /** The health the design asks for (see {@link #designHealth}). */
    private float designHealth = 600.0F;
    /** Nothing can hurt him (awakening, the lift, a transfer, the long death) but what bypasses invulnerability. */
    private boolean shielded;
    /** Paused: the lift carries him, or something holds him still. */
    private boolean held;
    /** The blow that is being taken, so that {@link #setHealth} knows who struck it. */
    @Nullable
    private DamageSource hurtSource;
    @Nullable
    private UltronFight cachedFight;

    public UltronEntity(EntityType<? extends UltronEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FLYING_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BODY, 0);
        builder.define(DATA_STATE, UltronState.IDLE.id());
        builder.define(DATA_STATE_TICK, 0L);
        builder.define(DATA_TARGET, -1);
    }

    // ---- bodies ----

    /** Which body he is in: 0 is Prime, 1 the Hybrid, 2 and after the Sentry-Ultrons. */
    public int bodyIndex() {
        return entityData.get(DATA_BODY);
    }

    public UltronBodies.Body body() {
        return UltronBodies.of(bodyIndex());
    }

    /**
     * Moves him into body {@code index} for a fight of {@code participants}: its health (full), armour, toughness and
     * size. The health is scaled by {@code [boss].difficultyScale}.
     */
    public void applyBody(int index, int participants) {
        UltronBodies.Body body = UltronBodies.of(index);
        entityData.set(DATA_BODY, body.index());
        setHealthPool(UltronBodies.health(index, participants) * BossConfig.DIFFICULTY_SCALE.get().floatValue());
        setBase(Attributes.ARMOR, body.armor());
        setBase(Attributes.ARMOR_TOUGHNESS, body.toughness());
        refreshDimensions();
        super.setHealth(getMaxHealth());
    }

    /** Changes the first body's maximum health for a fight that grew, keeping the fraction of health he has left. */
    public void rescaleHealth(int participants) {
        float fraction = getHealth() / getMaxHealth();
        setHealthPool(UltronBodies.health(bodyIndex(), participants) * BossConfig.DIFFICULTY_SCALE.get().floatValue());
        super.setHealth(Math.max(1.0F, getMaxHealth() * fraction));
    }

    /**
     * The health the design asks for. A creature's maximum health cannot be more than 1024 (the vanilla attribute's
     * limit), and a fight of four or more makes the design ask for more: the attribute then holds 1024 and every blow is
     * made smaller in the same proportion ({@link #adjust}), which is the same fight with a smaller number.
     */
    public float designHealth() {
        return designHealth;
    }

    private void setHealthPool(float design) {
        designHealth = design;
        setBase(Attributes.MAX_HEALTH, Math.min(design, HEALTH_LIMIT));
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(UltronBodies.of(bodyIndex()).hitboxScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_BODY.equals(key)) {
            refreshDimensions();
        }
    }

    // ---- what he is doing ----

    public UltronState state() {
        return UltronState.byId(entityData.get(DATA_STATE));
    }

    /** The game time the current state began. */
    public long stateTick() {
        return entityData.get(DATA_STATE_TICK);
    }

    public void setState(UltronState state) {
        if (state() != state) {
            entityData.set(DATA_STATE, state.id());
            entityData.set(DATA_STATE_TICK, level().getGameTime());
        }
    }

    /** The creature he is aimed at, for the effects, or null. */
    @Nullable
    public LivingEntity aimedAt() {
        int id = entityData.get(DATA_TARGET);
        return id < 0 || !(level().getEntity(id) instanceof LivingEntity living) ? null : living;
    }

    void setAimedAt(@Nullable LivingEntity target) {
        entityData.set(DATA_TARGET, target == null ? -1 : target.getId());
    }

    public UltronBrain brain() {
        return brain;
    }

    /** Whether the body is staggered after a dive: grounded, eyes blue, and hurt 30 percent more. */
    public boolean isStaggered() {
        return state() == UltronState.STAGGER;
    }

    /** Whether nothing can hurt him but what bypasses invulnerability. */
    public boolean isShielded() {
        return shielded;
    }

    public void setShielded(boolean shielded) {
        this.shielded = shielded;
    }

    /** Pauses him completely (the lift carries him) or lets him act again. */
    public void setHeld(boolean held) {
        this.held = held;
    }

    public boolean isHeld() {
        return held;
    }

    /** The fight he belongs to, or null if he belongs to none (then he is not a boss at all). */
    @Nullable
    public UltronFight fight() {
        if (cachedFight != null && !cachedFight.isFinished()) {
            return cachedFight;
        }
        cachedFight = BossFights.fightOf(this) instanceof UltronFight fight ? fight : null;
        return cachedFight;
    }

    // ---- life ----

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        UltronFight fight = fight();
        if (fight == null) {
            discard(); // a body without a fight is nothing
            return;
        }
        if (!held) {
            brain.tick(this, fight);
        }
    }

    // ---- what he takes ----

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.isInvulnerableTo(source);
        }
        return shielded || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.WITHER)
                || super.isInvulnerableTo(source);
    }

    /**
     * What a blow does to a body in the end, before armour: an explosion is halved, a staggered body takes 30 percent
     * more, and no hit takes more than {@code maxHitFraction} of its health. The void and {@code /kill} (what bypasses
     * invulnerability) are not touched.
     */
    public float adjust(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            amount *= UltronBossConfig.EXPLOSION_DAMAGE_FACTOR.get().floatValue();
        }
        if (isStaggered()) {
            amount *= STAGGER_BONUS;
        }
        // The cap is a fraction of the health the design asks for; what the attribute cannot hold is taken off the blow.
        return Math.min(amount, designHealth * UltronBossConfig.MAX_HIT_FRACTION.get().floatValue()) * (getMaxHealth() / designHealth);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        hurtSource = source;
        try {
            super.actuallyHurt(source, amount);
        } finally {
            hurtSource = null;
        }
    }

    /**
     * Where every lethal blow, from every hero, is seen: a body that would drop to nothing is handed to the fight
     * instead ({@link UltronFight#onBodyDestroyed}), which moves him into a Sentry or begins his death; and while the
     * fight says he must not fall (Prime before the island is up) his health stops at its floor. Only {@code /kill} and
     * a fight that is gone let him die.
     */
    @Override
    public void setHealth(float health) {
        if (!level().isClientSide) {
            UltronFight fight = fight();
            if (fight != null) {
                DamageSource source = hurtSource;
                boolean kill = source != null && source.is(DamageTypes.GENERIC_KILL);
                if (health <= 0.0F && !kill && fight.onBodyDestroyed(this, source, null)) {
                    // He is not dead: the fight has him (before the island is up, on his floor of health).
                    super.setHealth(Math.max(1.0F, fight.healthFloor(this)));
                    return;
                }
                if (health > 0.0F || kill) {
                    health = kill ? health : Math.max(health, fight.healthFloor(this));
                }
            }
        }
        super.setHealth(health);
    }

    /** Every mob effect is refused (a machine) but Instant Damage and Instant Health. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.is(MobEffects.HARM) || effect.is(MobEffects.HEAL);
    }

    // ---- hooks for the heroes ----

    /** Quicksilver Time does not slow a machine on a network. */
    @Override
    public int slowFactor(int zoneFactor) {
        return 0;
    }

    // ---- a boss that stays ----

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }
}
