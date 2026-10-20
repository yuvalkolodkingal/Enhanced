package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One of the Mark 85's <b>nanotech drones</b> (3.8, lane L4, PLAN 6.2; Eilon's "nanotech drones appear behind you and
 * fire very strong laser beams"). Two form behind his shoulders (Sneak + N) and follow him there for {@code droneTicks}
 * 400; every {@code droneShotGap} 15 ticks each fires a laser flash ({@code drone_laser}, {@code droneDamage} 6) at the
 * nearest hostile or attacker of his within {@code droneRange} 20 that it can see (never a player without PvP, never a
 * pet, never a passive creature: {@code AreaTargets}' rule). It has no health: a hit only makes it flicker. It ends
 * with him (his time, his death or logout, another dimension, the suit off) and is never saved.
 */
public class NanoDroneEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(NanoDroneEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(NanoDroneEntity.class,
            EntityDataSerializers.INT);
    /** The drone's age when a hit made it flicker last (-100: never). */
    private static final EntityDataAccessor<Integer> FLICKER = SynchedEntityData.defineId(NanoDroneEntity.class,
            EntityDataSerializers.INT);
    /** How long a hit's flicker lasts. */
    public static final int FLICKER_TICKS = 10;

    /** When it goes (game time), and its next shot's tick (its age). */
    private long until;
    private int nextShot;

    public NanoDroneEntity(EntityType<? extends NanoDroneEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /**
     * A new drone of {@code owner} in shoulder slot {@code slot} (0 right, 1 left), gone at game time {@code until}.
     */
    public static NanoDroneEntity of(ServerPlayer owner, int slot, long until, int firstShot) {
        NanoDroneEntity drone = new NanoDroneEntity(com.kolodkingal.enhanced.registry.ModEntities.NANO_DRONE.get(),
                owner.level());
        drone.entityData.set(OWNER, owner.getId());
        drone.entityData.set(SLOT, slot);
        drone.until = until;
        drone.nextShot = firstShot;
        Vec3 at = drone.post(owner);
        drone.moveTo(at.x, at.y, at.z, owner.getYRot(), 0.0F);
        return drone;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(SLOT, 0);
        builder.define(FLICKER, -100);
    }

    /** Its Iron Man (on either side), or null when he is not in this level. */
    @Nullable
    public Player owner() {
        return level().getEntity(entityData.get(OWNER)) instanceof Player p ? p : null;
    }

    public int slot() {
        return entityData.get(SLOT);
    }

    /** How strongly it flickers now, from a hit (0-1). */
    public float flicker(float partialTick) {
        float since = tickCount + partialTick - entityData.get(FLICKER);
        return since < 0.0F || since > FLICKER_TICKS ? 0.0F : 1.0F - since / FLICKER_TICKS;
    }

    /** Where it hovers: behind his shoulder (its slot's side), a little above it. */
    public Vec3 post(Player owner) {
        float scale = owner.getScale();
        float yaw = owner.yBodyRot;
        Vec3 back = Vec3.directionFromRotation(0.0F, yaw).scale(-0.9 * scale);
        Vec3 side = Vec3.directionFromRotation(0.0F, yaw + (slot() == 0 ? 90.0F : -90.0F)).scale(0.75 * scale);
        double bob = Math.sin((tickCount + slot() * 10) * 0.15) * 0.06;
        return owner.position().add(0.0, 1.75 * scale + bob, 0.0).add(back).add(side);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerPlayer owner = owner() instanceof ServerPlayer p ? p : null;
        if (owner == null || !owner.isAlive() || owner.isRemoved() || level().getGameTime() >= until
                || !NanoDrones.keeps(owner)) {
            discard();
            return;
        }
        Vec3 want = post(owner);
        Vec3 now = position();
        // Follow smoothly (a quick catch-up when far: he flies fast).
        Vec3 next = now.distanceToSqr(want) > 64.0 ? want : now.add(want.subtract(now).scale(0.45));
        setPos(next.x, next.y, next.z);
        setYRot(Mth.rotLerp(0.4F, getYRot(), owner.getYRot()));
        if (tickCount >= nextShot) {
            nextShot = tickCount + NanoTech.cfg(IronManConfig.MARK85_DRONE_SHOT_GAP, 15);
            shoot(owner);
        }
    }

    /** A laser flash at the nearest foe it can see, if any. */
    @Nullable
    LivingEntity shoot(ServerPlayer owner) {
        LivingEntity target = target(owner);
        if (target == null) {
            return null;
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 from = position().add(0.0, getBbHeight() * 0.5, 0.0);
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        NanoTech.hurt(owner, target, ModDamageTypes.DRONE_LASER,
                (float) NanoTech.cfg(IronManConfig.MARK85_DRONE_DAMAGE, 6.0));
        NanoTech.send(level, this, IronFxPayload.DRONE_SHOT, List.of(from, to), owner.getId());
        NanoTech.sound(level, from, ModSounds.DRONE_LASER.get(), 0.7F, 1.0F + level.random.nextFloat() * 0.2F);
        return target;
    }

    /** The nearest hostile or attacker of his it may shoot within range and sight. */
    @Nullable
    LivingEntity target(ServerPlayer owner) {
        double range = NanoTech.cfg(IronManConfig.MARK85_DRONE_RANGE, 20.0);
        Vec3 from = position().add(0.0, getBbHeight() * 0.5, 0.0);
        LivingEntity attacker = owner.getLastHurtByMob();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range),
                e -> HomingMissiles.hostile(owner, e) || e == attacker && NanoTech.mayHurt(owner, e))) {
            Vec3 middle = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            double dist = middle.distanceTo(from);
            if (dist > range || dist >= bestDist || !clear(from, middle)) {
                continue;
            }
            best = e;
            bestDist = dist;
        }
        return best;
    }

    /** Whether no block stands between two points. */
    private boolean clear(Vec3 from, Vec3 to) {
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isInvulnerableTo(source)) {
            entityData.set(FLICKER, tickCount);
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }
}
