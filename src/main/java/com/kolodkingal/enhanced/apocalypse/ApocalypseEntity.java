package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Apocalypse, the boss: En Sabah Nur, the first mutant, 2.9 blocks tall. He is a body with a controller: this class is
 * the body (its health and the rules of what can hurt it, its attributes in each form, what is synced for the clients to
 * draw) and {@link ApocalypseBrain} is the controller that chooses and runs his attacks; the fight ({@link
 * ApocalypseFight}) drives both through the phases of the film.
 *
 * <p>What makes it a boss that cannot be cheesed, all in {@link #hurt}: while his shield is up everything but mind power
 * does <em>exactly nothing</em> (no damage, no knockback, no invulnerability frames), and mind power wears the shield
 * instead of him; while it is forming, re-forming or the fight is in a set piece he cannot be hurt; in a window (the
 * shield broken) and in the true form everything hurts, but one hit takes at most {@code maxHitFraction} of his health,
 * explosions are halved, and his health has a <em>floor</em> ({@link #setHealth}) below which nobody can bring it, so
 * the cinematics always happen. Every mob effect but Glowing is refused, fire, drowning, freezing and the rest do
 * nothing, and Quicksilver Time slows him only for a while ({@link #slowFactor}: he adapts, as in the film).
 *
 * <p>Since 3.1 (Eilon's items 8 and 9) his health never heals by itself (the fight's {@code regenPerSecond} and {@code
 * trueFormRegenPerSecond} are 0, and the game's own healing is refused, {@link #heal(float)}), and the mind power of a
 * Scarlet Witch or a Phoenix is <em>critical</em> ({@link MindPower#isCritical}): it wears his shield {@code
 * critShieldFactor} times as much (their strains too) and hurts his body {@code critBodyFactor} times as much, while
 * every other blow to his body counts {@code othersBodyFactor} times; a critical hit is seen and heard ({@link
 * #critEffects}).
 *
 * <p>His maximum health is more than the 1024 the game allows for fights of more than one: the attribute then holds
 * 1024, the fight's design number is {@link #designMax}, and every blow is made smaller in the same proportion, which is
 * the same fight with a smaller number. He is never saved, never despawns, and cannot be summoned.
 */
public class ApocalypseEntity extends Monster implements BossHooks {
    /** The game's limit on a creature's maximum health. */
    private static final float HEALTH_LIMIT = 1024.0F;
    /** The action-bar line of a critical hit, to its attacker: "Critical hit! x%s". */
    public static final String CRIT_MESSAGE = "message.enhanced.apocalypse.crit";
    /** One attacker's critical hits are seen and heard at most once in this many ticks. */
    public static final int CRIT_EFFECT_TICKS = 10;
    /** The gold of his shield, as the particles and the neutralised projectiles of it are coloured. */
    public static final int SHIELD_GOLD = 0xF5C84A;
    /** The speed of the armoured form and of the true form. */
    public static final double SPEED = 0.28;
    public static final double TRUE_FORM_SPEED = 0.36;

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.BYTE);
    /** The game time the state began, so that every client animates a tell the same way. */
    private static final EntityDataAccessor<Long> DATA_STATE_TICK = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.LONG);
    /** The point his current attack is aimed at (locked at the aim moment). */
    private static final EntityDataAccessor<Vector3f> DATA_AIM = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.VECTOR3);
    /** The id of the player he is tracking, or -1. */
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.INT);
    /** The state of the shield: the ordinal of {@link ApocalypseShield.State}. */
    private static final EntityDataAccessor<Byte> DATA_SHIELD = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.BYTE);
    /** How strong the shield is, 0 to 1 (the cracks the client draws). */
    private static final EntityDataAccessor<Float> DATA_SHIELD_FRAC = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.FLOAT);
    /** The phase, 1 to 3. */
    private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.BYTE);
    /** The armour has fallen: the frail body with the cracked cap. */
    private static final EntityDataAccessor<Boolean> DATA_TRUE_FORM = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.BOOLEAN);
    /** He dies in the Phoenix's fire, not in sand. */
    private static final EntityDataAccessor<Boolean> DATA_FIRE_DEATH = SynchedEntityData.defineId(ApocalypseEntity.class, EntityDataSerializers.BOOLEAN);

    private final ApocalypseShield shield = new ApocalypseShield();
    private final ApocalypseBrain brain = new ApocalypseBrain();
    /** The health the design asks for, more than the attribute can hold in a fight of more than one. */
    private float designMax = 900.0F;
    /** The fraction of his health below which no blow brings it (0: none). */
    private double floorFraction;
    private boolean armourCut;
    /** Open to every blow a quarter harder though the shield is not broken (the Escape was interrupted). */
    private boolean open;
    @Nullable
    private DamageSource hurtSource;
    @Nullable
    private ApocalypseFight cachedFight;
    private long lastDeflectSound = Long.MIN_VALUE / 2;
    // Quicksilver Time: he adapts after adaptTicks and then ignores it for adaptedTicks.
    private long slowedSince = -1;
    private long lastSlowed = Long.MIN_VALUE / 2;
    private long adaptedUntil;
    /** When each Phoenix's touch last strained the shield ({@link MindPower#strainCounts}). */
    private final Map<String, Long> strains = new HashMap<>();
    /** When each attacker's critical hit was last seen and heard ({@link #critEffects}). */
    private final Map<UUID, Long> lastCritEffects = new HashMap<>();
    private boolean lightningStruck;
    /** The model viewer's copy: the tick it is shown at (-1 when this is a real boss), and whether the tick advances. */
    private float shownTick = -1.0F;
    private boolean shownRunning;

    public ApocalypseEntity(EntityType<? extends ApocalypseEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    /**
     * His attributes in the armoured form (the config's numbers are put on when the fight makes him: the config is not
     * loaded yet when the attributes are registered).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.ATTACK_DAMAGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, ApocalypseState.AWAKENING.id());
        builder.define(DATA_STATE_TICK, 0L);
        builder.define(DATA_AIM, new Vector3f());
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_SHIELD, (byte) ApocalypseShield.State.GONE.ordinal());
        builder.define(DATA_SHIELD_FRAC, 0.0F);
        builder.define(DATA_PHASE, (byte) 1);
        builder.define(DATA_TRUE_FORM, false);
        builder.define(DATA_FIRE_DEATH, false);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The body: health, form

    /** The health the design asks for in this fight: {@code bodyHealth x (1 + healthPerExtraPlayer x (n - 1))}. */
    public static float designHealthFor(int fighters) {
        return (float) (ApocalypseBossConfig.BODY_HEALTH.get() * (1.0 + ApocalypseBossConfig.HEALTH_PER_EXTRA_PLAYER.get() * (Math.max(1, Math.min(6, fighters)) - 1))
                * BossConfig.DIFFICULTY_SCALE.get());
    }

    /** Gives him the body of a fight of {@code fighters}: full health and the attributes of the armoured form. */
    public void applyBody(int fighters) {
        designMax = designHealthFor(fighters);
        setBase(Attributes.MAX_HEALTH, Math.min(designMax, HEALTH_LIMIT));
        setBase(Attributes.KNOCKBACK_RESISTANCE, 1.0);
        applyForm();
        super.setHealth(getMaxHealth());
    }

    /** The fight grew (or shrank): the design number changes and he keeps the fraction of health he has. */
    public void rescale(int fighters) {
        float fraction = healthFraction();
        designMax = designHealthFor(fighters);
        setBase(Attributes.MAX_HEALTH, Math.min(designMax, HEALTH_LIMIT));
        super.setHealth(Math.max(1.0F, getMaxHealth() * fraction));
    }

    /** Sets his armour, toughness and speed for his form (and for Magneto's cut). */
    private void applyForm() {
        boolean frail = isTrueForm();
        setBase(Attributes.ARMOR, frail || armourCut ? 0.0 : ApocalypseBossConfig.ARMOR.get());
        setBase(Attributes.ARMOR_TOUGHNESS, frail || armourCut ? 0.0 : ApocalypseBossConfig.ARMOR_TOUGHNESS.get());
        setBase(Attributes.MOVEMENT_SPEED, frail ? TRUE_FORM_SPEED : SPEED);
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** The maximum health the design asks for (the attribute holds at most 1024 of it). */
    public float designMax() {
        return designMax;
    }

    /** How many design points one point of the attribute's health is worth: 1 unless the design asks for more than 1024. */
    public float designPerHealth() {
        return designMax / getMaxHealth();
    }

    /** His health in design points. */
    public float designHealth() {
        return getHealth() * designPerHealth();
    }

    /** The fraction of his full health he has, 0 to 1. */
    public float healthFraction() {
        return getHealth() / getMaxHealth();
    }

    /** The health, in the attribute's points, at a fraction of the design's (rounded up to a whole design point). */
    public float healthAt(double fraction) {
        return (float) (Math.ceil(designMax * fraction) / designPerHealth());
    }

    /** Sets his health to a fraction of his full health, whatever the floor. */
    public void setFraction(double fraction) {
        super.setHealth(Math.min(getMaxHealth(), Math.max(0.0F, healthAt(fraction))));
    }

    /**
     * Heals {@code designPoints} of the design's health, never above {@code ceilingFraction} of it: his healing while a
     * shield is up and in the true form, each 0 by default since 3.1 (only a server that sets them heals him).
     */
    public void heal(float designPoints, double ceilingFraction) {
        float ceiling = healthAt(ceilingFraction);
        if (getHealth() < ceiling) {
            super.setHealth(Math.min(ceiling, getHealth() + designPoints / designPerHealth()));
        }
    }

    /**
     * The game's own healing (a splashed potion of Healing, whose instant effect never asks {@link #canBeAffected}, or
     * another mod's) does nothing: his health never heals but by the fight's rules (3.1, item 8).
     */
    @Override
    public void heal(float amount) {
    }

    /** The fraction below which no blow brings his health (0 for none). */
    public void setFloor(double fraction) {
        this.floorFraction = fraction;
    }

    public double floorFraction() {
        return floorFraction;
    }

    /** The health the floor is at, in the attribute's points. */
    public float floorHealth() {
        return healthAt(floorFraction);
    }

    /**
     * Where every blow lands on his health, whoever deals it: it stops at the floor of the phase, so that nobody can kill
     * him early and the cinematic always happens. Only {@code /kill} brings him lower.
     */
    @Override
    public void setHealth(float health) {
        if (!level().isClientSide() && floorFraction > 0.0 && !(hurtSource != null && hurtSource.is(DamageTypes.GENERIC_KILL))) {
            float floor = Math.min(getHealth(), floorHealth());
            if (health < floor) {
                health = floor;
            }
        }
        super.setHealth(health);
    }

    public boolean isTrueForm() {
        return entityData.get(DATA_TRUE_FORM);
    }

    /** The armour falls (or is put back for a test): the frail body, the smaller hitbox, no armour, quicker. */
    public void setTrueForm(boolean trueForm) {
        entityData.set(DATA_TRUE_FORM, trueForm);
        applyForm();
        refreshDimensions();
    }

    /** Magneto's strip cuts the armour to nothing for the window; it comes back when the window is over. */
    public void setArmourCut(boolean cut) {
        this.armourCut = cut;
        applyForm();
    }

    public boolean isArmourCut() {
        return armourCut;
    }

    /** He is open to every blow, as in a window, with no shield broken: the stagger of an interrupted Escape. */
    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isTrueForm() ? EntityDimensions.scalable(0.8F, 2.5F).withEyeHeight(2.25F) : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_TRUE_FORM.equals(key)) {
            refreshDimensions();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the clients draw

    public ApocalypseState state() {
        return ApocalypseState.byId(entityData.get(DATA_STATE));
    }

    /** The game time the current state began. */
    public long stateTick() {
        return entityData.get(DATA_STATE_TICK);
    }

    /**
     * How long ago the current state began, in ticks with the partial tick (what every pose and every tell is drawn from).
     * A copy in the model viewer is held at the tick it was put at, or runs on from it.
     */
    public float stateTime(float partial) {
        if (shownTick >= 0.0F) {
            return shownTick + (shownRunning ? partial : 0.0F);
        }
        return Math.max(0.0F, level().getGameTime() + partial - stateTick());
    }

    /**
     * The model viewer puts this client-only copy in a state, as it would be {@code tick} ticks into it (see {@code
     * ModelViewer}): nothing here is sent anywhere, and the copy is never a fight's.
     */
    public void showState(ApocalypseState state, int tick, boolean running, boolean trueForm, ApocalypseShield.State shieldState, float shieldFraction,
                          int phase, boolean fire, Vec3 aim) {
        shownRunning = running;
        shownTick = tick;
        entityData.set(DATA_STATE, state.id());
        entityData.set(DATA_STATE_TICK, level().getGameTime() - tick);
        entityData.set(DATA_TRUE_FORM, trueForm);
        entityData.set(DATA_SHIELD, (byte) shieldState.ordinal());
        entityData.set(DATA_SHIELD_FRAC, shieldFraction);
        entityData.set(DATA_PHASE, (byte) phase);
        entityData.set(DATA_FIRE_DEATH, fire);
        entityData.set(DATA_AIM, new Vector3f((float) aim.x, (float) aim.y, (float) aim.z));
    }

    /** Whether this is the model viewer's copy and not a boss of a fight. */
    public boolean shownByViewer() {
        return shownTick >= 0.0F;
    }

    public void setState(ApocalypseState state) {
        if (state() != state) {
            entityData.set(DATA_STATE, state.id());
            entityData.set(DATA_STATE_TICK, level().getGameTime());
        }
    }

    /** The point his attack is aimed at. */
    public Vec3 aim() {
        Vector3f aim = entityData.get(DATA_AIM);
        return new Vec3(aim.x, aim.y, aim.z);
    }

    public void setAim(Vec3 aim) {
        entityData.set(DATA_AIM, new Vector3f((float) aim.x, (float) aim.y, (float) aim.z));
    }

    /** The player he is tracking, or null. */
    @Nullable
    public LivingEntity tracked() {
        int id = entityData.get(DATA_TARGET);
        return id < 0 || !(level().getEntity(id) instanceof LivingEntity living) ? null : living;
    }

    public void setTracked(@Nullable LivingEntity target) {
        entityData.set(DATA_TARGET, target == null ? -1 : target.getId());
    }

    public int phase() {
        return entityData.get(DATA_PHASE);
    }

    public void setPhase(int phase) {
        entityData.set(DATA_PHASE, (byte) phase);
    }

    public boolean diesInFire() {
        return entityData.get(DATA_FIRE_DEATH);
    }

    public void setDiesInFire(boolean fire) {
        entityData.set(DATA_FIRE_DEATH, fire);
    }

    /** Copies the shield to the synced data (once a tick, by the fight). */
    void syncShield() {
        entityData.set(DATA_SHIELD, (byte) shield.state().ordinal());
        entityData.set(DATA_SHIELD_FRAC, shield.fraction());
    }

    /** The state of the shield as the clients know it. */
    public ApocalypseShield.State shieldState() {
        int ordinal = entityData.get(DATA_SHIELD);
        ApocalypseShield.State[] states = ApocalypseShield.State.values();
        return states[Math.max(0, Math.min(states.length - 1, ordinal))];
    }

    public float shieldFraction() {
        return entityData.get(DATA_SHIELD_FRAC);
    }

    public ApocalypseShield shield() {
        return shield;
    }

    public ApocalypseBrain brain() {
        return brain;
    }

    /** The fight he belongs to, or null if he belongs to none (then he is nothing, and is removed). */
    @Nullable
    public ApocalypseFight fight() {
        if (cachedFight != null && !cachedFight.isFinished()) {
            return cachedFight;
        }
        cachedFight = BossFights.fightOf(this) instanceof ApocalypseFight fight ? fight : null;
        return cachedFight;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Life

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        if (fight() == null) {
            discard(); // a boss without a fight is nothing: what a crash or a copied chunk leaves
            return;
        }
        if (!shield.isVulnerable()) {
            neutraliseProjectiles();
        }
    }

    /**
     * Projectiles that reach the sphere of the shield are neutralised before they hit (so an arrow does not stick and
     * bounce): Mjolnir, Stormbreaker and the fragments of a shattered Mjolnir are turned back to Thor, everything else
     * turns to gold dust. (The same as Wanda's shield does, with the gold of this one.)
     */
    private void neutraliseProjectiles() {
        ServerLevel server = (ServerLevel) level();
        Vec3 center = position().add(0, getBbHeight() * 0.5, 0);
        double radius = ApocalypseShield.RADIUS;
        for (Projectile projectile : server.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(radius + 4.0),
                p -> p.getOwner() != this && !BossFights.sameFight(this, p) && !headingHome(p))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toBoss = center.subtract(projectile.position());
            double reach = radius + 0.5 + velocity.length();
            if (velocity.dot(toBoss) > 0 && toBoss.lengthSqr() < reach * reach) {
                neutralise(projectile);
            }
        }
    }

    /**
     * One projectile turned away by the shield. An arrow never reaches {@link #hurt} (it turns to dust first), so the
     * fight is told here too: his line about arrows and the Tower of Babel is for the first arrow as much as the first blow.
     */
    public void neutralise(Projectile projectile) {
        if (level() instanceof ServerLevel server) {
            WandaAbilities.neutralize(server, projectile, SHIELD_GOLD);
            deflectEffects(projectile.position(), true);
            ApocalypseFight fight = fight();
            if (fight != null) {
                fight.onDeflected();
            }
        }
    }

    /** A projectile already on its way home to its owner (a weapon of Thor's, Thanos's sword): it is let by. */
    private static boolean headingHome(Projectile projectile) {
        if (projectile instanceof ShieldCost cost && cost.oneOfAKind()) {
            return cost.returning();
        }
        return projectile instanceof ThrownWeaponEntity weapon ? weapon.isReturning()
                : projectile instanceof MjolnirFragmentEntity fragment && fragment.isReturning();
    }

    // ---------------------------------------------------------------------------------------------------------
    // What he takes

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.isInvulnerableTo(source);
        }
        return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.CRAMMING)
                || super.isInvulnerableTo(source);
    }

    /** The routing of every blow (spec 3.5.3). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) {
            return false;
        }
        ApocalypseFight fight = fight();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // The void and /kill. /kill ends the fight as an abort (no reward); the void does nothing (the fight puts him back).
            if (!source.is(DamageTypes.GENERIC_KILL)) {
                return false;
            }
            if (fight != null) {
                fight.abortKilled();
            }
            return super.hurt(source, amount);
        }
        if (fight == null || fight.bossInvulnerable() || amount <= 0.0F || ApocalypseEvents.sameSide(this, source.getEntity())) {
            return false; // (a shard of War's that flies into him is nothing: no ripple, no chime, no word about arrows)
        }
        fight.noteAttack(source);
        switch (shield.state()) {
            case UP -> {
                float mind = MindPower.shieldDamage(source, amount);
                if (mind > 0.0F) {
                    // A Scarlet Witch's or a Phoenix's mind power is critical: critShieldFactor times the wear.
                    double factor = MindPower.isCritical(source) ? ApocalypseShieldConfig.CRIT_SHIELD_FACTOR.get() : 1.0;
                    return absorb(fight, source, (float) (mind * factor), factor);
                }
                deflect(source);
                return false; // exactly nothing: no damage, no knockback, no invulnerability frames
            }
            case FORMING, REFORMING -> {
                return false;
            }
            default -> {
                // BROKEN or GONE: everything hurts.
                if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                    amount *= ApocalypseBossConfig.EXPLOSION_DAMAGE_FACTOR.get().floatValue();
                }
                // Their mind power is critical on his body; every other blow counts for much less (3.1, item 9).
                boolean critical = MindPower.isCritical(source);
                double factor = critical ? ApocalypseShieldConfig.CRIT_BODY_FACTOR.get() : ApocalypseShieldConfig.OTHERS_BODY_FACTOR.get();
                // In his own points of health (a fight of many has more design points than the game's attribute holds); his armour,
                // the window's quarter more and the cap of one hit come after: see getDamageAfterArmorAbsorb.
                boolean hurt = super.hurt(source, (float) (amount * factor) / designPerHealth());
                if (hurt && critical && source.getEntity() instanceof ServerPlayer attacker) {
                    critEffects(attacker, chest(), factor);
                }
                return hurt;
            }
        }
    }

    /**
     * What a blow does to him once it has met his armour (spec 3.5.3): in a window (the shield broken, or the stagger of an
     * interrupted Escape) a quarter more, then never more than {@code maxHitFraction} of his health, so that no single
     * blow can take a phase from him however it is made (a critical one neither: {@link #hurt} multiplies before this).
     */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        float after = super.getDamageAfterArmorAbsorb(source, amount);
        if (shield.state() == ApocalypseShield.State.BROKEN || open) {
            after *= ApocalypseBossConfig.WINDOW_DAMAGE_FACTOR.get().floatValue();
        }
        return Math.min(after, designMax * ApocalypseBossConfig.MAX_HIT_FRACTION.get().floatValue() / designPerHealth());
    }

    /**
     * Mind power reaches the shield: it wears the shield and not his health, and may break it. {@code factor} is the
     * critical factor already in {@code mind} (1 for a hit that is not critical); a counted hit above 1 is seen and
     * heard.
     */
    private boolean absorb(ApocalypseFight fight, DamageSource source, float mind, double factor) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return false;
        }
        long now = level().getGameTime();
        float before = shield.strength();
        ApocalypseShield.Result result = shield.absorb(attacker.getUUID(), mind, now, fight.windowTicksOfThisShield());
        if (result == ApocalypseShield.Result.IGNORED) {
            return false;
        }
        Vec3 at = hitPoint(source);
        fight.onShieldWorn(attacker, Math.min(before, mind), result == ApocalypseShield.Result.BROKEN, at);
        critEffects(attacker, at, factor);
        return true;
    }

    /**
     * The shield is strained (not hit): a Phoenix's Mind Control or Psionic Field, once per cast ({@link MindPower#strainCounts}:
     * his mind refuses her, so the cast starts no recharge of hers, and the shield keeps it). A Phoenix of {@code
     * critHeroes} strains it {@code critShieldFactor} times as much, a critical hit.
     */
    public void strain(ServerPlayer phoenix, PhoenixPower.Kind kind) {
        ApocalypseFight fight = fight();
        float points = MindPower.strain(kind);
        if (fight == null || fight.bossInvulnerable() || !shield.isUp() || points <= 0.0F
                || !MindPower.strainCounts(strains, phoenix.getUUID(), kind, level().getGameTime())) {
            return;
        }
        double factor = MindPower.isCritHero(phoenix) ? ApocalypseShieldConfig.CRIT_SHIELD_FACTOR.get() : 1.0;
        points = (float) (points * factor);
        float before = shield.strength();
        ApocalypseShield.Result result = shield.strain(points, level().getGameTime(), fight.windowTicksOfThisShield());
        if (result != ApocalypseShield.Result.IGNORED) {
            Vec3 at = chest();
            fight.onShieldWorn(phoenix, Math.min(before, points), result == ApocalypseShield.Result.BROKEN, at);
            critEffects(phoenix, at, factor);
        }
    }

    /** His chest: where a strain rings on the shield, and where a critical hit on his body bursts. */
    private Vec3 chest() {
        return position().add(0, getBbHeight() * 0.7, 0);
    }

    /**
     * A critical hit (its factor above 1) is seen and heard, at most once per attacker every {@link #CRIT_EFFECT_TICKS}
     * ticks: 20 crit sparks and 10 enchanted sparks burst at {@code at} for everyone near, the heavy crack of {@link
     * CritSound}, and "Critical hit! x2" on the attacker's action bar.
     */
    private void critEffects(ServerPlayer attacker, Vec3 at, double factor) {
        if (factor <= 1.0 || !(level() instanceof ServerLevel server)) {
            return;
        }
        long now = server.getGameTime();
        Long last = lastCritEffects.get(attacker.getUUID());
        if (last != null && now - last >= 0 && now - last < CRIT_EFFECT_TICKS) {
            return;
        }
        if (lastCritEffects.size() > 8) {
            Iterator<Long> iterator = lastCritEffects.values().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next() >= CRIT_EFFECT_TICKS) {
                    iterator.remove();
                }
            }
        }
        lastCritEffects.put(attacker.getUUID(), now);
        server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 20, 0.35, 0.35, 0.35, 0.5);
        server.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 10, 0.35, 0.35, 0.35, 0.3);
        CritSound.play(server, at);
        attacker.displayClientMessage(Component.translatable(CRIT_MESSAGE, factorText(factor)), true);
    }

    /** A factor as the action bar shows it: "2" for 2.0, "2.5" for 2.5 (at most two decimals). */
    private static String factorText(double factor) {
        return BigDecimal.valueOf(factor).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** The point on the sphere where a blow struck, for the ripple. */
    private Vec3 hitPoint(DamageSource source) {
        Vec3 center = position().add(0, getBbHeight() * 0.5, 0);
        Entity from = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        Vec3 away = from != null ? from.position().add(0, from.getBbHeight() * 0.5, 0).subtract(center)
                : source.getSourcePosition() != null ? source.getSourcePosition().subtract(center) : new Vec3(0, 1, 0);
        return away.lengthSqr() < 1.0E-6 ? center.add(0, ApocalypseShield.RADIUS, 0) : center.add(away.normalize().scale(ApocalypseShield.RADIUS));
    }

    /** A blow the shield turns away: a golden ripple at the point of impact and a glassy tink (rate-limited). */
    private void deflect(DamageSource source) {
        ApocalypseFight fight = fight();
        if (fight != null) {
            fight.onDeflected();
        }
        deflectEffects(hitPoint(source), false);
    }

    /**
     * His shield turns away the telekinesis of {@code holder}, who is not mind power: the golden ripple on the sphere
     * toward them (rate-limited as every deflection), and what the fight makes of a deflection (his Babel line).
     */
    public void turnAway(ServerPlayer holder) {
        ApocalypseFight fight = fight();
        if (fight != null) {
            fight.onDeflected();
        }
        Vec3 center = position().add(0, getBbHeight() * 0.5, 0);
        Vec3 toward = holder.getEyePosition().subtract(center);
        deflectEffects(toward.lengthSqr() < 1.0E-6 ? center.add(0, ApocalypseShield.RADIUS, 0)
                : center.add(toward.normalize().scale(ApocalypseShield.RADIUS)), false);
    }

    private void deflectEffects(Vec3 at, boolean force) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        long now = server.getGameTime();
        if (!force && now - lastDeflectSound < 4) {
            return;
        }
        lastDeflectSound = now;
        server.playSound(null, at.x, at.y, at.z, ModSounds.APOCALYPSE_SHIELD_HIT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        ApocalypseFx.send(server, at, ApocalypseFxPayload.SHIELD_HIT, getId(), at, Vec3.ZERO, Math.round(shield.fraction() * 100.0F), shield.state().ordinal());
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        hurtSource = source;
        try {
            super.actuallyHurt(source, amount);
        } finally {
            hurtSource = null;
        }
        ApocalypseFight fight = fight();
        if (fight != null && source.getEntity() instanceof ServerPlayer player) {
            fight.noteDamage(player);
        }
    }

    @Override
    public void die(DamageSource source) {
        ApocalypseFight fight = fight();
        if (fight != null && !fight.isOver()) {
            fight.abortKilled();
        }
        super.die(source);
    }

    /** Every mob effect is refused (slowness, weakness, nausea, poison, wither, Instant Damage) but Glowing. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.is(MobEffects.GLOWING);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The heroes against him

    /**
     * Quicksilver Time slows him fully for {@code adaptTicks}, then he <em>adapts</em> (the film) and is immune to it for
     * {@code adaptedTicks}; the fight then answers with an Entomb on the speedster's feet.
     */
    @Override
    public int slowFactor(int zoneFactor) {
        if (level().isClientSide()) {
            return zoneFactor;
        }
        long now = level().getGameTime();
        if (now < adaptedUntil) {
            return 0;
        }
        if (slowedSince < 0 || now - lastSlowed > 5) {
            slowedSince = now; // a new slowing: the zone was away for a while
        }
        lastSlowed = now;
        if (now - slowedSince >= ApocalypseBossConfig.ADAPT_TICKS.get()) {
            adaptedUntil = now + ApocalypseBossConfig.ADAPTED_TICKS.get();
            slowedSince = -1;
            ApocalypseFight fight = fight();
            if (fight != null) {
                fight.onAdapted();
            }
            return 0;
        }
        return zoneFactor;
    }

    /** Whether he has adapted to Quicksilver Time and ignores it. */
    public boolean hasAdapted() {
        return level().getGameTime() < adaptedUntil;
    }

    /**
     * Magneto's Armor and Weapon Strip: in a window it cuts his armour to nothing for the rest of it and lengthens it
     * ("Erik and Jean dismantled it"), once a window. Whatever else, the strip takes nothing from him.
     */
    @Override
    public boolean onMagnetoStrip(ServerPlayer magneto) {
        if (!level().isClientSide() && shield.extendWindow(ApocalypseBossConfig.MAGNETO_STRIP_EXTEND_TICKS.get())) {
            setArmourCut(true);
            if (level() instanceof ServerLevel server) {
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
                SandControl.puff(server, position().add(0, getBbHeight() * 0.6, 0), 10, 0.4);
            }
        }
        return true;
    }

    /** Lightning is Storm's, and it stops his escape. */
    @Override
    public void onLightning(@Nullable ServerPlayer thor, float amount) {
        lightningStruck = true;
    }

    /** Whether lightning has struck him since this was last asked; asking forgets it. */
    public boolean takeLightning() {
        boolean struck = lightningStruck;
        lightningStruck = false;
        return struck;
    }

    /**
     * Telekinesis holds him as a boss (3.1, D-T 6.3.1), but never in a set piece (awakening, a transition, the armour's
     * fall, vanished, kneeling, death, his withdrawal) nor while his shield forms or re-forms; and while it is up, only mind
     * power gets through it ({@code boss_shield} for everyone else). A pure query.
     */
    @Nullable
    @Override
    public String gripRefusal(ServerPlayer holder, Telekinesis.Mode mode) {
        ApocalypseFight fight = fight();
        ApocalypseShield.State state = shield.state();
        if (fight == null || fight.bossInvulnerable() || state == ApocalypseShield.State.FORMING || state == ApocalypseShield.State.REFORMING) {
            return BossGrip.UNTOUCHABLE;
        }
        return state == ApocalypseShield.State.UP && !MindPower.isBreaker(holder) ? BossGrip.SHIELD : null;
    }

    /** A press his shield turned away ({@code boss_shield}) shows its golden ripple, once per press. */
    @Override
    public void onGripRefused(ServerPlayer holder, String key) {
        if (BossGrip.SHIELD.equals(key)) {
            turnAway(holder);
        }
    }

    /** His armour, while it stands: Magneto's metal. In his true form there is no metal on him. */
    @Override
    public boolean isMetal() {
        return !isTrueForm();
    }

    /** {@code [apocalypse_boss].holdTicks}, asked of his fight. */
    @Override
    public int holdTicks() {
        ApocalypseFight fight = fight();
        return fight == null ? 0 : fight.holdTicks();
    }

    /** A grip drops his brain's plan and stops his walk; his own stagger, his healing and an Escape's channel go on. */
    @Override
    public void onGripped(ServerPlayer holder) {
        brain.reset();
        getNavigation().stop();
    }

    /** He never goes through a portal: his fight is in this dimension. */
    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        return false;
    }

    /** ... nor even begins to go through one (no exit portal is looked for, nor built, in the other dimension). */
    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------
    // A boss that stays

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
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
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.APOCALYPSE_DEATH.get();
    }

    /** Whether this player is one who fights him (not a Creative or Spectator one): a target. */
    static boolean isTargetable(@Nullable Entity entity) {
        return entity instanceof ServerPlayer player && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }
}
