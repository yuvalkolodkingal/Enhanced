package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A cocoon of Apocalypse's sand around a creature he entombed (the boss's Entomb, the hero's Sneak + R). It follows the
 * creature, renews the {@code Entombed} effect on it every tick, holds it where it stands (a player moves themselves, so
 * the server puts them back; the creatures that think are stopped), takes {@code damagePerSecond} from it every second
 * <em>but never below two hearts</em>, and breaks when its own health is gone (its victim's friends free it by hitting
 * it: three sword hits), when its time is up, when the victim dies or leaves, or when something frees it ({@link
 * #freeFrom}: Tommy's Vibrate, Wanda's telekinesis taking hold of a friend).
 *
 * <p>Like an End Crystal it is pickable and hit with a {@code hurt} of its own. It is never saved and cannot be summoned.
 */
public class SandCocoonEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_VICTIM = SynchedEntityData.defineId(SandCocoonEntity.class, EntityDataSerializers.INT);
    /** The part of its health it has left, 0 to 1: the cracks the client draws. */
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(SandCocoonEntity.class, EntityDataSerializers.FLOAT);
    /** 3.5: the colour a custom hero's pick of Entomb chose for the sand (synced), or -1 for the sand's own. */
    private static final EntityDataAccessor<Integer> DATA_TINT = SynchedEntityData.defineId(SandCocoonEntity.class,
            EntityDataSerializers.INT);

    @Nullable
    private UUID victimId;
    @Nullable
    private UUID casterId;
    private boolean casterIsPlayer;
    private int ticksLeft;
    private float damagePerSecond;
    private float health = 20.0F;
    private float maxHealth = 20.0F;
    private Vec3 anchor = Vec3.ZERO;

    public SandCocoonEntity(EntityType<? extends SandCocoonEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /**
     * @param caster the one who cast it (the boss, or the hero), or null
     * @param ticks  how long it holds
     */
    public SandCocoonEntity(ServerLevel level, LivingEntity victim, @Nullable LivingEntity caster, int ticks, float damagePerSecond, float health) {
        this(ModEntities.SAND_COCOON.get(), level);
        this.victimId = victim.getUUID();
        this.casterId = caster == null ? null : caster.getUUID();
        this.casterIsPlayer = caster instanceof Player;
        this.ticksLeft = ticks;
        this.damagePerSecond = damagePerSecond;
        this.health = health;
        this.maxHealth = health;
        this.anchor = victim.position();
        entityData.set(DATA_VICTIM, victim.getId());
        entityData.set(DATA_TINT, AbilityColors.chosen(caster, HeroAbility.APOCALYPSE_ENTOMB));
        moveTo(victim.getX(), victim.getY(), victim.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VICTIM, -1);
        builder.define(DATA_HEALTH, 1.0F);
        builder.define(DATA_TINT, -1);
    }

    /** 3.5: the colour chosen for the cocoon's sand, or -1. */
    public int tint() {
        return entityData.get(DATA_TINT);
    }

    // ---- what it is ----

    /** The creature it holds, or null. */
    @Nullable
    public LivingEntity victim() {
        if (level() instanceof ServerLevel server) {
            return victimId != null && server.getEntity(victimId) instanceof LivingEntity living ? living : null;
        }
        return level().getEntity(entityData.get(DATA_VICTIM)) instanceof LivingEntity living ? living : null;
    }

    @Nullable
    public UUID victimId() {
        return victimId;
    }

    /** The part of its health it has left. */
    public float healthFraction() {
        return entityData.get(DATA_HEALTH);
    }

    public float health() {
        return health;
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    /** The one who cast it (the boss, or the hero), or null. */
    @Nullable
    public UUID casterId() {
        return casterId;
    }

    /** Whether a hero cast it (its own caster is a player): the orphan sweep leaves a hero's cocoon alone. */
    public boolean isHeroes() {
        return casterIsPlayer;
    }

    // ---- life ----

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            if (level().getEntity(entityData.get(DATA_VICTIM)) instanceof LivingEntity victim) {
                setPos(victim.getX(), victim.getY(), victim.getZ());
            }
            return;
        }
        LivingEntity victim = victim();
        if (victim == null || !victim.isAlive() || victim.isSpectator() || victim.level() != level()
                || victim instanceof Player player && player.isCreative() || --ticksLeft <= 0) {
            release();
            return;
        }
        setPos(victim.getX(), victim.getY(), victim.getZ());
        victim.addEffect(new MobEffectInstance(ModEffects.ENTOMBED, SandControl.EFFECT_TICKS, 0, false, false, true));
        hold(victim);
        if (tickCount % 20 == 0 && damagePerSecond > 0.0F && victim.getHealth() > SandControl.ENTOMB_FLOOR) {
            float amount = Math.min(damagePerSecond, victim.getHealth() - SandControl.ENTOMB_FLOOR);
            LivingEntity caster = casterId != null && server.getEntity(casterId) instanceof LivingEntity living ? living : null;
            victim.invulnerableTime = 0;
            victim.hurt(ModDamageTypes.source(server, ModDamageTypes.ENTOMB, caster, caster), amount);
        }
        if (tickCount % 10 == 0) {
            SandControl.puff(server, position().add(0, 1.0, 0), 2, 0.4);
        }
    }

    /** Holds the creature where the sand closed on it. */
    private void hold(LivingEntity victim) {
        if (victim instanceof ServerPlayer player) {
            // A player moves themselves on their own computer: the server puts them back, and takes the ability they hold.
            anchor = new Vec3(anchor.x, Math.min(anchor.y, player.getY()), anchor.z);
            double drift = Math.hypot(player.getX() - anchor.x, player.getZ() - anchor.z);
            if (drift > 0.35 || player.getY() > anchor.y + 0.1) {
                player.connection.teleport(anchor.x, Math.min(player.getY(), anchor.y), anchor.z, player.getYRot(), player.getXRot());
            }
            Vec3 motion = player.getDeltaMovement();
            if (motion.x != 0.0 || motion.z != 0.0 || motion.y > 0.0) {
                player.setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
                player.hurtMarked = true;
            }
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            player.connection.aboveGroundTickCount = 0;
            PowerTicker.stopHoldAbilities(Powers.get(player));
        } else {
            Vec3 motion = victim.getDeltaMovement();
            victim.setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
            if (victim instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
            }
        }
    }

    /** The sand lets go: the effect ends, with a dry crack and a rush of grains, and the cocoon is gone. */
    public void release() {
        if (isRemoved()) {
            return;
        }
        if (level() instanceof ServerLevel server) {
            LivingEntity victim = victim();
            if (victim != null) {
                victim.removeEffect(ModEffects.ENTOMBED);
            }
            SandControl.puff(server, position().add(0, 1.0, 0), 24, 0.5);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.ENTOMB_BREAK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        discard();
    }

    /**
     * Lets whoever is in a cocoon out: Tommy's Vibrate frees him, and Wanda's telekinesis taking hold of a friend breaks
     * the cocoon that held them.
     *
     * @return whether there was a cocoon
     */
    public static boolean freeFrom(Entity victim) {
        SandCocoonEntity cocoon = cocoonOf(victim);
        if (cocoon == null) {
            return false;
        }
        cocoon.release();
        return true;
    }

    /** The cocoon that holds this creature (server side), or null. */
    @Nullable
    public static SandCocoonEntity cocoonOf(Entity victim) {
        if (victim.level().isClientSide) {
            return null;
        }
        for (SandCocoonEntity cocoon : victim.level().getEntitiesOfClass(SandCocoonEntity.class, victim.getBoundingBox().inflate(2.0),
                c -> victim.getUUID().equals(c.victimId) && !c.isRemoved())) {
            return cocoon;
        }
        return null;
    }

    // ---- being hit ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            release();
            return true;
        }
        if (amount <= 0.0F) {
            return false;
        }
        health -= amount;
        entityData.set(DATA_HEALTH, Math.max(0.0F, health / maxHealth));
        if (health <= 0.0F) {
            release();
        } else {
            level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 0.8F, 1.2F);
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
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
    public boolean isAttackable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
