package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** An entity is about to join a level (spawned or loaded); cancelling keeps it out. */
public class EntityJoinLevelEvent extends EntityEvent implements Cancellable {
    private final Level level;
    private final boolean loadedFromDisk;
    private boolean canceled;

    public EntityJoinLevelEvent(Entity entity, Level level, boolean loadedFromDisk) {
        super(entity);
        this.level = level;
        this.loadedFromDisk = loadedFromDisk;
    }

    public Level getLevel() {
        return level;
    }

    public boolean loadedFromDisk() {
        return loadedFromDisk;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public EntityJoinLevelEvent(Entity entity, Level level) {
        this(entity, level, false);
    }
}
