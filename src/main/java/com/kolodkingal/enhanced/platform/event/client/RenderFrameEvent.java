package com.kolodkingal.enhanced.platform.event.client;

import net.minecraft.client.DeltaTracker;

/** A frame (NeoForge's RenderFrameEvent). */
public abstract class RenderFrameEvent {
    private final DeltaTracker partialTick;

    protected RenderFrameEvent(DeltaTracker partialTick) {
        this.partialTick = partialTick;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    /** Before a frame is drawn. */
    public static class Pre extends RenderFrameEvent {
        public Pre(DeltaTracker partialTick) {
            super(partialTick);
        }
    }

    /** After a frame was drawn. */
    public static class Post extends RenderFrameEvent {
        public Post(DeltaTracker partialTick) {
            super(partialTick);
        }
    }
}
