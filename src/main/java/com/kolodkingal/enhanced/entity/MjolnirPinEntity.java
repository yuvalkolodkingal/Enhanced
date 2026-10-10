package com.kolodkingal.enhanced.entity;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Mjolnir set down on someone: it lies across the creature or player it pins (The Avengers: Thor sets his hammer on
 * Loki's chest, and Loki can't get up). It carries the hammer, follows its target and lets {@link MjolnirPin} hold the
 * target in place for as long as it lasts; whoever it pins can look around, but not move.
 * <p>
 * The pin does not outlast the world it was made in: if it is loaded from disk (the world was closed or crashed with it
 * in place) it lets go on its first tick, and the hammer falls to the ground, its owner kept.
 */
public class MjolnirPinEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_HAMMER =
            SynchedEntityData.defineId(MjolnirPinEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_TARGET =
            SynchedEntityData.defineId(MjolnirPinEntity.class, EntityDataSerializers.INT);

    private UUID ownerId = new UUID(0L, 0L);
    private UUID targetUuid = new UUID(0L, 0L);
    /** The game time at which it lets go. */
    private long endsAt;
    private boolean loadedFromDisk;
    /** Where the target was held, and the lowest it has been since (it may fall, but never climb). */
    private Vec3 holdAt = Vec3.ZERO;

    public MjolnirPinEntity(EntityType<? extends MjolnirPinEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    /** Mjolnir {@code hammer}, set down by {@code ownerId} on {@code target}, for {@code ticks} ticks. */
    public MjolnirPinEntity(ServerLevel level, UUID ownerId, LivingEntity target, ItemStack hammer, int ticks) {
        this(ModEntities.MJOLNIR_PIN.get(), level);
        this.ownerId = ownerId;
        this.targetUuid = target.getUUID();
        this.endsAt = level.getGameTime() + ticks;
        this.holdAt = target.position();
        entityData.set(DATA_HAMMER, hammer.copyWithCount(1));
        entityData.set(DATA_TARGET, target.getId());
        setPos(centre(target));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HAMMER, ItemStack.EMPTY);
        builder.define(DATA_TARGET, -1);
    }

    /** The hammer lying on the target. */
    public ItemStack hammer() {
        ItemStack stack = entityData.get(DATA_HAMMER);
        return stack.isEmpty() ? new ItemStack(ModItems.MJOLNIR.get()) : stack.copy();
    }

    /** The entity id of whoever is pinned (the client finds them with it), or -1. */
    public int targetEntityId() {
        return entityData.get(DATA_TARGET);
    }

    public UUID ownerId() {
        return ownerId;
    }

    public UUID targetUuid() {
        return targetUuid;
    }

    /** The point the pinned one is held at (X and Z), and the lowest they have been (Y). */
    public Vec3 holdAt() {
        return holdAt;
    }

    public void setHoldAt(Vec3 holdAt) {
        this.holdAt = holdAt;
    }

    /** The hammer's resting place on a target: across the chest. */
    private static Vec3 centre(Entity target) {
        return target.position().add(0, target.getBbHeight() * 0.6, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // A crackle of lightning round the hammer, now and then.
            if (random.nextInt(8) == 0 && level().getEntity(targetEntityId()) != null) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 0.8, getY() + (random.nextDouble() - 0.5) * 0.4,
                        getZ() + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (loadedFromDisk) {
            MjolnirPin.release(level, this);
            return;
        }
        LivingEntity target = MjolnirPin.targetOf(level, this);
        if (target == null) {
            MjolnirPin.release(level, this);
            return;
        }
        setPos(centre(target));
        MjolnirPin.tick(level, this, target);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("Owner", ownerId);
        tag.putUUID("Target", targetUuid);
        tag.put("Hammer", hammer().save(registryAccess()));
        tag.putLong("EndsAt", endsAt);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            targetUuid = tag.getUUID("Target");
        }
        entityData.set(DATA_HAMMER, ItemStack.parseOptional(registryAccess(), tag.getCompound("Hammer")));
        endsAt = tag.getLong("EndsAt");
        loadedFromDisk = true;
    }

    /** The game time at which it lets go. */
    public long endsAt() {
        return endsAt;
    }
}
