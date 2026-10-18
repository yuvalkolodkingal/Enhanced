package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * One shard of Magneto's Iron Shrapnel Storm: very fast, almost no drop, pierces through several creatures and
 * ignores the usual hit cooldown so a stream of shards all land.
 */
public class IronShrapnelEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE = 40;
    /** Ticks lived, in its own (possibly slowed) time. */
    private double life;

    private float damage = 4.0F;
    private int pierce = 3;
    private final IntSet hit = new IntOpenHashSet();

    public IronShrapnelEntity(EntityType<? extends IronShrapnelEntity> type, Level level) {
        super(type, level);
    }

    public IronShrapnelEntity(Level level, LivingEntity shooter) {
        super(ModEntities.IRON_SHRAPNEL.get(), shooter, level);
    }

    public void configure(float damage, int pierce) {
        this.damage = damage;
        this.pierce = pierce;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.IRON_SHRAPNEL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.005;
    }

    @Override
    public void tick() {
        super.tick();
        // 3.5: a custom hero's shards trail the colour his pick of the Shrapnel chose.
        if (level().isClientSide) {
            int tint = AbilityColors.chosen(getOwner(), HeroAbility.MAGNETO_SHRAPNEL);
            if (tint >= 0) {
                level().addParticle(new EnergyParticleOptions(tint, 0.5F), getX(), getY(), getZ(), 0, 0, 0);
            }
        }
        // Counted in the shard's own time, so Quicksilver Time doesn't cut its flight short.
        if (!level().isClientSide && (life += SlowMotion.timeScale(this)) > MAX_LIFE) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !hit.contains(target.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        hit.add(target.getId());
        // Ignore the hit cooldown: a storm of shards should all land.
        target.invulnerableTime = 0;
        target.hurt(ModDamageTypes.source(level(), ModDamageTypes.SHRAPNEL, getOwner(), this), damage);
        if (hit.size() >= pierce) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET)),
                    getX(), getY(), getZ(), 4, 0.05, 0.05, 0.05, 0.08);
            server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.2);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.4F, 1.8F);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Pierce", pierce);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            damage = tag.getFloat("Damage");
            pierce = tag.getInt("Pierce");
        }
    }
}
