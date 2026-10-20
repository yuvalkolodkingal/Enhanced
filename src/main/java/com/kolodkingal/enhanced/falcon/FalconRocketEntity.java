package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ironman.IronMissileEntity;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Falcon's Wrist Rocket (3.8, PLAN 8.4): the entity type {@code falcon_rocket}, of the class of Iron Man's missiles
 * ({@link IronMissileEntity}, the skeleton's choice: one class, two types). It flies a rocket's straight line of its
 * own ({@code WristWeapons.ROCKET_SPEED} a tick, no gravity, {@link #MAX_LIFE} ticks), whatever the suits' missiles do:
 * it hits the first creature or block on its way for {@code rocketDamage} 8 ({@code falcon_dart}: the blasters' row),
 * then a small boom of {@code rocketPower} 1.0 that never breaks a block and spares him, his Redwing and whoever the
 * heroes' rule spares ({@link AreaTargets#mayHurt}). Drawn by {@code client/falcon/FalconRocketRenderer}.
 */
public class FalconRocketEntity extends IronMissileEntity {
    /** How long it flies before it pops in the air. */
    public static final int MAX_LIFE = 60;

    private float damage = 8.0F;
    private float power = 1.0F;
    private int life;

    public FalconRocketEntity(EntityType<? extends IronMissileEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /** A rocket from {@code from} along {@code direction} at {@code speed}, fired by {@code owner}; not added yet. */
    public static FalconRocketEntity create(Level level, @Nullable Entity owner, Vec3 from, Vec3 direction,
                                            double speed, float damage, float power) {
        FalconRocketEntity rocket = new FalconRocketEntity(ModEntities.FALCON_ROCKET.get(), level);
        rocket.setOwner(owner);
        rocket.setPos(from);
        rocket.damage = damage;
        rocket.power = power;
        rocket.setDeltaMovement(direction.normalize().scale(speed));
        rocket.updateRotation();
        return rocket;
    }

    @Override
    public void tick() {
        // (Its own flight: the base tick of every entity, then a straight line; the suits' missiles fly as their lanes
        // say, and this one never steers.)
        baseTick();
        Vec3 motion = getDeltaMovement();
        if (level().isClientSide) {
            Vec3 at = position();
            level().addParticle(ParticleTypes.SMOKE, at.x - motion.x * 0.5, at.y - motion.y * 0.5,
                    at.z - motion.z * 0.5, 0, 0.01, 0);
            level().addParticle(ParticleTypes.SMALL_FLAME, at.x - motion.x * 0.3, at.y - motion.y * 0.3,
                    at.z - motion.z * 0.3, 0, 0, 0);
            setPos(at.add(motion));
            return;
        }
        if (isRemoved()) {
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation());
            if (hit instanceof EntityHitResult entityHit) {
                strike(entityHit.getEntity());
            }
            boom();
            return;
        }
        setPos(position().add(motion));
        updateRotation();
        if (++life > MAX_LIFE) {
            boom();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (target == owner || !target.isAlive() || target.isSpectator() || !target.isPickable()
                || target instanceof IronMissileEntity) {
            return false;
        }
        if (!(target instanceof LivingEntity living) || target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof RedwingEntity drone && owner != null && owner.getUUID().equals(drone.ownerId())) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return !(owner instanceof LivingEntity shooter) || AreaTargets.mayHurt(shooter, living, false);
    }

    /** The rocket's own blow to what it hit. */
    private void strike(Entity target) {
        if (!(target instanceof LivingEntity living) || !(level() instanceof ServerLevel server)) {
            return;
        }
        if (getOwner() instanceof ServerPlayer shooter) {
            FalconAbilities.hurt(shooter, living, ModDamageTypes.FALCON_DART, damage, this);
        } else {
            living.invulnerableTime = 0;
            living.hurt(ModDamageTypes.source(server, ModDamageTypes.FALCON_DART, getOwner(), this), damage);
        }
    }

    /** The small boom: no block is broken, and the heroes' rule decides who it hurts. */
    private void boom() {
        if (level() instanceof ServerLevel server && power > 0.0F) {
            Entity owner = getOwner();
            ExplosionDamageCalculator spare = new ExplosionDamageCalculator() {
                @Override
                public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                    if (entity == owner || entity instanceof RedwingEntity) {
                        return false;
                    }
                    if (owner instanceof LivingEntity shooter && entity instanceof LivingEntity living) {
                        return AreaTargets.mayHurt(shooter, living, false);
                    }
                    return !(entity instanceof LivingEntity);
                }
            };
            server.explode(this, server.damageSources().explosion(this, owner), spare, getX(), getY(), getZ(), power,
                    false, Level.ExplosionInteraction.NONE);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putFloat("power", power);
        tag.putInt("life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        power = tag.getFloat("power");
        life = tag.getInt("life");
    }
}
