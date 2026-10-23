package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/** A screen (NeoForge's ScreenEvent). */
public abstract class ScreenEvent {
    private final @Nullable Screen screen;

    protected ScreenEvent(@Nullable Screen screen) {
        this.screen = screen;
    }

    @Nullable
    public Screen getScreen() {
        return screen;
    }

    /** A screen is about to open: open another (setNewScreen) or none (cancel). */
    public static class Opening extends ScreenEvent implements Cancellable {
        private final @Nullable Screen currentScreen;
        private @Nullable Screen newScreen;
        private boolean canceled;

        public Opening(@Nullable Screen currentScreen, @Nullable Screen screen) {
            super(screen);
            this.currentScreen = currentScreen;
            this.newScreen = screen;
        }

        @Nullable
        public Screen getCurrentScreen() {
            return currentScreen;
        }

        @Nullable
        public Screen getNewScreen() {
            return newScreen;
        }

        public void setNewScreen(@Nullable Screen newScreen) {
            this.newScreen = newScreen;
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
