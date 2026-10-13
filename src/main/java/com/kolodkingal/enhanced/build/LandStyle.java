package com.kolodkingal.enhanced.build;

import java.util.Locale;

/**
 * What the {@link Spell#LAND} brush does to the ground under it, a pulse every 4 ticks while the right button is held.
 * Land never moves logs, leaves, containers or protected cells. The ordinal travels in a recipe's {@code style}: append
 * only.
 */
public enum LandStyle {
    /** The surface rises (most at the brush's middle): a hill grows. */
    RAISE,
    /** The surface sinks: a valley is dug. */
    LOWER,
    /** Each column moves toward the average height of its neighbours. */
    SMOOTH;

    /** The style with this id; any number is a style. */
    public static LandStyle byId(int id) {
        LandStyle[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** The lang key of its name. */
    public String key() {
        return "screen.enhanced.build.style.land." + name().toLowerCase(Locale.ROOT);
    }
}
