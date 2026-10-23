package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A meteor of Titan rock that Thanos calls down from a portal in the sky. It falls straight onto the ring that marked
 * its spot and bursts there: radius 2.5, damage to whoever is in it, and no block changed unless the server's {@code
 * blockDamage} is on (and then only a small crater). A Chaos Shield stops it and pays its damage.
 *
 * <p>Telekinesis catches it in flight (Billy and the Phoenix; D-T 6.2.1): in the grip it is still and never bursts,
 * however low it is held, and let go without a throw it crumbles away harming nobody. Thrown, it is the thrower's: it
 * flies straight along his look and bursts on the first block or creature it meets, as his telekinetic slam, sparing
 * whom he may not hurt (himself, his friends without PvP, villagers and animals), and it never breaks a block.
 */
public class ThanosMeteorEntity extends Projectile implements ShieldCost, BossHooks {
    /** How far the burst reaches. */
    public static final double RADIUS = 2.5;
    /** How fast it falls, blocks per tick: from its height in the 24 ticks its ring warns. */
    public static final double FALL_SPEED = 1.0;
    /** How high above its ring it starts. */
    public static final double HEIGHT = 24.0;
    private static final int MAX_LIFE = 120;
    /** How long a meteor thrown back by telekinesis flies, at most (ticks). */
    private static final int TURNED_LIFE = 60;

    @Nullable
    private Vec3 landing;
    private int life;
    /** Thrown back by a hero's telekinesis: it is his (its owner) and flies straight until it meets something. */
    private boolean turned;

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
        if (level().isClientSide()) {
            setPos(from.add(getDeltaMovement()));
            level().addParticle(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 0, 0.2, 0);
            return;
        }
        if (Telekinesis.isHeld(this)) {
            return; // still in a grip: it neither falls nor bursts, and does not age (D-T 6.2.1)
        }
        ServerLevel level = (ServerLevel) level();
        if (turned) {
            tickTurned(level, from);
            return;
        }
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

    /**
     * A tick of a meteor thrown back: it flies straight, and bursts on the first block or creature (not its thrower) on
     * its way. It goes with its thrower if he leaves, and at the end of its life.
     */
    private void tickTurned(ServerLevel level, Vec3 from) {
        if (!(getOwner() instanceof ServerPlayer thrower) || thrower.isRemoved() || ++life > TURNED_LIFE) {
            discard();
            return;
        }
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() != HitResult.Type.MISS) {
            to = wall.getLocation();
        }
        // The rock is as wide as its box: what its middle passes within half of it, it meets.
        Vec3 middle = new Vec3(0.0, getBbHeight() * 0.5, 0.0);
        Vec3 start = from.add(middle);
        Vec3 end = to.add(middle);
        AABB sweep = new AABB(start, end).inflate(getBbWidth());
        Vec3 met = null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != thrower && e.isAlive() && !e.isSpectator())) {
            AABB box = e.getBoundingBox().inflate(getBbWidth() * 0.5);
            Optional<Vec3> at = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            if (at.isPresent() && (met == null || at.get().distanceToSqr(start) < met.distanceToSqr(start))) {
                met = at.get();
            }
        }
        if (met != null) {
            burstFor(level, thrower, met.subtract(middle));
        } else if (wall.getType() != HitResult.Type.MISS) {
            burstFor(level, thrower, to);
        } else {
            setPos(to);
            hasImpulse = true;
            if (life % 3 == 0) {
                level.sendParticles(new EnergyParticleOptions(StoneKind.SPACE.color(), 1.4F), getX(), getY() + 1.0, getZ(), 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    /**
     * The burst of a meteor thrown back: {@code meteorDamage} to everyone its thrower may hurt within its radius, as his
     * telekinetic slam, and no block changed, whatever {@code blockDamage} says.
     */
    private void burstFor(ServerLevel level, ServerPlayer thrower, Vec3 at) {
        setPos(at);
        float damage = ThanosBossConfig.METEOR_DAMAGE.get().floatValue();
        StonePowers.splash(level, thrower, at, RADIUS, damage, ModDamageTypes.TELEKINETIC_SLAM, 0.9, 0.5, this, false);
        ThanosEffects.shockRing(level, at, (float) RADIUS, 6, StoneKind.SPACE.color());
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.5, 0.3, 0.5, 0.0);
        ThanosSounds.POWER_BLAST.play(level, at, 0.8F, 1.1F);
        discard();
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

    /** A fight never survives a restart: neither he nor anything of his is written to the world. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public float shieldCost() {
        return ThanosBossConfig.METEOR_DAMAGE.get().floatValue();
    }

    // ---- telekinesis (3.1, D-T 6.2.1): a shot of the fight, still in the grip ----

    /** Whether a hero's telekinesis threw it back: it is his now. */
    public boolean isTurned() {
        return turned;
    }

    /** Caught: it falls toward its ring no more (held below where it would land, it does not burst). */
    @Override
    public void onGripped(ServerPlayer holder) {
        landing = null;
    }

    /** Let go without a throw: it crumbles away in a puff of the Space Stone's blue, harming nobody. */
    @Override
    public void onLetGo(ServerPlayer holder, boolean brokeFree) {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.SPACE.color(), 1.4F), getX(), getY() + getBbHeight() * 0.5, getZ(),
                    16, 0.5, 0.5, 0.5, 0.05);
        }
        discard();
    }

    /** Thrown by its holder: it is his, and leaves the fight; it flies straight along {@code velocity}. */
    @Override
    public boolean throwBack(ServerPlayer thrower, Vec3 velocity) {
        turned = true;
        life = 0;
        landing = null;
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
