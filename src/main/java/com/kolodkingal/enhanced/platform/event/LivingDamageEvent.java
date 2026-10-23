package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** The damage a living entity takes (NeoForge's LivingDamageEvent). */
public abstract class LivingDamageEvent extends LivingEvent {
    protected LivingDamageEvent(LivingEntity entity) {
        super(entity);
    }

    /** The damage a living entity is about to take, after every reduction: change it with setNewDamage. */
    public static class Pre extends LivingDamageEvent {
        private final DamageSource source;
        private final float originalDamage;
        private float newDamage;

        public Pre(LivingEntity entity, DamageSource source, float originalDamage, float newDamage) {
            super(entity);
            this.source = source;
            this.originalDamage = originalDamage;
            this.newDamage = newDamage;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getOriginalDamage() {
            return originalDamage;
        }

        public float getNewDamage() {
            return newDamage;
        }

        public void setNewDamage(float newDamage) {
            this.newDamage = newDamage;
        }
    }

    /** A living entity was hurt: the damage it took (getNewDamage), after every reduction. */
    public static class Post extends LivingDamageEvent {
        private final DamageSource source;
        private final float originalDamage;
        private final float newDamage;
        private final float blockedDamage;
        private final float shieldDamage;

        public Post(
                LivingEntity entity, DamageSource source, float originalDamage, float newDamage, float blockedDamage,
                float shieldDamage) {
            super(entity);
            this.source = source;
            this.originalDamage = originalDamage;
            this.newDamage = newDamage;
            this.blockedDamage = blockedDamage;
            this.shieldDamage = shieldDamage;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getOriginalDamage() {
            return originalDamage;
        }

        public float getNewDamage() {
            return newDamage;
        }

        public float getBlockedDamage() {
            return blockedDamage;
        }

        public float getShieldDamage() {
            return shieldDamage;
        }
    }
}
