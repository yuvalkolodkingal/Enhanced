package com.kolodkingal.enhanced.strange;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Doctor Strange's body while his spirit is out (3.6, the Astral Projection): a copy of his look, kneeling, eyes closed,
 * with 20 health of its own (the player's real health is not touched). It does not move, nothing pushes it, it never
 * suffocates nor drowns. Any harm to it calls him back ({@link AstralProjection#bodyHurt}): he is set back where it
 * kneels and takes half the harm. Never saved: a body whose spirit is not out any more (a restart, a lost owner) goes.
 * Drawn by {@code AstralBodyRenderer} with its owner's skin.
 */
public class AstralBodyEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(AstralBodyEntity.class,
            EntityDataSerializers.OPTIONAL_UUID);

    public AstralBodyEntity(EntityType<? extends AstralBodyEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    /** Whose body this is (his spirit is out), or null. */
    @Nullable
    public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    public void setOwner(UUID owner) {
        entityData.set(OWNER, Optional.of(owner));
    }

    /** Its owner, if he is in this level. */
    @Nullable
    public Player ownerHere() {
        UUID id = owner();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    @Override
    protected void registerGoals() {
        // (It kneels where he stood: no goals.)
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        if (level() instanceof ServerLevel && tickCount % 10 == 0 && !AstralProjection.isBody(this)) {
            discard(); // (its spirit is not out any more: a body left after a restart, or a lost owner's)
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F) {
            return false;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!AstralProjection.isBody(this)) {
            return super.hurt(source, amount);
        }
        // The body calls him back: he returns to it and takes half the harm; the body itself is not hurt.
        AstralProjection.bodyHurt(this, source, amount);
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL)
                || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}
