package com.kolodkingal.enhanced.platform.client;

/**
 * How high the vanilla HUD's status bars stand above the bottom of the screen this frame (port P0, PLAN2 3.3), so a
 * mod's HUD can sit above them: the left column (health, armour) and the right one (food, air, a mount's health).
 * Read it while drawing the HUD. Client only.
 */
public final class HudHeights {
    private HudHeights() {
    }

    /** The left column's height in GUI pixels (NeoForge: {@code Gui.leftHeight}). */
    public static int left() {
        return ClientServices.get().hudLeftHeight();
    }

    /** The right column's height in GUI pixels (NeoForge: {@code Gui.rightHeight}). */
    public static int right() {
        return ClientServices.get().hudRightHeight();
    }
}
