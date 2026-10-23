package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.HelaBossConfig;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Hela, the Goddess of Death, the boss (3.5): the body of the fight of {@link HelaFight}. Her mind is {@link HelaBrain},
 * which the fight runs; this class is what she is: her health (in {@code hela_boss.health} points scaled with the
 * fighters, see {@link #applyBody}), her state for the clients ({@link HelaState}, the tick it began, where she aims),
 * what hurts her and what does not, and the hooks the heroes reach her by.
 *
 * <p><b>Health.</b> The game's health attribute holds at most 1024, and a fight of four would need more: her
 * {@link #designMax} is the plan's number, and every blow is divided by {@link #perHealth} on its way in, so the bar, the
 * phases and the channel's six percent are all fractions of the one health she has.
 *
 * <p><b>What hurts her.</b> Not falls, fire, drowning, walls, freezing or wither; nothing in a state that is
 * {@link HelaState#untouchable}; nothing of her own fight. Broken out of her channel she is stunned and takes 25 percent
 * more. When a blow would kill her she does not die: she falls ({@link HelaState#DYING}) and the fight is won.
 */
public class HelaEntity extends Monster implements BossHooks {
    /** The most real health she is given (the attribute's ceiling is 1024). */
    public static final float REAL_CEILING = 1000.0F;
    /** How fast she glides. */
    public static final double SPEED = 0.30;
    /** Stunned, she takes this much more. */
    public static final float STUN_FACTOR = 1.25F;

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(HelaEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_STATE_TICK = SynchedEntityData.defineId(HelaEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> DATA_AIM = SynchedEntityData.defineId(HelaEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(HelaEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_COUNT = SynchedEntityData.defineId(HelaEntity.class, EntityDataSerializers.BYTE);

    @Nullable
    private HelaFight fight;
    private final HelaBrain brain = new HelaBrain(this);
    private float designMax = 600.0F;
    private float perHealth = 1.0F;
    private float channelDamage;
    @Nullable
    private ServerPlayer killer;

    public HelaEntity(EntityType<? extends HelaEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    /** Her attributes; the fight puts on the config's health and armour when it makes her ({@link #applyBody}). */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.ATTACK_DAMAGE, 9.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, HelaState.ARRIVING.id());
        builder.define(DATA_STATE_TICK, 0L);
        builder.define(DATA_AIM, new Vector3f());
        builder.define(DATA_PHASE, (byte) 1);
        builder.define(DATA_COUNT, (byte) 3);
    }

    @Override
    protected void registerGoals() {
        // (Her mind is the fight's brain: no vanilla goals.)
    }

    // ---------------------------------------------------------------------------------------------------------
    // The fight

    void attach(HelaFight fight) {
        this.fight = fight;
    }

    @Nullable
    public HelaFight fight() {
        if (fight != null && fight.isFinished()) {
            return null;
        }
        return fight;
    }

    public HelaBrain brain() {
        return brain;
    }

    /**
     * Her health for {@code fighters}: {@code health × (1 + healthPerExtraFighter × (fighters - 1))}, keeping the part of
     * it she has (a fighter who joins in the first seconds makes her the stronger, not the more hurt); her armour.
     */
    public void applyBody(int fighters) {
        float fraction = getMaxHealth() > 0.0F ? Math.min(1.0F, getHealth() / getMaxHealth()) : 1.0F;
        designMax = (float) (HelaBossConfig.HEALTH.get() * (1.0 + HelaBossConfig.HEALTH_PER_EXTRA_FIGHTER.get() * Math.max(0, fighters - 1)));
        perHealth = Math.max(1.0F, designMax / REAL_CEILING);
        AttributeInstance max = getAttribute(Attributes.MAX_HEALTH);
        if (max != null) {
            max.setBaseValue(designMax / perHealth);
        }
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(HelaBossConfig.ARMOR.get());
        }
        setHealth(getMaxHealth() * fraction);
    }

    /** Her health in the plan's points (what the health bar is a fraction of). */
    public float designMax() {
        return designMax;
    }

    public float designHealth() {
        return getHealth() * perHealth;
    }

    /** How many of the plan's points one point of her real health is (1 until a fight is too big for the attribute). */
    public float perHealth() {
        return perHealth;
    }

    /** The part of her health she has, 0 to 1. */
    public float fraction() {
        return getMaxHealth() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth()));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Her state, for the clients

    public HelaState state() {
        return HelaState.byId(entityData.get(DATA_STATE));
    }

    /** The game time her state began. */
    public long stateTick() {
        return entityData.get(DATA_STATE_TICK);
    }

    /** Ticks since her state began (with the partial tick): what every pose is drawn from. */
    public float stateTime(float partial) {
        return Math.max(0.0F, level().getGameTime() + partial - stateTick());
    }

    /** Server ticks since her state began. */
    public int stateTicks() {
        return (int) Math.max(0L, level().getGameTime() - stateTick());
    }

    public void setState(HelaState state) {
        entityData.set(DATA_STATE, state.id());
        entityData.set(DATA_STATE_TICK, level().getGameTime());
        if (state == HelaState.CHANNEL) {
            channelDamage = 0.0F;
        }
    }

    /** Where her attack goes (the target of a volley, the line of a wall, the circle of a rain). */
    public Vec3 aim() {
        Vector3f aim = entityData.get(DATA_AIM);
        return new Vec3(aim.x, aim.y, aim.z);
    }

    public void setAim(Vec3 aim) {
        entityData.set(DATA_AIM, new Vector3f((float) aim.x, (float) aim.y, (float) aim.z));
    }

    /** The phase of the fight (1-3), synced for the music and the bar. */
    public int phase() {
        return entityData.get(DATA_PHASE);
    }

    void setPhase(int phase) {
        entityData.set(DATA_PHASE, (byte) phase);
    }

    /** How many blades her volley throws now (the tell draws as many). */
    public int bladeCount() {
        return entityData.get(DATA_COUNT);
    }

    void setBladeCount(int count) {
        entityData.set(DATA_COUNT, (byte) count);
    }

    /** Whether the headdress is grown (it grows in the last half of her arrival). */
    public boolean crowned(float partial) {
        return state() != HelaState.ARRIVING || stateTime(partial) >= HelaFight.FORM_AT;
    }

    // ---------------------------------------------------------------------------------------------------------
    // What hurts her

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.isInvulnerableTo(source);
        }
        return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.WITHER) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // /kill ends the fight with no winner (the fight sees her gone); the void does nothing (she is put back).
            return source.is(DamageTypes.GENERIC_KILL) && super.hurt(source, amount);
        }
        HelaFight fight = fight();
        if (fight == null || fight.isOver() || state().untouchable || amount <= 0.0F) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && (BossFights.sameFight(this, attacker) || attacker instanceof ArmyMob mob && mob.ownerId() == null)) {
            return false; // (a blade of her own, or one of her dead)
        }
        if (attacker instanceof ServerPlayer player) {
            fight.noteAttacker(player);
        }
        return super.hurt(source, amount / perHealth);
    }

    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        float after = super.getDamageAfterArmorAbsorb(source, amount);
        return state() == HelaState.STUNNED ? after * STUN_FACTOR : after;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float before = getHealth();
        super.actuallyHurt(source, amount);
        if (state() == HelaState.CHANNEL) {
            channelDamage += Math.max(0.0F, before - getHealth());
        }
    }

    /** The damage the fighters dealt her since her channel began (real health). */
    public float channelDamage() {
        return channelDamage;
    }

    /**
     * A blow that would kill her makes her fall instead (the fight is won, she dissolves); {@code /kill} kills her (the
     * fight sees her gone and ends with no winner).
     */
    @Override
    public void die(DamageSource source) {
        HelaFight fight = fight();
        if (fight == null || fight.isOver() || source.is(DamageTypes.GENERIC_KILL)) {
            super.die(source);
            return;
        }
        setHealth(1.0F);
        Entity cause = source.getEntity();
        killer = cause instanceof ServerPlayer player ? player
                : cause instanceof Projectile shot && shot.getOwner() instanceof ServerPlayer player ? player : null;
        fight.bossFell(killer);
    }

    @Override
    public void heal(float amount) {
        if (!state().untouchable || state() == HelaState.RISE) {
            super.heal(amount);
        }
    }

    /** Only Glowing touches her (no slowness, no poison, no weakness). */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.is(MobEffects.GLOWING);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The heroes against her

    /** Telekinesis holds her as a boss, never while she arrives, rises, falls or leaves. */
    @Nullable
    @Override
    public String gripRefusal(ServerPlayer holder, Telekinesis.Mode mode) {
        HelaFight fight = fight();
        return fight == null || fight.isOver() || state().untouchable ? BossGrip.UNTOUCHABLE : null;
    }

    /** {@code [hela_boss]}: two seconds at most. */
    @Override
    public int holdTicks() {
        return 40;
    }

    @Override
    public void onGripped(ServerPlayer holder) {
        brain.interrupt();
        getNavigation().stop();
    }

    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
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
        return ModSounds.NECROSWORD_HIT.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HELA_DEFEAT.get();
    }

    /** The one she is after now (the brain's choice), or null. */
    @Nullable
    public LivingEntity foe() {
        return getTarget();
    }
}
