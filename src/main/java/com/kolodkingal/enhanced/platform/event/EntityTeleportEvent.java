package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** An entity is about to teleport (NeoForge's EntityTeleportEvent): change the target or cancel it. */
public abstract class EntityTeleportEvent extends EntityEvent implements Cancellable {
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean canceled;

    protected EntityTeleportEvent(Entity entity, double targetX, double targetY, double targetZ) {
        super(entity);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public double getTargetX() {
        return targetX;
    }

    public void setTargetX(double targetX) {
        this.targetX = targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    public void setTargetZ(double targetZ) {
        this.targetZ = targetZ;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public Vec3 getTarget() {
        return new Vec3(getTargetX(), getTargetY(), getTargetZ());
    }

    public Vec3 getPrev() {
        return getEntity().position();
    }

    /** A living entity is about to teleport by a chorus fruit; cancelling keeps it here. */
    public static class ChorusFruit extends EntityTeleportEvent {
        public ChorusFruit(LivingEntity entity, double targetX, double targetY, double targetZ) {
            super(entity, targetX, targetY, targetZ);
        }

        @Override
        public LivingEntity getEntity() {
            return (LivingEntity) super.getEntity();
        }

        public LivingEntity getEntityLiving() {
            return getEntity();
        }
    }

    /** An enderman or a shulker is about to teleport; cancelling keeps it here. */
    public static class EnderEntity extends EntityTeleportEvent {
        public EnderEntity(LivingEntity entity, double targetX, double targetY, double targetZ) {
            super(entity, targetX, targetY, targetZ);
        }

        @Override
        public LivingEntity getEntity() {
            return (LivingEntity) super.getEntity();
        }

        public LivingEntity getEntityLiving() {
            return getEntity();
        }
    }

    /** A player is about to teleport by an ender pearl; cancelling keeps him where he is. */
    public static class EnderPearl extends EntityTeleportEvent {
        private final ThrownEnderpearl pearlEntity;
        private float attackDamage;
        private final @Nullable HitResult hitResult;

        public EnderPearl(
                ServerPlayer entity, double targetX, double targetY, double targetZ, ThrownEnderpearl pearlEntity,
                float attackDamage, @Nullable HitResult hitResult) {
            super(entity, targetX, targetY, targetZ);
            this.pearlEntity = pearlEntity;
            this.attackDamage = attackDamage;
            this.hitResult = hitResult;
        }

        @Override
        public ServerPlayer getEntity() {
            return (ServerPlayer) super.getEntity();
        }

        public ThrownEnderpearl getPearlEntity() {
            return pearlEntity;
        }

        public float getAttackDamage() {
            return attackDamage;
        }

        public void setAttackDamage(float attackDamage) {
            this.attackDamage = attackDamage;
        }

        @Nullable
        public HitResult getHitResult() {
            return hitResult;
        }

        public ServerPlayer getPlayer() {
            return getEntity();
        }
    }
}
