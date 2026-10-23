package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** A living entity is about to die; cancelling keeps it alive (its health is the canceller's business). */
public class LivingDeathEvent extends LivingEvent implements Cancellable {
    private final DamageSource source;
    private boolean canceled;

    public LivingDeathEvent(LivingEntity entity, DamageSource source) {
        super(entity);
        this.source = source;
    }

    public DamageSource getSource() {
        return source;
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
