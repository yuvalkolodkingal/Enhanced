package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;

/** An entity's tick (NeoForge's EntityTickEvent). */
public abstract class EntityTickEvent extends EntityEvent {
    protected EntityTickEvent(Entity entity) {
        super(entity);
    }

    /** Before an entity's tick; cancelling skips the tick. */
    public static class Pre extends EntityTickEvent implements Cancellable {
        private boolean canceled;

        public Pre(Entity entity) {
            super(entity);
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

    /** After an entity's tick. */
    public static class Post extends EntityTickEvent {
        public Post(Entity entity) {
            super(entity);
        }
    }
}
