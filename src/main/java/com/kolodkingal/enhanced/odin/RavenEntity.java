package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Huginn or Muninn (3.5), one of Odin's two ravens: a small black bird that circles a point in the air for a while and
 * then is gone. Huginn and Muninn (B) circle the spot where he looked; the Ravens' Report (Sneak + B) circles him. While
 * they fly, every living thing within {@link #SIGHT} blocks of them glows through walls for him alone (the client's own
 * reckoning, from the ravens it sees: {@code RavenSight}), and the HUD counts it.
 *
 * <p>Its flight is reckoned on the server, a circle round its centre with a slow rise and fall, so it never paths, never
 * collides and never lands; it is never saved (a raven outlives no restart). It has 4 health: anyone but its owner can
 * shoot it down, and then it is gone until the ravens are called again. It drops nothing.
 */
public class RavenEntity extends Mob implements OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_REPORT =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.BOOLEAN);
    /** How far from a raven a creature is seen. */
    public static final double SIGHT = 32.0;
    /** Blocks per tick along its circle. */
    private static final double SPEED = 0.55;

    private Vec3 center = Vec3.ZERO;
    private double radius = 16.0;
    private double angle;
    /** +1 or -1: the way it turns (the two ravens circle against each other). */
    private int turn = 1;
    /** The game time at which it leaves. */
    private long leavesAt;
    /** Whether it circles its owner (the report), whose position is its centre every tick. */
    private boolean followsOwner;
    /** Ticks it spends flying out from his shoulder to its circle. */
    private int outbound = 20;

    public RavenEntity(EntityType<? extends RavenEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        setPersistenceRequired();
    }

    /**
     * A raven of {@code odin}'s, sent from his shoulder to circle {@code center} at {@code radius} until {@code leavesAt}.
     *
     * @param phase where on the circle it flies (radians), so the two never meet
     */
    public static RavenEntity send(ServerLevel level, ServerPlayer odin, Vec3 center, double radius, double phase, int turn, long leavesAt,
                                   boolean report) {
        RavenEntity raven = new RavenEntity(ModEntities.RAVEN.get(), level);
        raven.entityData.set(DATA_OWNER, Optional.of(odin.getUUID()));
        raven.entityData.set(DATA_REPORT, report);
        raven.center = center;
        raven.radius = radius;
        raven.angle = phase;
        raven.turn = turn;
        raven.leavesAt = leavesAt;
        raven.followsOwner = report;
        Vec3 shoulder = odin.position().add(0, odin.getBbHeight() * 0.9, 0).add(Vec3.directionFromRotation(0, odin.getYRot() + 90 * turn).scale(0.5));
        raven.moveTo(shoulder.x, shoulder.y, shoulder.z, odin.getYRot(), 0.0F);
        return raven;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.FLYING_SPEED, 0.6).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_REPORT, false);
    }

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    /** Whether it flies for the Ravens' Report (circling its owner) rather than scouting a spot. */
    public boolean report() {
        return entityData.get(DATA_REPORT);
    }

    /** The game time at which it leaves (server). */
    public long leavesAt() {
        return leavesAt;
    }

    /** Whether this raven belongs to {@code player}. */
    public boolean ownedBy(Player player) {
        return player.getUUID().equals(getOwnerUUID());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayer odin) || !odin.isAlive() || odin.level() != level || level.getGameTime() >= leavesAt) {
            leave();
            return;
        }
        if (followsOwner) {
            center = odin.position().add(0, 6.0, 0);
        }
        angle += turn * SPEED / Math.max(4.0, radius);
        double bob = Math.sin(tickCount * 0.11 + angle) * 1.2;
        Vec3 want = center.add(Math.cos(angle) * radius, bob, Math.sin(angle) * radius);
        Vec3 at = position();
        Vec3 next;
        if (outbound > 0) {
            // Flying out from his shoulder: straight at its place on the circle, fast.
            outbound--;
            Vec3 to = want.subtract(at);
            double d = to.length();
            next = d < 1.6 ? want : at.add(to.scale(Math.min(1.6, d) / d));
            if (d < 1.6) {
                outbound = 0;
            }
        } else {
            next = want;
        }
        Vec3 motion = next.subtract(at);
        setDeltaMovement(motion);
        setPos(next.x, next.y, next.z);
        if (motion.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(-motion.x, motion.z));
            setYRot(yaw);
            setYHeadRot(yaw);
            yBodyRot = yaw;
        }
        if (tickCount % 80 == (getId() & 63)) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.RAVEN_CAW.get(), SoundSource.NEUTRAL, 0.8F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    /** Its time is up (or its owner is gone): a puff of black feathers, and it is gone. */
    public void leave() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 6, 0.15, 0.15, 0.15, 0.01);
            level.playSound(null, getX(), getY(), getZ(), ModSounds.RAVEN_FLAP.get(), SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Never by its owner (nor by what he throws or shoots).
        if (source.getEntity() instanceof Player player && ownedBy(player)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        // A raven leaves nothing behind.
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RAVEN_CAW.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RAVEN_FLAP.get();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
