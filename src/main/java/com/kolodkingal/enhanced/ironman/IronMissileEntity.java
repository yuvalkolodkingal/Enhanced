package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket or mini-missile of Iron Man's suits (3.8, PLAN 5.7, 6.2): the Mark 1's arm rocket, the Mark 3's shoulder
 * missiles, the Mark 85's homing ones, and, as a second entity type of the same class, Falcon's Wrist Rocket ({@code
 * falcon_rocket}). It flies with no gravity at its speed, asks {@link MissileGuidance#steer} for its motion every tick
 * (straight, but for lane L4's homing), and at the first creature or block: its {@link #damage()} to the creature it
 * struck and a small boom (to the others near, never breaking a block unless the server allows it). It pops in the air
 * after {@link #maxLife} ticks. Whom it hits is the heroes' rule ({@link AreaTargets#mayHurt}): it passes through his
 * pets, villagers and players when PvP is off.
 *
 * <p>A flare of the Mark 3 ({@link #decoy}) draws a missile that is not its own to it.
 */
public class IronMissileEntity extends Projectile {
    /** The kinds (synced, for the client's look). */
    public static final byte ARM_ROCKET = 0;
    public static final byte SHOULDER = 1;
    public static final byte HOMING = 2;
    public static final byte FALCON = 3;

    private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(IronMissileEntity.class,
            EntityDataSerializers.BYTE);

    private float damage = 3.0F;
    private float power = 0.6F;
    private int maxLife = 60;
    private int life;
    /** A flare drawing it (a point), or null. */
    @Nullable
    private Vec3 decoy;
    private ResourceKey<DamageType> damageType = ModDamageTypes.IRON_MISSILE;

    public IronMissileEntity(EntityType<? extends IronMissileEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /**
     * A missile of {@code kind} from {@code from} along {@code direction} at {@code speed} blocks a tick, fired by
     * {@code owner}, doing {@code damage} and a boom of {@code power}, popping after {@code range} blocks. Not added
     * to the level.
     */
    public static IronMissileEntity create(Level level, @Nullable Entity owner, byte kind, Vec3 from, Vec3 direction,
                                           double speed, float damage, float power, double range) {
        IronMissileEntity missile = new IronMissileEntity(kind == FALCON ? ModEntities.FALCON_ROCKET.get()
                : ModEntities.IRON_MISSILE.get(), level);
        missile.setOwner(owner);
        missile.setPos(from);
        missile.damage = damage;
        missile.power = power;
        missile.maxLife = Math.max(1, (int) Math.ceil(range / Math.max(0.1, speed)));
        missile.entityData.set(DATA_KIND, kind);
        missile.setDeltaMovement(direction.normalize().scale(speed));
        missile.updateRotation();
        return missile;
    }

    public byte kind() {
        return entityData.get(DATA_KIND);
    }

    public float damage() {
        return damage;
    }

    public float power() {
        return power;
    }

    /** Ticks it has flown. */
    public int life() {
        return life;
    }

    /** Ticks it may fly before it pops (lane L4's homing reads and sets it). */
    public int maxLife() {
        return maxLife;
    }

    public void setMaxLife(int ticks) {
        maxLife = Math.max(1, ticks);
    }

    /** Another damage type for its hit (Falcon's rocket's own). */
    public void setDamageType(ResourceKey<DamageType> type) {
        damageType = type;
    }

    /** A flare draws it: it flies to {@code at} (and pops there). */
    public void decoy(Vec3 at) {
        decoy = at;
    }

    @Nullable
    public Vec3 decoyPoint() {
        return decoy;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, SHOULDER);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide()) {
            if (decoy != null) {
                Vec3 to = decoy.subtract(position());
                if (to.lengthSqr() < 1.0) {
                    pop();
                    return;
                }
                motion = to.normalize().scale(Math.max(0.6, motion.length()));
            } else {
                motion = MissileGuidance.steer(this);
            }
            setDeltaMovement(motion);
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !level().isClientSide()) {
            hitTargetOrDeflectSelf(hit);
            if (isRemoved()) {
                return;
            }
        }
        setPos(position().add(motion));
        updateRotation();
        if (level().isClientSide()) {
            level().addParticle(kind() == FALCON ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                    getX() - motion.x * 0.5, getY() - motion.y * 0.5, getZ() - motion.z * 0.5, 0, 0, 0);
            level().addParticle(ParticleTypes.SMOKE, getX() - motion.x, getY() - motion.y, getZ() - motion.z, 0, 0.01,
                    0);
            return;
        }
        if (++life > maxLife) {
            pop();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (!super.canHitEntity(target) || target == owner || target instanceof IronMissileEntity) {
            return false;
        }
        if (target instanceof LivingEntity living && owner instanceof LivingEntity shooter) {
            return AreaTargets.mayHurt(shooter, living, false);
        }
        return target instanceof LivingEntity;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        LivingEntity struck = target instanceof LivingEntity living ? living : null;
        if (struck != null) {
            struck.invulnerableTime = 0;
            struck.hurt(ModDamageTypes.source(level(), damageType, getOwner(), this), damage);
        }
        explode(result.getLocation(), struck);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.BLOCK && !isRemoved()) {
            explode(result.getLocation(), null);
        }
    }

    /** It pops in the air (its time ran out, or it reached a flare). */
    private void pop() {
        explode(position(), null);
    }

    /** The small boom, then it is gone. */
    private void explode(Vec3 at, @Nullable LivingEntity spare) {
        if (level() instanceof ServerLevel server) {
            if (getOwner() instanceof ServerPlayer player) {
                IronManAbilities.boom(player, at, power, splashRadius(power), splashDamage(power), damageType, this,
                        spare);
            } else {
                server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
        discard();
    }

    /** The radius a boom of {@code power} reaches (power 0.8: 1.5 blocks). */
    public static double splashRadius(double power) {
        return 1.875 * power;
    }

    /** What a boom of {@code power} does to the others it reaches (power 0.8: 2). */
    public static double splashDamage(double power) {
        return 2.5 * power;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Kind", kind());
        tag.putFloat("Damage", damage);
        tag.putFloat("Power", power);
        tag.putInt("MaxLife", maxLife);
        tag.putInt("Life", life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_KIND, tag.getByte("Kind"));
        damage = tag.getFloat("Damage");
        power = tag.getFloat("Power");
        maxLife = Math.max(1, tag.getInt("MaxLife"));
        life = tag.getInt("Life");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
