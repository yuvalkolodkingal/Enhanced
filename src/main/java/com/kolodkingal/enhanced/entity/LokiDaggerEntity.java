package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * One of Loki's conjured daggers: slim, fast and nearly flat in flight, trailing a faint shimmer of his magic. It
 * exists only while it flies: whatever it hits, it vanishes in a spray of green sparks, so it can never be picked up.
 */
public class LokiDaggerEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE = 60;
    /** Ticks lived, in its own (possibly slowed) time. */
    private double life;

    private float damage = 5.0F;
    /** 3.5: the colour a custom hero's pick chose for the throw (synced: the trail is drawn by every client), or -1. */
    private static final EntityDataAccessor<Integer> TINT = SynchedEntityData.defineId(LokiDaggerEntity.class, EntityDataSerializers.INT);

    public LokiDaggerEntity(EntityType<? extends LokiDaggerEntity> type, Level level) {
        super(type, level);
    }

    public LokiDaggerEntity(Level level, LivingEntity thrower, float damage) {
        super(ModEntities.LOKI_DAGGER.get(), thrower, level);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TINT, -1);
    }

    /** 3.5: the throw in a chosen colour (-1: the thrower's magic). */
    public void tint(int color) {
        entityData.set(TINT, color);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LOKI_DAGGER.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.012;
    }

    /** The thrower's magic colour (his current look's), for the trail and the sparks. */
    public int magicColor() {
        int tint = entityData.get(TINT);
        return tint >= 0 ? tint : getOwner() instanceof Player owner ? Powers.color(owner) : Power.LOKI.color;
    }

    private int magicGlow() {
        int tint = entityData.get(TINT);
        return tint >= 0 ? AbilityColors.glowOf(tint) : getOwner() instanceof Player owner ? Powers.glow(owner) : Power.LOKI.glow;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount > 1) {
                level().addParticle(new EnergyParticleOptions(random.nextInt(3) == 0 ? magicGlow() : magicColor(), 0.55F),
                        xo + (random.nextDouble() - 0.5) * 0.08, yo + 0.12 + (random.nextDouble() - 0.5) * 0.08,
                        zo + (random.nextDouble() - 0.5) * 0.08, 0, 0, 0);
            }
        } else if ((life += SlowMotion.timeScale(this)) > MAX_LIFE) {
            // Counted in the dagger's own time, so Quicksilver Time doesn't cut its flight short.
            vanish();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // A dagger passes through its own thrower's illusions.
        return super.canHitEntity(target)
                && !(target instanceof LokiIllusionEntity illusion && getOwner() != null && getOwner().getUUID().equals(illusion.ownerId()));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            result.getEntity().hurt(ModDamageTypes.source(level(), ModDamageTypes.LOKI_DAGGER, getOwner(), this), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            vanish();
        }
    }

    /** The dagger was never real: it breaks up into green sparks. */
    private void vanish() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new EnergyParticleOptions(magicColor(), 1.0F), getX(), getY() + 0.1, getZ(), 14, 0.08, 0.08, 0.08, 0.12);
            server.sendParticles(new EnergyParticleOptions(magicGlow(), 0.8F), getX(), getY() + 0.1, getZ(), 6, 0.05, 0.05, 0.05, 0.06);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.LOKI_DISSOLVE.get(), SoundSource.PLAYERS, 0.45F, 1.6F);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            damage = tag.getFloat("Damage");
        }
    }
}
