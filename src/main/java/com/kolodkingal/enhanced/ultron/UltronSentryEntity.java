package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An Ultron Sentry: a flying robot, his army, and the hero's drone. It hovers (no gravity, no pathfinding: the brain
 * steers its velocity, see {@link UltronMotion}), hunts the nearest fighter, and does two things: it fires a plasma bolt
 * from its right arm after a tell of ten ticks in which the arm rises, the palm glows and its eyes flash white (the
 * aim is fixed when the tell begins, so a bolt can be dodged), and it punches what comes within two blocks after a tell
 * of five. When three or more are about the same target each takes a different angle, 120 degrees apart, and drifts
 * round it.
 *
 * <p>Its eyes are blue while it assembles (twenty invulnerable ticks), is stunned (lightning stuns it) or has no target,
 * red while it hunts, white in a tell: the colour of the hive mind. It drops nothing and gives no experience (no
 * farming), is immune to fire, drowning, Poison and Wither, and is magnetic (Stark's drone metal). It belongs to the
 * boss's fight (marked, no owner) or is the hero's drone (it has an <em>owner</em>, and then follows the hero's
 * orders: see {@link #tickDrone}); the first is swept up after a crash, the second is not.
 */
public class UltronSentryEntity extends PathfinderMob implements OwnableEntity, BossHooks {
    /** Ticks a new Sentry spends assembling, invulnerable and unable to act. */
    public static final int ASSEMBLE_TICKS = 20;
    /** The tell of a plasma shot, and of a punch. */
    public static final int SHOT_TELL = 10;
    public static final int PUNCH_TELL = 5;
    /** A Sentry hits what is this near with its fist. */
    public static final double PUNCH_RANGE = 2.2;
    /** It shoots from this near to this far. */
    public static final double SHOT_MIN = 4.0;
    public static final double SHOT_MAX = 22.0;
    /** The speed of its plasma bolt, in blocks a tick. */
    public static final double BOLT_SPEED = 1.4;

    /** What the body is doing, for the model: the arm raised for the shot tell, the fist drawn back. */
    public enum Act {
        IDLE, SHOT, PUNCH, STUNNED, ASSEMBLING;

        static Act byId(int id) {
            Act[] all = values();
            return all[Math.max(0, Math.min(all.length - 1, id))];
        }
    }

    /** What colour the eyes show: blue (idle, stunned, assembling), red (hunting), white (a tell). */
    public enum Eyes {
        BLUE, RED, WHITE;

        static Eyes byId(int id) {
            Eyes[] all = values();
            return all[Math.max(0, Math.min(all.length - 1, id))];
        }
    }

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(UltronSentryEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> DATA_ACT = SynchedEntityData.defineId(UltronSentryEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_EYES = SynchedEntityData.defineId(UltronSentryEntity.class, EntityDataSerializers.BYTE);
    /** The game time the current act began, so that every client animates a tell the same way. */
    private static final EntityDataAccessor<Long> DATA_ACT_TICK = SynchedEntityData.defineId(UltronSentryEntity.class, EntityDataSerializers.LONG);

    private int assembling;
    private int stunned;
    private int frozen;
    private boolean held;
    private int shotCooldown = 20;
    private int punchCooldown;
    private int shotTell;
    private int punchTell;
    /** Where the bolt will go, fixed when the shot tell begins. */
    private Vec3 aim = Vec3.ZERO;
    private Vec3 punchAim = Vec3.ZERO;
    private double swarmAngle;
    private int retarget;
    private int wander;
    private Vec3 wanderTo = Vec3.ZERO;
    /** The drone's life, in ticks left (the hero's drones only; the boss's Sentries have none). */
    private int lifetime = -1;

    public UltronSentryEntity(EntityType<? extends UltronSentryEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FLYING_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    /**
     * A Sentry of the boss's army at {@code at}, assembling for twenty ticks, with the health the config gives (scaled
     * by {@code [boss].difficultyScale}). Not added to the level: the fight adds it ({@code fight.spawn}).
     */
    public static UltronSentryEntity army(ServerLevel level, Vec3 at, boolean assembling) {
        UltronSentryEntity sentry = new UltronSentryEntity(ModEntities.ULTRON_SENTRY.get(), level);
        sentry.setPos(at);
        double health = UltronBossConfig.SENTRY_HEALTH.get() * BossConfig.DIFFICULTY_SCALE.get();
        sentry.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        sentry.setHealth((float) health);
        if (assembling) {
            sentry.beginAssembling();
        } else {
            sentry.entityData.set(DATA_EYES, (byte) Eyes.RED.ordinal());
        }
        return sentry;
    }

    // ---- who it is ----

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    /** Makes it a hero's drone (or, with null, the boss's Sentry again). Set before the Sentry is added to a level. */
    public void setOwner(@Nullable UUID owner) {
        entityData.set(DATA_OWNER, Optional.ofNullable(owner));
    }

    /** Whether it is a hero's drone. */
    public boolean isDrone() {
        return getOwnerUUID() != null;
    }

    /** The ticks the drone has left to live, or -1 if it has no limit. */
    public int lifetime() {
        return lifetime;
    }

    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_ACT, (byte) Act.IDLE.ordinal());
        builder.define(DATA_EYES, (byte) Eyes.BLUE.ordinal());
        builder.define(DATA_ACT_TICK, 0L);
    }

    public Act act() {
        return Act.byId(entityData.get(DATA_ACT));
    }

    public Eyes eyes() {
        return Eyes.byId(entityData.get(DATA_EYES));
    }

    public long actTick() {
        return entityData.get(DATA_ACT_TICK);
    }

    /** Whether it is still assembling (invulnerable, cannot act, no vessel for Ultron). */
    public boolean isAssembling() {
        return assembling > 0;
    }

    public boolean isStunned() {
        return stunned > 0;
    }

    private void setAct(Act act) {
        if (act() != act) {
            entityData.set(DATA_ACT, (byte) act.ordinal());
            entityData.set(DATA_ACT_TICK, level().getGameTime());
        }
    }

    /** Shows this colour in the eyes (the hunt sets it every tick; the ritual lights them red at tick 100). */
    public void setEyes(Eyes eyes) {
        if (eyes() != eyes) {
            entityData.set(DATA_EYES, (byte) eyes.ordinal());
        }
    }

    /** Starts the twenty ticks of assembling. */
    public void beginAssembling() {
        assembling = ASSEMBLE_TICKS;
        entityData.set(DATA_ACT, (byte) Act.ASSEMBLING.ordinal());
        entityData.set(DATA_EYES, (byte) Eyes.BLUE.ordinal());
    }

    /** Ultron changes bodies: every Sentry freezes for a while, eyes red. */
    public void freeze(int ticks) {
        frozen = Math.max(frozen, ticks);
    }

    /** Pauses the Sentry completely (the lift carries it) or lets it act again. */
    public void setHeld(boolean held) {
        this.held = held;
    }

    /** Stuns it: no action, eyes blue, a flicker. */
    public void stun(int ticks) {
        stunned = Math.max(stunned, ticks);
    }

    // ---- what it takes ----

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.isInvulnerableTo(source);
        }
        return assembling > 0 || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.WITHER)
                || super.isInvulnerableTo(source);
    }

    /** Poison and Wither: a machine takes neither. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && !effect.is(MobEffects.WITHER) && super.canBeAffected(effect);
    }

    /** Lightning stuns a Sentry (Thor). */
    @Override
    public void onLightning(@Nullable ServerPlayer thor, float amount) {
        stun(UltronBossConfig.SENTRY_STUN_TICKS.get());
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
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
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
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            sparks(server, 24);
            UltronSounds.SENTRY_DEATH.play(server, position());
        }
    }

    /** A Sentry that is taken apart without a blow (Ultron uploads into it, the fight ends): sparks and gone. */
    public void dissolve() {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            sparks(server, 24);
            UltronSounds.SENTRY_DEATH.play(server, position(), 0.7F, 1.2F);
        }
        discard();
    }

    private void sparks(ServerLevel server, int count) {
        Vec3 at = position().add(0, getBbHeight() * 0.5, 0);
        server.sendParticles(new EnergyParticleOptions(isDrone() ? 0xFF3B30 : 0x45B4F5, 1.2F), at.x, at.y, at.z, count, 0.3, 0.5, 0.3, 0.15);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, count / 2, 0.3, 0.5, 0.3, 0.3);
    }

    // ---- life ----

    /** The fight this Sentry belongs to, or null. */
    @Nullable
    public UltronFight fight() {
        return BossFights.fightOf(this) instanceof UltronFight fight ? fight : null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        if (lifetime >= 0 && --lifetime < 0) {
            dissolve();
            return;
        }
        if (assembling > 0) {
            if (--assembling == 0) {
                setAct(Act.IDLE);
            }
            UltronMotion.steer(this, Vec3.ZERO, 0.2);
            return;
        }
        if (held || Telekinesis.isHeld(this) || MjolnirPin.isPinned(this) || hasEffect(ModEffects.SPELLBOUND)) {
            setTarget(null);
            return;
        }
        if (frozen > 0) {
            frozen--;
            setEyes(Eyes.RED);
            UltronMotion.steer(this, Vec3.ZERO, 0.2);
            return;
        }
        if (stunned > 0) {
            if (--stunned == 0) {
                setAct(Act.IDLE);
            } else {
                setAct(Act.STUNNED);
                setEyes(Eyes.BLUE);
                UltronMotion.steer(this, Vec3.ZERO, 0.1);
                return;
            }
        }
        if (isDrone()) {
            tickDrone();
        } else {
            tickHostile();
        }
    }

    /**
     * The hero's drone acts: follows him, attacks what hurt him and what he last hit, and hostile creatures near him.
     * Built by the hero stage; until then a drone only hovers.
     */
    protected void tickDrone() {
        setEyes(Eyes.RED);
        UltronMotion.steer(this, Vec3.ZERO, 0.1);
    }

    /** The boss's Sentry: hunt the nearest fighter, shoot it from a distance and punch it close. */
    private void tickHostile() {
        UltronFight fight = fight();
        if (fight == null) {
            discard(); // its fight is gone: nothing keeps a Sentry of a fight that does not exist
            return;
        }
        if (confused()) {
            setEyes(Eyes.BLUE);
            wander(fight);
            return;
        }
        if (retarget-- <= 0 || getTarget() == null || !getTarget().isAlive()) {
            retarget = 20;
            setTarget(nearestTarget(fight));
        }
        LivingEntity target = getTarget();
        if (shotTell > 0 || punchTell > 0) {
            tell(target);
            return;
        }
        if (target == null) {
            setEyes(Eyes.BLUE);
            setAct(Act.IDLE);
            orbit(fight);
            return;
        }
        setEyes(Eyes.RED);
        setAct(Act.IDLE);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = distanceTo(target);
        approach(fight, target, distance);
        if (shotCooldown > 0) {
            shotCooldown--;
        }
        if (punchCooldown > 0) {
            punchCooldown--;
        }
        if (distance <= PUNCH_RANGE && punchCooldown <= 0) {
            punchTell = PUNCH_TELL;
            punchAim = target.position().add(0, target.getBbHeight() * 0.5, 0);
            beginTell(Act.PUNCH);
        } else if (distance >= SHOT_MIN && distance <= SHOT_MAX && shotCooldown <= 0 && hasLineOfSight(target)) {
            shotTell = SHOT_TELL;
            aim = leadPoint(target, distance);
            beginTell(Act.SHOT);
        }
    }

    private boolean confused() {
        return hasEffect(ModEffects.CONFUSED);
    }

    /** A confused Sentry stumbles about, going for nobody. */
    private void wander(UltronFight fight) {
        setTarget(null);
        if (wander-- <= 0) {
            wander = 10;
            wanderTo = position().add(random.nextGaussian() * 3.0, random.nextGaussian(), random.nextGaussian() * 3.0);
        }
        Vec3 toward = wanderTo.subtract(position());
        double speed = speed() * 0.6;
        UltronMotion.steer(this, toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize().scale(speed), 0.08);
    }

    private double speed() {
        return Math.max(0.0, getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    private void beginTell(Act act) {
        setAct(act);
        setEyes(Eyes.WHITE);
        UltronSounds.SERVO.play((ServerLevel) level(), position(), 0.6F, 1.0F);
    }

    /** The shot and the punch wind up: the aim is fixed, the Sentry holds still, and then it lands. */
    private void tell(@Nullable LivingEntity target) {
        UltronMotion.steer(this, Vec3.ZERO, 0.15);
        if (target != null) {
            getLookControl().setLookAt(target, 40.0F, 40.0F);
        }
        if (shotTell > 0 && --shotTell == 0) {
            fire();
            shotCooldown = 40 + random.nextInt(21);
            setAct(Act.IDLE);
        }
        if (punchTell > 0 && --punchTell == 0) {
            punch();
            punchCooldown = 20;
            setAct(Act.IDLE);
        }
    }

    private void fire() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
        Vec3 direction = aim.subtract(from);
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }
        UltronBoltEntity bolt = UltronBoltEntity.create(server, this, from, direction.normalize().scale(BOLT_SPEED),
                (float) (UltronBossConfig.SENTRY_BOLT_DAMAGE.get() * BossConfig.DIFFICULTY_SCALE.get()), UltronBoltEntity.Palette.SENTRY);
        BossFight fight = BossFights.fightOf(this);
        if (fight != null) {
            fight.spawn(bolt);
        } else {
            server.addFreshEntity(bolt);
        }
        UltronSounds.BOLT.play(server, from);
    }

    private void punch() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        UltronSounds.PUNCH.play(server, position(), 0.7F, 1.2F);
        AABB reach = new AABB(punchAim, punchAim).inflate(PUNCH_RANGE * 0.6, PUNCH_RANGE * 0.6, PUNCH_RANGE * 0.6);
        float damage = (float) (UltronBossConfig.SENTRY_MELEE_DAMAGE.get() * BossConfig.DIFFICULTY_SCALE.get());
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, reach, e -> mayHit(e) && distanceTo(e) <= PUNCH_RANGE + 0.6)) {
            if (victim.hurt(damage(server, ModDamageTypes.ULTRON_SLAM), damage)) {
                Vec3 out = victim.position().subtract(position());
                victim.knockback(0.5, -out.x, -out.z);
                victim.hurtMarked = true;
            }
        }
    }

    private DamageSource damage(ServerLevel server, ResourceKey<DamageType> type) {
        return ModDamageTypes.source(server, type, this, this);
    }

    /** Whether this Sentry's blow may land on the creature: the fight's enemies, never its own side. */
    private boolean mayHit(LivingEntity victim) {
        return victim != this && !(victim instanceof Player p && (p.isCreative() || p.isSpectator()))
                && !BossFights.sameFight(this, victim) && !(victim instanceof UltronEntity);
    }

    // ---- hunting ----

    /** The fighter (or the decoy of one) nearest to this Sentry that a Sentry hunts: never Creative or Spectator. */
    @Nullable
    private LivingEntity nearestTarget(UltronFight fight) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : fight.targets()) {
            double distance = distanceToSqr(candidate);
            if (distance < bestDistance && distance <= 64.0 * 64.0) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Where to aim: the target's chest, led a little by how it is moving (a third of the flight time). */
    private Vec3 leadPoint(LivingEntity target, double distance) {
        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.6, 0);
        return chest.add(target.getKnownMovement().scale(distance / BOLT_SPEED * 0.35));
    }

    /** Hovering about the target: a ring of its own angle if others are near it, and the right distance from it. */
    private void approach(UltronFight fight, LivingEntity target, double distance) {
        List<UltronSentryEntity> crowd = level().getEntitiesOfClass(UltronSentryEntity.class, target.getBoundingBox().inflate(8.0),
                s -> s.getOwnerUUID() == null && s.getTarget() == target && !s.isRemoved());
        crowd.sort(Comparator.comparingInt(Entity::getId));
        int index = Math.max(0, crowd.indexOf(this));
        swarmAngle += 0.012;
        double angle = crowd.size() >= 3 ? swarmAngle + index * (Mth.TWO_PI / 3.0) : swarmAngle * 0.5 + getId();
        double wantDistance = crowd.size() >= 3 ? 9.0 : 8.0;
        Vec3 spot = target.position().add(Math.cos(angle) * wantDistance, 0, Math.sin(angle) * wantDistance);
        double groundY = UltronMotion.groundBelow(level(), getX(), getY() + 0.1, getZ());
        double y = Math.max(groundY + 1.5, Math.min(groundY + 3.5, target.getY() + 2.0));
        if (distance <= PUNCH_RANGE + 1.0 && punchCooldown <= 0) {
            spot = target.position(); // close enough to be punching: stay on it
        }
        Vec3 toward = new Vec3(spot.x - getX(), y - getY(), spot.z - getZ());
        Vec3 wanted = toward.lengthSqr() < 0.04 ? Vec3.ZERO : toward.normalize().scale(Math.min(speed(), toward.length() * 0.2 + 0.05));
        Vec3 centre = fight.arena().center();
        double limit = fight.arena().containRadius() - 1.0;
        UltronMotion.steer(this, UltronMotion.keepInDisc(position(), wanted, centre, limit), 0.06);
    }

    /** With nobody to hunt, drifting over the middle of the arena at a hovering height. */
    private void orbit(UltronFight fight) {
        Vec3 centre = fight.arena().center();
        double groundY = UltronMotion.groundBelow(level(), getX(), getY() + 0.1, getZ());
        Vec3 toward = new Vec3(centre.x - getX(), groundY + 2.5 - getY(), centre.z - getZ());
        UltronMotion.steer(this, toward.length() < 4.0 ? new Vec3(0, toward.y * 0.1, 0) : toward.normalize().scale(speed() * 0.5), 0.05);
    }

    // ---- save ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setOwner(tag.hasUUID("Owner") ? tag.getUUID("Owner") : null);
        lifetime = tag.contains("Lifetime") ? tag.getInt("Lifetime") : -1;
    }
}
