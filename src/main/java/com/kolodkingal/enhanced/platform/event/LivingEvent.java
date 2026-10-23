package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.LivingEntity;

/** An event about a living entity (NeoForge's LivingEvent). */
public abstract class LivingEvent extends EntityEvent {
    protected LivingEvent(LivingEntity entity) {
        super(entity);
    }

    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) super.getEntity();
    }
}
