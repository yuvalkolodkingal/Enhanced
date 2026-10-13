package com.kolodkingal.enhanced.build;

import java.util.Locale;

/**
 * What the {@link Spell#GROW} brush does inside its sphere, a pulse every 4 ticks while the right button is held. Each
 * cell's choice is a hash of the stroke's seed and its position, so the server grows what the client showed. The ordinal
 * travels in a recipe's {@code style}: append only.
 */
public enum GrowStyle {
    /** Air next to a solid block becomes that block: a rock grows into a boulder. */
    SWELL,
    /** A solid block with air on two or more faces vanishes. */
    SHRINK,
    /** Westview's garden: dirt becomes grass, grass sprouts flowers, netherrack becomes nylium. */
    BLOOM;

    /** The style with this id; any number is a style. */
    public static GrowStyle byId(int id) {
        GrowStyle[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** The lang key of its name. */
    public String key() {
        return "screen.enhanced.build.style.grow." + name().toLowerCase(Locale.ROOT);
    }
}
