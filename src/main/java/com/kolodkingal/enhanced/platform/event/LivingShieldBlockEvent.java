package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** A living entity's shield is about to block a blow (or not): setBlocked decides. */
public class LivingShieldBlockEvent extends LivingEvent implements Cancellable {
    private final DamageSource damageSource;
    private final boolean originalBlock;
    private boolean blocked;
    private boolean canceled;

    public LivingShieldBlockEvent(LivingEntity entity, DamageSource damageSource, boolean originalBlock) {
        super(entity);
        this.damageSource = damageSource;
        this.originalBlock = originalBlock;
        this.blocked = originalBlock;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public boolean getOriginalBlock() {
        return originalBlock;
    }

    public boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
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
