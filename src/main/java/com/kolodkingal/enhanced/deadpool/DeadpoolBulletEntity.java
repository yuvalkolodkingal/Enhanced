package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * One of Deadpool's cartoon bullets (3.7): a bright gold tracer, no gravity, never slows, never breaks or burns a block
 * (it ends in a spark). A projectile ({@code #minecraft:is_projectile} through its damage type), so the Reality Stone
 * bubbles it and shields stop it. Whom it hits is the heroes' rule ({@link AreaTargets#mayHurt}: monsters, those after
 * him, players only with PvP; never his pets, villagers or animals: the round passes them by).
 *
 * <p>The Trick Shot's round ({@link #trick}) bounces once: off the first block it meets, toward the nearest creature he
 * may hurt within {@code trickShotSeek} of the bounce that the bounce point can see (a ricochet's spark and ping), or,
 * with nobody there, off the face like a ball; the second block ends it.
 */
public class DeadpoolBulletEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Boolean> DATA_TRICK = SynchedEntityData.defineId(DeadpoolBulletEntity.class,
            EntityDataSerializers.BOOLEAN);

    private float damage = 4.0F;
    /** How many ticks it flies before it fades. */
    private int maxLife = 14;
    private int life;
    /** The bounces it still has (the Trick Shot's one). */
    private int bounces;

    public DeadpoolBulletEntity(EntityType<? extends DeadpoolBulletEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /**
     * A round from {@code from} along {@code direction} at {@code speed} blocks a tick, fired by {@code owner}, that
     * fades after {@code range} blocks; {@code trick}: the Trick Shot's, with its bounce. Not added to the level.
     */
    public static DeadpoolBulletEntity create(Level level, @Nullable Entity owner, Vec3 from, Vec3 direction,
                                              double speed, float damage, double range, boolean trick) {
        DeadpoolBulletEntity bullet = new DeadpoolBulletEntity(ModEntities.DEADPOOL_BULLET.get(), level);
        bullet.setOwner(owner);
        bullet.setPos(from);
        bullet.damage = damage;
        bullet.maxLife = Math.max(1, (int) Math.ceil(range / Math.max(0.1, speed)));
        bullet.bounces = trick ? 1 : 0;
        bullet.entityData.set(DATA_TRICK, trick);
        bullet.setDeltaMovement(direction.normalize().scale(speed));
        bullet.updateRotation();
        return bullet;
    }

    public float damage() {
        return damage;
    }

    /** Whether it is the Trick Shot's round (drawn a little bigger and brighter). */
    public boolean trick() {
        return entityData.get(DATA_TRICK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TRICK, false);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        if (!isInWater()) {
            // A thrown thing loses a hundredth of its speed every tick; a cartoon bullet does not.
            setDeltaMovement(getDeltaMovement().scale(1.0 / 0.99));
        }
        if (++life > maxLife && level() instanceof ServerLevel server) {
            spark(server, position(), false);
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (!super.canHitEntity(target) || target == owner || target instanceof DeadpoolBulletEntity) {
            return false;
        }
        if (!(target instanceof LivingEntity living) || target instanceof ArmorStand || !living.isAlive()) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (owner instanceof LivingEntity shooter) {
            return AreaTargets.mayHurt(shooter, living, false);
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (owner instanceof ServerPlayer shooter && target instanceof LivingEntity living) {
            DeadpoolAbilities.hurt(shooter, living, ModDamageTypes.DEADPOOL_BULLET, damage, this);
        } else {
            target.invulnerableTime = 0;
            target.hurt(ModDamageTypes.source(server, ModDamageTypes.DEADPOOL_BULLET, owner, this), damage);
        }
        spark(server, result.getLocation(), true);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        if (bounces > 0) {
            bounces--;
            ricochet(server, result);
            return;
        }
        spark(server, result.getLocation(), true);
        discard();
    }

    /**
     * The Trick Shot's bounce: from just off the face it hit, toward the nearest creature the shooter may hurt within
     * the seek radius that the bounce point sees, else off the face like a ball.
     */
    private void ricochet(ServerLevel server, BlockHitResult result) {
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 at = result.getLocation().add(normal.scale(0.25));
        double speed = Math.max(1.0, getDeltaMovement().length());
        LivingEntity prey = seek(server, at);
        Vec3 motion;
        if (prey != null) {
            motion = prey.getBoundingBox().getCenter().subtract(at).normalize().scale(speed);
        } else {
            Vec3 v = getDeltaMovement();
            motion = v.subtract(normal.scale(2.0 * v.dot(normal)));
        }
        setPos(at);
        setDeltaMovement(motion);
        updateRotation();
        life = 0;
        hasImpulse = true;
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.08, 0.08, 0.08, 0.25);
        server.playSound(null, at.x, at.y, at.z, ModSounds.DEADPOOL_RICOCHET.get(), SoundSource.PLAYERS, 0.9F,
                1.0F + server.random.nextFloat() * 0.2F);
        DeadpoolAbilities.send(server, this, DeadpoolFxPayload.RICOCHET,
                prey != null ? List.of(at, prey.getBoundingBox().getCenter()) : List.of(at, at.add(motion)), 0);
    }

    /** The nearest creature within the seek radius of {@code at} that the shooter may hurt and {@code at} can see. */
    @Nullable
    private LivingEntity seek(ServerLevel server, Vec3 at) {
        double radius = DeadpoolAbilities.cfg(DeadpoolConfig.TRICK_SHOT_SEEK, 12);
        List<LivingEntity> near = server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e.isAlive() && canHitEntity(e) && e.getBoundingBox().getCenter().distanceTo(at) <= radius);
        near.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at)));
        for (LivingEntity e : near) {
            Vec3 to = e.getBoundingBox().getCenter();
            HitResult sight = server.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    this));
            if (sight.getType() == HitResult.Type.MISS) {
                return e;
            }
        }
        return null;
    }

    /** A gold spark where it ends (a crack when it hit something). */
    private void spark(ServerLevel server, Vec3 at, boolean hit) {
        server.sendParticles(new EnergyParticleOptions(DeadpoolAbilities.TRACER, hit ? 1.0F : 0.6F), at.x, at.y, at.z,
                hit ? 5 : 2, 0.1, 0.1, 0.1, 0.1);
        if (hit) {
            server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0.2);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("maxLife", maxLife);
        tag.putInt("life", life);
        tag.putInt("bounces", bounces);
        tag.putBoolean("trick", trick());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        maxLife = Math.max(1, tag.getInt("maxLife"));
        life = tag.getInt("life");
        bounces = tag.getInt("bounces");
        entityData.set(DATA_TRICK, tag.getBoolean("trick"));
    }
}
