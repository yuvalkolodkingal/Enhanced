package com.kolodkingal.enhanced.build;

import java.util.Locale;

/**
 * Which blocks of a box {@link Spell#CHANGE} turns into the material. Change keeps each block's shape (oak stairs become
 * spruce stairs), never turns a block into air and never fills air. The ordinal travels in a recipe's {@code style}:
 * append only.
 */
public enum ChangeStyle {
    /** Every block of the kind looked at, inside the box. The default. */
    KIND,
    /** Every block that is not air. */
    ALL,
    /** Every block that touches air on at least one face: the skin of a hill or a house. */
    OUTSIDE;

    /** The style with this id; any number is a style. */
    public static ChangeStyle byId(int id) {
        ChangeStyle[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** The lang key of its name. */
    public String key() {
        return "screen.enhanced.build.style.change." + name().toLowerCase(Locale.ROOT);
    }
}
