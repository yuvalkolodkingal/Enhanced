package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A bolt of Ultron's plasma: the Repulsor Volley's bolts, every Sentry's and drone's shot, and (heavier, with a splash)
 * the hero's Overcharge. It flies straight, never drops and never slows (a {@link ThrowableProjectile} that cancels its
 * own drag), hurts the first creature it touches and spends itself on the first block, which it never breaks, burns or
 * dents: it makes sparks and nothing else. Quicksilver Time hangs it in the air and Billy can catch it, like an arrow.
 *
 * <p>It is a {@link ShieldCost}: Wanda's Chaos Shield stops it and pays its damage. A bolt the boss fires belongs to the
 * boss's fight ({@code fight.spawn}), spares everything on the boss's side and every Creative and Spectator player, and
 * obeys the usual hit cooldown; a bolt a hero fires (its owner is a player) spares the hero's drones, hurts players
 * only when PvP allows and the hero may harm them, and ignores the hit cooldown, so that a stream of them all land. A
 * drone's bolt is the hero's in everything but the cooldown.
 */
public class UltronBoltEntity extends ThrowableProjectile implements ShieldCost {
    /** Which colours the bolt is drawn in: the boss's amber, a Sentry's blue, the human Ultron's red. */
    public enum Palette {
        PRIME, SENTRY, HUMAN;

        static Palette byId(int id) {
            Palette[] all = values();
            return all[Math.max(0, Math.min(all.length - 1, id))];
        }
    }

