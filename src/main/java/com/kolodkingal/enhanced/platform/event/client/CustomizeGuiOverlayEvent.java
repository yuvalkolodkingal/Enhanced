package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;

/** Parts of the HUD that can be changed (NeoForge's CustomizeGuiOverlayEvent). */
public abstract class CustomizeGuiOverlayEvent {
    private final Window window;
    private final GuiGraphics guiGraphics;
    private final DeltaTracker partialTick;

    protected CustomizeGuiOverlayEvent(Window window, GuiGraphics guiGraphics, DeltaTracker partialTick) {
        this.window = window;
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
    }

    public Window getWindow() {
        return window;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    /** A boss bar is about to be drawn: move the next one (setIncrement) or hide it (cancel). */
    public static class BossEventProgress extends CustomizeGuiOverlayEvent implements Cancellable {
        private final LerpingBossEvent bossEvent;
        private final int x;
        private final int y;
        private int increment;
        private boolean canceled;

        public BossEventProgress(
                Window window, GuiGraphics guiGraphics, DeltaTracker partialTick, LerpingBossEvent bossEvent, int x,
                int y, int increment) {
            super(window, guiGraphics, partialTick);
            this.bossEvent = bossEvent;
            this.x = x;
            this.y = y;
            this.increment = increment;
        }

        public LerpingBossEvent getBossEvent() {
            return bossEvent;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getIncrement() {
            return increment;
        }

        public void setIncrement(int increment) {
            this.increment = increment;
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
}
