package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;

/** An event about an entity (NeoForge's EntityEvent): getEntity() is narrowed by the subclasses. */
public abstract class EntityEvent {
    private final Entity entity;

    protected EntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
