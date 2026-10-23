package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StonePowers;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The boulder the Hulk throws (3.6, B): it looks like the block he stood on (no block is taken: the ground stays as it
 * was) and tumbles through the air. The creature it hits takes its damage ({@code boulderDamage}), everyone else within
 * 2 blocks of where it strikes a splash ({@code boulderSplash}), and it shatters into that block's dust; with {@code
 * [hulk] blockBreaking all} it leaves a crater of 1. Drawn by {@code HulkBoulderRenderer} (the block's model, turning).
 */
public class HulkBoulderEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<BlockState> BLOCK = SynchedEntityData.defineId(HulkBoulderEntity.class,
            EntityDataSerializers.BLOCK_STATE);
    /** The splash's radius. */
    public static final double SPLASH = 2.0;
    /** It shatters by itself after this long (lost in the sky, in water). */
    public static final int LIFETIME = 200;

    private float damage = 8.0F;
    private float splash = 4.0F;
    private int struck = -1;

    public HulkBoulderEntity(EntityType<? extends HulkBoulderEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK, Blocks.STONE.defaultBlockState());
    }

    /** The block it looks like. */
    public BlockState block() {
        return entityData.get(BLOCK);
    }

    public void setBlock(BlockState state) {
        entityData.set(BLOCK, state);
    }

    /** Its damages (already his look's and his rage's). */
    public void setDamage(float direct, float splash) {
        this.damage = direct;
        this.splash = splash;
    }

    public float damage() {
        return damage;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) {
            shatter(position());
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && !(target instanceof HulkBoulderEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel level && result.getEntity() instanceof LivingEntity target
                && getOwner() instanceof LivingEntity owner && AreaTargets.mayHurt(owner, target, true)) {
            struck = target.getId();
            if (StonePowers.hurt(level, owner, target, ModDamageTypes.HULK_BOULDER, damage, this)) {
                HulkAbilities.push(target, position().subtract(getDeltaMovement()), 1.0, 0.3, owner);
                if (owner instanceof ServerPlayer hulk) {
                    HulkRage.landed(hulk, com.kolodkingal.enhanced.power.Powers.get(hulk));
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && isAlive()) {
            Vec3 at = result.getLocation();
            if (result instanceof EntityHitResult hit) {
                at = hit.getEntity().position().add(0, hit.getEntity().getBbHeight() * 0.5, 0);
            }
            shatter(at);
        }
    }

    /** It strikes: the splash round {@code at}, the dust, the sound, the crater; and it is gone. */
    void shatter(Vec3 at) {
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        if (getOwner() instanceof LivingEntity owner) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SPLASH + 1.0),
                    e -> e != owner && e.getId() != struck && e.isAlive() && !e.isSpectator()
                            && AreaTargets.mayHurt(owner, e, false))) {
                if (e.getBoundingBox().inflate(SPLASH).contains(at)) {
                    StonePowers.hurt(level, owner, e, ModDamageTypes.HULK_BOULDER, splash, this);
                    HulkAbilities.push(e, at, 0.6, 0.25, owner);
                }
            }
            if (owner instanceof ServerPlayer hulk) {
                HulkTerrain.boulder(hulk, at);
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block()), at.x, at.y, at.z, 40, 0.5, 0.5, 0.5,
                0.15);
        level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_HIT.get(), getSoundSource(), 1.4F, 0.9F
                + random.nextFloat() * 0.2F);
        Net.sendToPlayersNear(level, null, at.x, at.y, at.z, HulkAbilities.FX_RANGE,
                new HulkFxPayload(HulkFxPayload.BOULDER_HIT, getOwner() == null ? -1 : getOwner().getId(), List.of(at),
                        Block.getId(block())));
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("block", NbtUtils.writeBlockState(block()));
        tag.putFloat("damage", damage);
        tag.putFloat("splash", splash);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("block")) {
            setBlock(NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("block")));
        }
        damage = tag.getFloat("damage");
        splash = tag.getFloat("splash");
    }
}
