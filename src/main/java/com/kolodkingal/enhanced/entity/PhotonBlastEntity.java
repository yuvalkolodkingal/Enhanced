package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Photon's bolt of pure light. Flies straight, bursts on impact without breaking blocks. */
public class PhotonBlastEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> DATA_SUPERCHARGED =
            SynchedEntityData.defineId(PhotonBlastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_LIFE = 60;

    private float damage = 6.0F;

    public PhotonBlastEntity(EntityType<? extends PhotonBlastEntity> type, Level level) {
        super(type, level);
    }

    public PhotonBlastEntity(Level level, LivingEntity shooter, float damage, boolean supercharged) {
        super(ModEntities.PHOTON_BLAST.get(), shooter, level);
        this.damage = damage;
        entityData.set(DATA_SUPERCHARGED, supercharged);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SUPERCHARGED, false);
    }

    public boolean isSupercharged() {
        return entityData.get(DATA_SUPERCHARGED);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PHOTON_ORB.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            int color = isSupercharged() ? Power.PHOTON.glow : Power.PHOTON.color;
            for (int i = 0; i < 2; i++) {
                level().addParticle(new EnergyParticleOptions(color, isSupercharged() ? 1.6F : 1.1F),
                        getX() + (random.nextDouble() - 0.5) * 0.2, getY() + 0.15 + (random.nextDouble() - 0.5) * 0.2,
                        getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
            }
        } else if (tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            Entity target = result.getEntity();
            target.hurt(ModDamageTypes.source(level(), ModDamageTypes.PHOTON_BLAST, getOwner(), this), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            int color = isSupercharged() ? Power.PHOTON.glow : Power.PHOTON.color;
            server.sendParticles(new EnergyParticleOptions(color, 1.8F), getX(), getY(), getZ(), 24, 0.2, 0.2, 0.2, 0.15);
            server.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.6F);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("Supercharged", isSupercharged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        entityData.set(DATA_SUPERCHARGED, tag.getBoolean("Supercharged"));
    }
}
