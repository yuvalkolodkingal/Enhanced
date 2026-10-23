package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** An entity is about to mount or dismount another; cancelling stops it. */
public class EntityMountEvent extends EntityEvent implements Cancellable {
    private final @Nullable Entity entityBeingMounted;
    private final Level level;
    private final boolean mounting;
    private boolean canceled;

    public EntityMountEvent(Entity entity, @Nullable Entity entityBeingMounted, Level level, boolean mounting) {
        super(entity);
        this.entityBeingMounted = entityBeingMounted;
        this.level = level;
        this.mounting = mounting;
    }

    @Nullable
    public Entity getEntityBeingMounted() {
        return entityBeingMounted;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isMounting() {
        return mounting;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /** The entity that mounts or dismounts (getEntity()). */
    public Entity getEntityMounting() {
        return getEntity();
    }

    public boolean isDismounting() {
        return !mounting;
    }
}
