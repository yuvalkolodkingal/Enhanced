package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** A mob is about to take a new target; cancelling keeps the old one. */
public class LivingChangeTargetEvent extends LivingEvent implements Cancellable {
    private final @Nullable LivingEntity originalAboutToBeSetTarget;
    private @Nullable LivingEntity newAboutToBeSetTarget;
    private boolean canceled;

    public LivingChangeTargetEvent(LivingEntity entity, @Nullable LivingEntity originalAboutToBeSetTarget) {
        super(entity);
        this.originalAboutToBeSetTarget = originalAboutToBeSetTarget;
        this.newAboutToBeSetTarget = originalAboutToBeSetTarget;
    }

    @Nullable
    public LivingEntity getOriginalAboutToBeSetTarget() {
        return originalAboutToBeSetTarget;
    }

    @Nullable
    public LivingEntity getNewAboutToBeSetTarget() {
        return newAboutToBeSetTarget;
    }

    public void setNewAboutToBeSetTarget(@Nullable LivingEntity newAboutToBeSetTarget) {
        this.newAboutToBeSetTarget = newAboutToBeSetTarget;
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
