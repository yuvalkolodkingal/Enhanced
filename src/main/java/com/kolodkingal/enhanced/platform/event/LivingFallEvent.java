package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.LivingEntity;

/** A living entity lands: change the distance or the damage multiplier, or cancel the fall. */
public class LivingFallEvent extends LivingEvent implements Cancellable {
    private float distance;
    private float damageMultiplier;
    private boolean canceled;

    public LivingFallEvent(LivingEntity entity, float distance, float damageMultiplier) {
        super(entity);
        this.distance = distance;
        this.damageMultiplier = damageMultiplier;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