    /** How far a bolt flies before it fades, in ticks of its own time. */
    public static final int DEFAULT_LIFE = 30;
    private static final EntityDataAccessor<Byte> DATA_PALETTE = SynchedEntityData.defineId(UltronBoltEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_HEAVY = SynchedEntityData.defineId(UltronBoltEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 4.0F;
    private float splashRadius;
    private float splashDamage;
    private float knockback;
    private int maxLife = DEFAULT_LIFE;
    /** Ticks lived, in the bolt's own (possibly slowed) time. */
    private double life;

    public UltronBoltEntity(EntityType<? extends UltronBoltEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /**
     * A bolt from {@code from} along {@code velocity} (blocks per tick), owned by {@code owner} (null for none). It is
     * not added to the level: the caller adds it (a boss's bolt through its fight).
     */
    public static UltronBoltEntity create(Level level, @Nullable Entity owner, Vec3 from, Vec3 velocity, float damage, Palette palette) {
        UltronBoltEntity bolt = new UltronBoltEntity(ModEntities.ULTRON_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.setPos(from);
        bolt.damage = damage;
        bolt.entityData.set(DATA_PALETTE, (byte) palette.ordinal());
        bolt.setDeltaMovement(velocity);
        bolt.updateRotation();
        return bolt;
    }

    /** Makes it the heavy blast of the Overcharge: wider, with a splash around what it hits and a knockback. */
    public UltronBoltEntity heavy(float splashRadius, float splashDamage, float knockback) {
        this.entityData.set(DATA_HEAVY, true);
        this.splashRadius = splashRadius;
        this.splashDamage = splashDamage;
        this.knockback = knockback;
        return this;
    }

    /** Gives a light bolt a splash: half its damage to everything within {@code radius} of what it hits (the hero's). */
    public UltronBoltEntity splash(float radius) {
        this.splashRadius = radius;
        this.splashDamage = damage * 0.5F;
        return this;
    }

    /** How many ticks (of its own time) the bolt flies before it fades. */
    public UltronBoltEntity lasts(int ticks) {
        this.maxLife = ticks;
        return this;
    }

    public float damage() {
        return damage;
    }

    public Palette palette() {
        return Palette.byId(entityData.get(DATA_PALETTE));
    }

    /** Whether it is the heavy blast of the Overcharge. */
    public boolean isHeavy() {
        return entityData.get(DATA_HEAVY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PALETTE, (byte) 0);
        builder.define(DATA_HEAVY, false);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    // ---- Wanda's shield ----

    /** What the bolt costs the Chaos Shield that stops it: its damage. */
    @Override
    public float shieldCost() {
        return damage;
    }

    // ---- flight ----

    @Override
    public void tick() {
        if (!level().isClientSide && !isRemoved() && overlapping()) {
            return;
        }
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (!isInWater()) {
            // A thrown thing loses a hundredth of its speed every tick; plasma does not.
            setDeltaMovement(getDeltaMovement().scale(1.0 / 0.99));
        }
        // Counted in the bolt's own time, so that Quicksilver Time doesn't cut its flight short.
        if ((life += SlowMotion.timeScale(this)) > maxLife && level() instanceof ServerLevel server) {
            spark(server, position());
            discard();
        }
    }

    /**
     * Whether the bolt starts this tick already touching a creature it can hit, and hits it. The game looks for what a
     * projectile hits along its way and, at 2.6 blocks a tick, a creature that moves into the bolt's last position (a
     * knocked-back zombie, one walking at it) is inside the margin round it at the start of the segment, so that the
     * line enters nothing and the bolt flies through. Plasma does not.
     */
    private boolean overlapping() {
        AABB probe = getBoundingBox().inflate(0.3);
        for (Entity target : level().getEntities(this, probe, this::canHitEntity)) {
            EntityHitResult hit = new EntityHitResult(target, position());
            if (!EventHooks.onProjectileImpact(this, hit)) {
                hitTargetOrDeflectSelf(hit);
            }
            if (isRemoved()) {
                return true;
            }
        }
        return false;
    }

    /** The one the bolt is fought for: the player behind a drone's bolt, otherwise whoever fired it. */
    @Nullable
    private Entity master() {
        Entity owner = getOwner();
        if (owner instanceof OwnableEntity pet && pet.getOwner() != null) {
            return pet.getOwner();
        }
        return owner;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity master = master();
        if (!super.canHitEntity(target) || target == getOwner() || target == master) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (target instanceof UltronBoltEntity || BossFights.sameFight(this, target)) {
            return false;
        }
        if (master instanceof Player shooter) {
            // A hero's bolt, or his drone's: not his drones, not players unless PvP allows it.
            if (target instanceof OwnableEntity pet && shooter.getUUID().equals(pet.getOwnerUUID())) {
                return false;
            }
            if (target instanceof Player victim) {
                MinecraftServer server = level().getServer();
                return server != null && server.isPvpAllowed() && shooter.canHarmPlayer(victim);
            }
            return true;
        }
        // The boss's bolts spare what is on its side, and break nothing of anyone's: a painting, an item frame, a boat, a
        // minecart or an armour stand in the arena is not an enemy (a projectile that hit one would take it off its wall).
        return !(target instanceof UltronEntity) && target instanceof LivingEntity && !(target instanceof ArmorStand);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        Entity target = result.getEntity();
        Entity master = master();
        DamageSource source = ModDamageTypes.source(server, ModDamageTypes.ULTRON_BOLT, master, this);
        if (getOwner() instanceof Player) {
            // A hero's stream: every bolt lands, whatever the last one did.
            target.invulnerableTime = 0;
        }
        boolean hurt = target.hurt(source, damage);
        if (hurt && knockback > 0.0F && target instanceof LivingEntity living) {
            Vec3 push = getDeltaMovement().normalize();
            living.knockback(knockback * 0.5, -push.x, -push.z);
            living.hurtMarked = true;
        }
        if (splashRadius > 0.0F && master instanceof LivingEntity shooter) {
            burst(server, shooter, target, source);
        }
        spark(server, result.getLocation());
        discard();
    }

    /** Half the damage (or the splash the bolt was given) to every other creature the shooter may hurt near the hit. */
    private void burst(ServerLevel server, LivingEntity shooter, Entity hit, DamageSource source) {
        AABB box = hit.getBoundingBox().inflate(splashRadius);
        List<LivingEntity> near = server.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != hit && e != shooter && e.distanceToSqr(hit) <= (double) splashRadius * splashRadius && AreaTargets.mayHurt(shooter, e, false));
        for (LivingEntity victim : near) {
            if (getOwner() instanceof Player) {
                victim.invulnerableTime = 0;
            }
            victim.hurt(source, splashDamage);
            if (knockback > 0.0F) {
                Vec3 out = victim.position().subtract(hit.position());
                victim.knockback(knockback * 0.5, -out.x, -out.z);
                victim.hurtMarked = true;
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server) {
            // It makes a spark and a small crack where it ends; nothing breaks, burns or catches fire.
            spark(server, result.getLocation());
            discard();
        }
    }

    private void spark(ServerLevel server, Vec3 at) {
        int color = switch (palette()) {
            case PRIME -> 0xFFA23A;
            case SENTRY -> 0x45B4F5;
            case HUMAN -> 0xE23B2E;
        };
        server.sendParticles(new EnergyParticleOptions(color, isHeavy() ? 2.0F : 1.0F), at.x, at.y, at.z, isHeavy() ? 14 : 4, 0.1, 0.1, 0.1, 0.12);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, isHeavy() ? 10 : 2, 0.1, 0.1, 0.1, 0.2);
        UltronSounds.BOLT.play(server, at, isHeavy() ? 1.3F : 0.4F, isHeavy() ? 0.6F : 1.3F);
    }
}
