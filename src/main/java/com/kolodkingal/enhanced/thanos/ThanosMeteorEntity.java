package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A meteor of Titan rock that Thanos calls down from a portal in the sky. It falls straight onto the ring that marked
 * its spot and bursts there: radius 2.5, damage to whoever is in it, and no block changed unless the server's {@code
 * blockDamage} is on (and then only a small crater). A Chaos Shield stops it and pays its damage.
 */
public class ThanosMeteorEntity extends Projectile implements ShieldCost {
    /** How far the burst reaches. */
    public static final double RADIUS = 2.5;
    /** How fast it falls, blocks per tick: from its height in the 24 ticks its ring warns. */
    public static final double FALL_SPEED = 1.0;
    /** How high above its ring it starts. */
    public static final double HEIGHT = 24.0;
    private static final int MAX_LIFE = 120;

    @Nullable
    private Vec3 landing;
    private int life;

    public ThanosMeteorEntity(EntityType<? extends ThanosMeteorEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Lets it go from {@code landing + 24 blocks up} toward {@code landing}. */
    public void launch(ThanosEntity thanos, Vec3 landing) {
        setOwner(thanos);
        this.landing = landing;
        setPos(landing.add(0, HEIGHT, 0));
        setDeltaMovement(0, -FALL_SPEED, 0);
    }

    /** Where it will land. */
    @Nullable
    public Vec3 landing() {
        return landing;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        if (level().isClientSide) {
            setPos(from.add(getDeltaMovement()));
            level().addParticle(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 0, 0.2, 0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (!(getOwner() instanceof ThanosEntity thanos) || !thanos.isAlive() || landing == null || ++life > MAX_LIFE) {
            discard();
            return;
        }
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            burst(level, thanos, hit.getLocation());
        } else if (to.y <= landing.y) {
            burst(level, thanos, new Vec3(landing.x, landing.y, landing.z));
        } else {
            setPos(to);
            hasImpulse = true;
            if (life % 3 == 0) {
                level.sendParticles(new EnergyParticleOptions(StoneKind.SPACE.color(), 1.4F), getX(), getY() + 1.0, getZ(), 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    private void burst(ServerLevel level, ThanosEntity thanos, Vec3 at) {
        setPos(at);
        float damage = ThanosBossConfig.METEOR_DAMAGE.get().floatValue() * thanos.damageScale();
        StonePowers.splash(level, thanos, at, RADIUS, damage, ModDamageTypes.STONE_BLAST, 0.9, 0.5, this, false);
        ThanosEffects.shockRing(level, at, (float) RADIUS, 6, StoneKind.SPACE.color());
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.5, 0.3, 0.5, 0.0);
        ThanosSounds.POWER_BLAST.play(level, at, 0.8F, 1.1F);
        StonePowers.craterIfAllowed(level, thanos, at, 1.5F);
        discard();
    }

    @Override
    public float shieldCost() {
        return ThanosBossConfig.METEOR_DAMAGE.get().floatValue();
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
