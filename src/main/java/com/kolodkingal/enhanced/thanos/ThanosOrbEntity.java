package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 *
 * <p>Telekinesis catches it in flight (Billy and the Phoenix; D-T 6.2.1): in the grip it is still and petrifies nothing it
 * touches, and let go without a throw it fades away. Thrown, it is the thrower's: the first creature he may hurt that it
 * touches is struck by the stone's reality for him: Thanos is staggered, a Phantom of his dissolves, any other creature
 * is petrified.
 */
public class ThanosOrbEntity extends Projectile implements ShieldCost, BossHooks {
    public static final double SPEED = 0.55;
    public static final int LIFE = 50;
    /** The strength a Chaos Shield loses stopping it. */
    public static final float SHIELD_COST = 2.0F;

    private int life;
    /** Thrown back by a hero's telekinesis: it is his (its owner), and strikes for him. */
    private boolean turned;

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
        if (level().isClientSide()) {
            setPos(from.add(getDeltaMovement()));
            return;
        }
        if (Telekinesis.isHeld(this)) {
            return; // still in a grip: it flies nowhere, petrifies nothing and does not age (D-T 6.2.1)
        }
        ServerLevel level = (ServerLevel) level();
        LivingEntity caster;
        if (turned) {
            if (!(getOwner() instanceof ServerPlayer thrower) || thrower.isRemoved() || ++life > LIFE) {
                discard();
                return;
            }
            caster = thrower;
        } else {
            if (!(getOwner() instanceof ThanosEntity thanos) || !thanos.isAlive() || ++life > LIFE) {
                discard();
                return;
            }
            caster = thanos;
        }
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() != HitResult.Type.MISS) {
            to = wall.getLocation();
        }
        AABB sweep = new AABB(from, to).inflate(0.6);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive() && !e.isSpectator()
                && !BossFights.sameFight(caster, e) && AreaTargets.mayHurt(caster, e, false))) {
            AABB box = e.getBoundingBox().inflate(0.4);
            if (box.contains(from) || box.clip(from, to).isPresent()) {
                touch(level, caster, e);
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

    /**
     * The orb touches a creature: Thanos's petrifies it. One a hero threw back staggers Thanos, dissolves a Phantom of his
     * and petrifies anything else, for the thrower.
     */
    private void touch(ServerLevel level, LivingEntity caster, LivingEntity e) {
        if (turned && e instanceof ThanosEntity thanos) {
            thanos.stagger();
        } else if (turned && e instanceof ThanosPhantomEntity phantom) {
            phantom.dissolve();
        } else {
            StonePowers.petrify(level, caster, e, 60);
        }
    }

    /** A fight never survives a restart: neither he nor anything of his is written to the world. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public float shieldCost() {
        return SHIELD_COST;
    }

    // ---- telekinesis (3.1, D-T 6.2.1): a shot of the fight, still in the grip ----

    /** Whether a hero's telekinesis threw it back: it is his now. */
    public boolean isTurned() {
        return turned;
    }

    /** Let go without a throw: it fades in a puff of the Reality Stone's red, petrifying nobody. */
    @Override
    public void onLetGo(ServerPlayer holder, boolean brokeFree) {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.color(), 1.3F), getX(), getY(), getZ(), 12, 0.3, 0.3, 0.3, 0.04);
        }
        discard();
    }

    /** Thrown by its holder: it is his, and leaves the fight; it flies straight along {@code velocity}, a fresh life. */
    @Override
    public boolean throwBack(ServerPlayer thrower, Vec3 velocity) {
        turned = true;
        life = 0;
        setOwner(thrower);
        BossFights.disown(this);
        setDeltaMovement(velocity);
        hasImpulse = true;
        hurtMarked = true;
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
}
