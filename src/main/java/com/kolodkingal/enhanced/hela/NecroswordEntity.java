package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.ability.ShieldCost;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A necrosword (3.5): one of Hela's black blades with green edges. Thrown ({@link #throwFrom}) it flies straight at 1.6
 * blocks a tick with no gravity and strikes the first creature its maker may hurt ({@link HelaRules#canHurt}) for its
 * damage of {@code necrosword} (dark magic: a ward of the Vishanti keeps it out); fallen ({@link #rainAt}) it hangs
 * point down over its mark through the telegraph of a Blade Rain and then drops. It breaks on the first block it meets.
 *
 * <p>A shield stops it as any boss projectile ({@link ShieldCost}: it costs its damage); Death's Grasp crumbles it.
 * Never saved; it lives at most {@link #MAX_LIFE} ticks of its own time.
 */
public class NecroswordEntity extends ThrowableProjectile implements ShieldCost {
    /** A blade lives at most this many ticks of its own time (hanging included). */
    public static final int MAX_LIFE = 100;
    /** How fast a thrown blade flies, and a falling one drops, in blocks per tick. */
    public static final float THROW_SPEED = 1.6F;
    public static final float FALL_SPEED = 1.4F;
    /** How high over its mark a blade of the Blade Rain hangs. */
    public static final double RAIN_HEIGHT = 9.0;

    /** Ticks a falling blade still hangs before it drops (0: it flies). Synced: the client draws it hanging, trembling. */
    private static final EntityDataAccessor<Integer> DATA_HANG = SynchedEntityData.defineId(NecroswordEntity.class, EntityDataSerializers.INT);
    /** Whether it is a blade of the rain (drawn point down). */
    private static final EntityDataAccessor<Boolean> DATA_FALLING = SynchedEntityData.defineId(NecroswordEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 7.0F;
    private double life;

    public NecroswordEntity(EntityType<? extends NecroswordEntity> type, Level level) {
        super(type, level);
    }

    /** A blade thrown by {@code owner} from {@code from} along {@code direction}, for {@code damage}. */
    public static NecroswordEntity throwFrom(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 direction, float damage) {
        NecroswordEntity blade = new NecroswordEntity(ModEntities.NECROSWORD.get(), level);
        blade.setOwner(owner);
        blade.damage = damage;
        blade.setPos(from);
        blade.shoot(direction.x, direction.y, direction.z, THROW_SPEED, 0.0F);
        return blade;
    }

    /** A blade of the rain: it hangs over {@code mark} for {@code hang} ticks, then drops onto it. */
    public static NecroswordEntity rainAt(ServerLevel level, LivingEntity owner, Vec3 mark, int hang, float damage) {
        NecroswordEntity blade = new NecroswordEntity(ModEntities.NECROSWORD.get(), level);
        blade.setOwner(owner);
        blade.damage = damage;
        blade.setPos(mark.x, mark.y + RAIN_HEIGHT, mark.z);
        blade.entityData.set(DATA_FALLING, true);
        blade.entityData.set(DATA_HANG, Math.max(0, hang));
        blade.setDeltaMovement(Vec3.ZERO);
        return blade;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HANG, 0);
        builder.define(DATA_FALLING, false);
    }

    public float damage() {
        return damage;
    }

    /** Ticks it still hangs (a blade of the rain), 0 once it moves. */
    public int hang() {
        return entityData.get(DATA_HANG);
    }

    public boolean falling() {
        return entityData.get(DATA_FALLING);
    }

    @Override
    public float shieldCost() {
        return damage;
    }

    @Override
    public void tick() {
        int hang = hang();
        if (hang > 0) {
            // Hanging point down, trembling a little (the client draws it): nothing moves and nothing hits yet.
            baseTick();
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide) {
                entityData.set(DATA_HANG, hang - 1);
                if (hang == 1) {
                    setDeltaMovement(0.0, -FALL_SPEED, 0.0);
                }
                if ((life += SlowMotion.timeScale(this)) > MAX_LIFE) {
                    discard();
                }
            }
            return;
        }
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(new EnergyParticleOptions(HelaRules.GREEN, 0.5F), getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (isRemoved()) {
            return;
        }
        double scale = SlowMotion.timeScale(this);
        if ((life += scale) > MAX_LIFE || !(getOwner() instanceof LivingEntity owner) || !owner.isAlive()) {
            shatter(false);
            return;
        }
        Vec3 motion = getDeltaMovement();
        float speed = falling() ? FALL_SPEED : THROW_SPEED;
        if (motion.lengthSqr() > 1.0E-8) {
            setDeltaMovement(motion.normalize().scale(speed * scale));
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (hang() > 0 || !super.canHitEntity(target) || !(target instanceof LivingEntity living)) {
            return false;
        }
        return getOwner() instanceof LivingEntity owner && HelaRules.canHurt(owner, living);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide || !(result.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        HelaRules.hurt(victim, ModDamageTypes.NECROSWORD, owner, this, damage);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.NECROSWORD_HIT.get(), SoundSource.HOSTILE, 0.9F, 0.9F + random.nextFloat() * 0.2F);
        shatter(true);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.NECROSWORD_HIT.get(), SoundSource.HOSTILE, 0.6F, 0.7F);
            shatter(true);
        }
    }

    /** It breaks into black shards and green motes. */
    public void shatter(boolean loud) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new EnergyParticleOptions(HelaRules.GREEN, 0.8F), getX(), getY(), getZ(), loud ? 8 : 4, 0.15, 0.15, 0.15, 0.05);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), loud ? 6 : 3, 0.1, 0.1, 0.1, 0.02);
        }
        discard();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }
}
