package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * A sphere of the Reality Stone that Thanos throws. It flies straight and slowly, and turns the first creature it
 * touches to stone for three seconds (the PETRIFIED effect); a block or its fifty ticks of life end it. A Chaos Shield
 * stops it.
 */
public class ThanosOrbEntity extends Projectile implements ShieldCost {
    public static final double SPEED = 0.55;
    public static final int LIFE = 50;
    /** The strength a Chaos Shield loses stopping it. */
    public static final float SHIELD_COST = 2.0F;

    private int life;

    public ThanosOrbEntity(EntityType<? extends ThanosOrbEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Throws the orb from {@code from} along {@code direction}. */
    public void launch(ThanosEntity thanos, Vec3 from, Vec3 direction) {
        setOwner(thanos);
        setPos(from);
        setDeltaMovement(direction.normalize().scale(SPEED));
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
        if (!(getOwner() instanceof ThanosEntity thanos) || !thanos.isAlive() || ++life > LIFE) {
            discard();
            return;
        }
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() != HitResult.Type.MISS) {
            to = wall.getLocation();
        }
        AABB sweep = new AABB(from, to).inflate(0.6);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != thanos && e.isAlive() && !e.isSpectator()
                && !BossFights.sameFight(thanos, e) && AreaTargets.mayHurt(thanos, e, false))) {
            AABB box = e.getBoundingBox().inflate(0.4);
            if (box.contains(from) || box.clip(from, to).isPresent()) {
                StonePowers.petrify(level, thanos, e, 60);
                discard();
                return;
            }
        }
        setPos(to);
        hasImpulse = true;
        if (wall.getType() != HitResult.Type.MISS) {
            discard();
            return;
        }
        if (life % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.color(), 1.3F), getX(), getY(), getZ(), 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    @Override
    public float shieldCost() {
        return SHIELD_COST;
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
}
