package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/** A layer of the HUD is drawn (NeoForge's RenderGuiLayerEvent). */
public abstract class RenderGuiLayerEvent {
    private final GuiGraphics guiGraphics;
    private final DeltaTracker partialTick;
    private final ResourceLocation name;
    private final LayeredDraw.Layer layer;

    protected RenderGuiLayerEvent(
            GuiGraphics guiGraphics, DeltaTracker partialTick, ResourceLocation name, LayeredDraw.Layer layer) {
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
        this.name = name;
        this.layer = layer;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    public ResourceLocation getName() {
        return name;
    }

    public LayeredDraw.Layer getLayer() {
        return layer;
    }

    /** Before a HUD layer is drawn; cancelling hides it. */
    public static class Pre extends RenderGuiLayerEvent implements Cancellable {
        private boolean canceled;

        public Pre(GuiGraphics guiGraphics, DeltaTracker partialTick, ResourceLocation name, LayeredDraw.Layer layer) {
            super(guiGraphics, partialTick, name, layer);
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

    /** After a HUD layer was drawn. */
    public static class Post extends RenderGuiLayerEvent {
        public Post(GuiGraphics guiGraphics, DeltaTracker partialTick, ResourceLocation name, LayeredDraw.Layer layer) {
            super(guiGraphics, partialTick, name, layer);
        }
    }
}
