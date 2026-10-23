package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.LivingEntity;

/** A living entity is knocked back; cancelling stops it. */
public class LivingKnockBackEvent extends LivingEvent implements Cancellable {
    private float strength;
    private double ratioX;
    private double ratioZ;
    private boolean canceled;

    public LivingKnockBackEvent(LivingEntity entity, float strength, double ratioX, double ratioZ) {
        super(entity);
        this.strength = strength;
        this.ratioX = ratioX;
        this.ratioZ = ratioZ;
    }

    public float getStrength() {
        return strength;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public double getRatioX() {
        return ratioX;
    }

    public void setRatioX(double ratioX) {
        this.ratioX = ratioX;
    }

    public double getRatioZ() {
        return ratioZ;
    }

    public void setRatioZ(double ratioZ) {
        this.ratioZ = ratioZ;
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
