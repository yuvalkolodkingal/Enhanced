package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Redwing, Falcon's drone (3.8, PLAN 8.4): it flies two blocks over his right shoulder (through leaves and walls alike,
 * a drone's straight lines), turns to his look, and rams what Redwing Strike sends it at ({@link Redwing} holds the
 * rules: the marks, the zaps, the time, the repairs). It is never saved with the world (it lives in his pack), never
 * pushed, never hurt by him or by falling, and never dies messily: shot down, it drops into his pack in a shower of
 * sparks ({@link Redwing#shotDown}). {@code [falcon] redwingHealth} 12. Drawn by {@code
 * client/falcon/RedwingRenderer}.
 */
public class RedwingEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(
            RedwingEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(RedwingEntity.class,
            EntityDataSerializers.INT);

    /** How far over and beside his shoulder it flies (blocks). */
    static final double OVER = 0.55;
    static final double BESIDE = 0.75;
    /** Its speeds (blocks a tick) following him and ramming. */
    static final double FOLLOW_SPEED = 1.1;
    static final double STRIKE_SPEED = 1.5;
    /** Farther than this from him, it jumps to his shoulder. */
    static final double LEASH = 24.0;
    /** A ram that has not reached its target in this many ticks gives up. */
    static final int STRIKE_TICKS = 60;

    private int strikeTicks;

    public RedwingEntity(EntityType<? extends RedwingEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_TARGET, -1);
    }

    // ---- whose it is ----

    void setOwner(Player owner) {
        entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
    }

    /** Its Falcon's id (on both sides), or null. */
    @Nullable
    public UUID ownerId() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    /** Its Falcon, on the server, while he is in its world. */
    @Nullable
    ServerPlayer ownerPlayer() {
        UUID id = ownerId();
        if (id == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        Player player = server.getPlayerByUUID(id);
        return player instanceof ServerPlayer owner && owner.isAlive() ? owner : null;
    }

    /** The creature it is ramming (its id, on both sides), or -1. */
    public int strikeTarget() {
        return entityData.get(DATA_TARGET);
    }

    /** Whether it is ramming something now. */
    public boolean striking() {
        return strikeTarget() >= 0;
    }

    /** Redwing Strike: it flies at {@code target} and rams it. */
    void strike(LivingEntity target) {
        entityData.set(DATA_TARGET, target.getId());
        strikeTicks = 0;
    }

    // ---- its flight ----

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 3 == 0) {
                Vec3 back = Vec3.directionFromRotation(0.0F, getYRot()).scale(-0.3);
                level().addParticle(new EnergyParticleOptions(FalconAbilities.RED, 0.35F), getX() + back.x,
                        getY() + 0.12, getZ() + back.z, 0, -0.02, 0);
            }
            return;
        }
        ServerPlayer owner = ownerPlayer();
        if (owner == null || owner.level() != level()) {
            discard(); // (its Falcon is gone: it was in his pack all along)
            return;
        }
        if (striking()) {
            Entity target = level().getEntity(strikeTarget());
            if (!(target instanceof LivingEntity living) || !living.isAlive() || ++strikeTicks > STRIKE_TICKS) {
                entityData.set(DATA_TARGET, -1);
            } else {
                Vec3 to = living.getBoundingBox().getCenter();
                approach(to, STRIKE_SPEED);
                face(to.subtract(position()));
                if (getBoundingBox().inflate(0.35).intersects(living.getBoundingBox())) {
                    entityData.set(DATA_TARGET, -1);
                    Redwing.strikeHit(this, living);
                }
                return;
            }
        }
        Vec3 home = shoulder(owner);
        if (home.distanceTo(position()) > LEASH) {
            moveTo(home.x, home.y, home.z, owner.getYRot(), 0.0F);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        approach(home, FOLLOW_SPEED);
        float yaw = Mth.rotLerp(0.3F, getYRot(), owner.getYHeadRot());
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
        setXRot(Mth.lerp(0.3F, getXRot(), owner.getXRot() * 0.5F));
    }

    /** Where it hovers: two blocks over his right shoulder, and a little behind. */
    static Vec3 shoulder(Player owner) {
        Vec3 facing = Vec3.directionFromRotation(0.0F, owner.getYHeadRot());
        Vec3 right = new Vec3(-facing.z, 0, facing.x);
        double bob = 0.08 * Math.sin(owner.tickCount * 0.15);
        return owner.position().add(0, owner.getBbHeight() + OVER + bob, 0).add(right.scale(BESIDE))
                .subtract(facing.scale(0.25));
    }

    /** It moves toward {@code to}: quickly when far, settling as it arrives, at most {@code max} a tick. */
    private void approach(Vec3 to, double max) {
        Vec3 d = to.subtract(position());
        double distance = d.length();
        Vec3 motion = distance < 1.0E-4 ? Vec3.ZERO : d.scale(Math.min(max, Math.max(distance * 0.45,
                Math.min(distance, 0.05))) / distance);
        setDeltaMovement(motion);
    }

    private void face(Vec3 d) {
        if (d.horizontalDistanceSqr() < 1.0E-6) {
            return;
        }
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    @Override
    public void travel(Vec3 input) {
        // (A drone: no gravity, no friction, no walls; it moves as its motion says.)
        move(MoverType.SELF, getDeltaMovement());
    }

    // ---- what it is not ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        UUID owner = ownerId();
        Entity by = source.getEntity();
        if (owner != null && by != null && owner.equals(by.getUUID())) {
            return false; // (never by its own Falcon)
        }
        if (source.getDirectEntity() instanceof Projectile p && p.getOwner() != null && owner != null
                && owner.equals(p.getOwner().getUUID())) {
            return false;
        }
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.CRAMMING)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        // Shot down: no fall, no loot, no message; sparks, and it drops into his pack for repairs.
        if (level() instanceof ServerLevel server && !isRemoved()) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.2, getZ(), 14, 0.2, 0.2, 0.2, 0.2);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 6, 0.15, 0.15, 0.15, 0.02);
            Redwing.shotDown(this);
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // (it lives in his pack, not in the world)
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state,
                                   net.minecraft.core.BlockPos pos) {
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        UUID owner = ownerId();
        return owner != null && owner.equals(other.getUUID()) || super.isAlliedTo(other);
    }
}
