package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thanos's Double-Edged Sword, thrown. It flies straight, spinning, until it meets a wall or has flown 28 blocks; then it
 * turns and comes home to his hand, straight through walls, hitting everything on the way back too. Each creature is
 * hit once per pass. While it is away he has no Sweep and no Smash.
 *
 * <p>A Chaos Shield turns it home as it does Mjolnir ({@link ShieldCost#oneOfAKind}) and lets it by on the way back. It
 * belongs to its owner: Thor cannot catch it and the villains' pulls cannot take it (the tag {@code pull_immune}). It is
 * metal to Magneto, and a telekinesis that holds it holds it {@code thanos_boss.swordHoldTicks} at most, Magneto {@code
 * boss.magnetoHoldFactor} times as long ({@link BossHooks}, R8). If Thanos is gone it is gone with him.
 */
public class ThanosSwordEntity extends Projectile implements ShieldCost, BossHooks {
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(ThanosSwordEntity.class, EntityDataSerializers.BOOLEAN);
    /** How far it flies out before it turns. */
    public static final double RANGE = 28.0;
    public static final double THROW_SPEED = 1.3;
    public static final double RETURN_SPEED = 1.5;
    /** However it goes, the sword does not stay out longer than this (ticks). */
    private static final int MAX_LIFE = 200;

    private final IntSet hitOut = new IntOpenHashSet();
    private final IntSet hitBack = new IntOpenHashSet();
    private double travelled;
    private int life;

    public ThanosSwordEntity(EntityType<? extends ThanosSwordEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RETURNING, false);
    }

    /** Throws the sword from his hand along {@code direction}. */
    public void launch(ThanosEntity thanos, Vec3 from, Vec3 direction) {
        setOwner(thanos);
        setPos(from);
        setDeltaMovement(direction.normalize().scale(THROW_SPEED));
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    /** Turns it around, homeward. */
    public void setReturning(boolean returning) {
        entityData.set(DATA_RETURNING, returning);
        noPhysics = returning;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        if (level().isClientSide) {
            setPos(from.add(getDeltaMovement()));
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (!(getOwner() instanceof ThanosEntity thanos) || !thanos.isAlive() || ++life > MAX_LIFE) {
            discard();
            return;
        }
        Vec3 motion;
        if (!isReturning()) {
            motion = getDeltaMovement();
            Vec3 to = from.add(motion);
            BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean hitWall = wall.getType() != HitResult.Type.MISS;
            if (hitWall) {
                to = wall.getLocation();
            }
            smash(level, thanos, from, to, hitOut);
            setPos(to);
            travelled += to.distanceTo(from);
            if (hitWall || travelled >= RANGE) {
                setReturning(true);
            }
        } else {
            Vec3 hand = thanos.swordHandPosition();
            Vec3 toHand = hand.subtract(from);
            double distance = toHand.length();
            if (distance < 1.6) {
                thanos.catchSword(this);
                return;
            }
            motion = toHand.scale(Math.min(RETURN_SPEED, distance) / distance);
            Vec3 to = from.add(motion);
            smash(level, thanos, from, to, hitBack);
            setPos(to);
        }
        setDeltaMovement(motion);
        hasImpulse = true;
    }

    /** Everything along the segment that has not been hit on this pass takes the blade. */
    private void smash(ServerLevel level, ThanosEntity thanos, Vec3 from, Vec3 to, IntSet alreadyHit) {
        AABB sweep = new AABB(from, to).inflate(1.0);
        float damage = ThanosBossConfig.SWORD_THROW_DAMAGE.get().floatValue() * thanos.damageScale();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != thanos && e.isAlive() && !e.isSpectator()
                && !alreadyHit.contains(e.getId()) && !BossFights.sameFight(thanos, e) && AreaTargets.mayHurt(thanos, e, false))) {
            AABB box = e.getBoundingBox().inflate(0.5);
            if (!box.contains(from) && box.clip(from, to).isEmpty()) {
                continue;
            }
            alreadyHit.add(e.getId());
            if (StonePowers.hurt(level, thanos, e, ModDamageTypes.THANOS_SWORD, damage, this)) {
                StonePowers.shove(e, from, 0.9, 0.2);
                ThanosSounds.HIT.play(level, e.position());
            }
        }
    }

    /** A fight never survives a restart: neither he nor anything of his is written to the world. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public float shieldCost() {
        return ThanosBossConfig.SWORD_THROW_DAMAGE.get().floatValue();
    }

    @Override
    public boolean oneOfAKind() {
        return true;
    }

    @Override
    public void turnHome() {
        setReturning(true);
    }

    @Override
    public boolean returning() {
        return isReturning();
    }

    // ---- telekinesis (3.1, D-T 6.2.1): a shot of the fight, held for a while ----

    /** {@code [thanos_boss].swordHoldTicks}: it tears free of a grip after that (Magneto's lasts 1.5 times as long, R8). */
    @Override
    public int holdTicks() {
        return ThanosBossConfig.SWORD_HOLD_TICKS.get();
    }

    /** The Double-Edged Sword is metal: Magneto's R may catch it (D-M 2.3, R9). */
    @Override
    public boolean isMetal() {
        return true;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && getOwner() instanceof ThanosEntity thanos) {
            thanos.swordGone(this);
        }
        super.remove(reason);
    }
}
