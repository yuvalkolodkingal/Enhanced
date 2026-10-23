package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** An entity left a level (removed or unloaded). */
public class EntityLeaveLevelEvent extends EntityEvent {
    private final Level level;

    public EntityLeaveLevelEvent(Entity entity, Level level) {
        super(entity);
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }
}
