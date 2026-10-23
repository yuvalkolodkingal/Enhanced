package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/** The whole HUD is drawn (NeoForge's RenderGuiEvent). */
public abstract class RenderGuiEvent {
    private final GuiGraphics guiGraphics;
    private final DeltaTracker partialTick;

    protected RenderGuiEvent(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    /** Before the HUD is drawn; cancelling hides it. */
    public static class Pre extends RenderGuiEvent implements Cancellable {
        private boolean canceled;

        public Pre(GuiGraphics guiGraphics, DeltaTracker partialTick) {
            super(guiGraphics, partialTick);
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

    /** After the HUD was drawn. */
    public static class Post extends RenderGuiEvent {
        public Post(GuiGraphics guiGraphics, DeltaTracker partialTick) {
            super(guiGraphics, partialTick);
        }
    }
}
