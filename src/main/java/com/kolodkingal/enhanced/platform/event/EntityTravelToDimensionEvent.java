package com.kolodkingal.enhanced.platform.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** An entity is about to go to another dimension; cancelling keeps it here. */
public class EntityTravelToDimensionEvent extends EntityEvent implements Cancellable {
    private final ResourceKey<Level> dimension;
    private boolean canceled;

    public EntityTravelToDimensionEvent(Entity entity, ResourceKey<Level> dimension) {
        super(entity);
        this.dimension = dimension;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
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
