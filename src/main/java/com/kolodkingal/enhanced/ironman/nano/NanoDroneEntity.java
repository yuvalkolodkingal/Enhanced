package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.AvengersBeams;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One of the Mark 85's <b>nanotech drones</b> (3.8, lane L4, PLAN 6.2; Eilon's "nanotech drones appear behind you and
 * fire very strong laser beams"). Since 3.8.3 each drone is one side's <b>wing unit</b> of the Endgame look: two curved
 * crescent blades fanned out beside his arm ({@link WingFormation}), so the two drones (Sneak + N) make four blades,
 * two on each side. They follow him for {@code droneTicks} 400; every {@code droneShotGap} 15 ticks each drone fires a
 * laser flash ({@code drone_laser}, {@code droneDamage} 6) from its inner and outer blade in turn (the middle of the
 * blade's inner edge) at the nearest hostile or attacker of his within {@code droneRange} 20 that it can see (never a
 * player without PvP, never a pet, never a passive creature: {@code AreaTargets}' rule). The same shots as before, only
 * from four blades: the damage per second is unchanged. It has no health: a hit only makes it flicker. It ends with him
 * (his time, his death or logout, another dimension, the suit off) and is never saved.
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

    /** When it goes (game time), its next shot's tick (its age), and its shots so far (which blade fires next). */
    private long until;
    private int nextShot;
    private int shots;

    public NanoDroneEntity(EntityType<? extends NanoDroneEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /**
     * A new drone of {@code owner} in slot {@code slot} (0 his right wing, 1 his left; {@link WingFormation#side}),
     * gone at game time {@code until}.
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

    /** Where it hovers: the middle of its two blades beside his arm ({@link WingFormation#centre}). */
    public Vec3 post(Player owner) {
        return WingFormation.centre(owner, slot(), level().getGameTime());
    }

    /** Where blade {@code blade} (0 inner, 1 outer) of this drone fires from now: its inner edge's middle. */
    public Vec3 muzzle(Player owner, int blade) {
        return WingFormation.muzzle(owner, slot(), blade, level().getGameTime());
    }

    /** The blade (0 inner, 1 outer) that fires the next shot. */
    public int nextBlade() {
        return shots % WingFormation.BLADES;
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
        setYRot(Mth.rotLerp(0.4F, getYRot(), owner.yBodyRot));
        if (tickCount >= nextShot) {
            nextShot = tickCount + NanoTech.cfg(IronManConfig.MARK85_DRONE_SHOT_GAP, 15);
            shoot(owner);
        }
    }

    /**
     * A laser flash at the nearest foe it can see, if any, from its next blade's inner edge (3.8.3). (3.8.2) A Chaos
     * Shield in the way stops it at its surface: a flash there, and the shield pays the shot.
     */
    @Nullable
    LivingEntity shoot(ServerPlayer owner) {
        int blade = nextBlade();
        Vec3 from = muzzle(owner, blade);
        LivingEntity target = target(owner, from);
        if (target == null) {
            return null;
        }
        shots++;
        ServerLevel level = (ServerLevel) level();
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        float damage = (float) NanoTech.cfg(IronManConfig.MARK85_DRONE_DAMAGE, 6.0);
        WandaAbilities.ShieldStop wall = NanoTech.wallAcross(owner, from, to);
        if (wall != null) {
            to = wall.point();
            NanoTech.stopped(owner, wall, damage, AvengersBeams.DRONE_LASER.main());
        } else {
            NanoTech.hurt(owner, target, ModDamageTypes.DRONE_LASER, damage);
        }
        // The clients draw the laser from the blade as they see it ({@code arg}: which blade).
        NanoTech.send(level, this, IronFxPayload.DRONE_SHOT, List.of(from, to), blade);
        NanoTech.sound(level, from, ModSounds.DRONE_LASER.get(), 0.7F, 1.0F + level.random.nextFloat() * 0.2F);
        return target;
    }

    /** The nearest hostile or attacker of his it may shoot from {@code from} within range and sight. */
    @Nullable
    LivingEntity target(ServerPlayer owner, Vec3 from) {
        double range = NanoTech.cfg(IronManConfig.MARK85_DRONE_RANGE, 20.0);
        LivingEntity attacker = owner.getLastHurtByMob();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(range),
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

    /** Seen from far: the wings are big (a second player sees them across a battle). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
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
