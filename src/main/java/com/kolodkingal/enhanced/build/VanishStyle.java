package com.kolodkingal.enhanced.build;

import java.util.Locale;

/**
 * What {@link Spell#VANISH} makes disappear in a box. Blocks vanish without drops, and a waterlogged block leaves its water
 * behind. The ordinal travels in a recipe's {@code style}: append only.
 */
public enum VanishStyle {
    /** Every block in the box becomes air, water and lava too. The default, and what Delete does. */
    ALL,
    /** Every cell that is not on the box's outer layer: a room carved inside a hill. */
    HOLLOW,
    /** Only the blocks of {@code enhanced:build_plants} (logs, leaves, flowers, grass...): clear the land for a house. */
    PLANTS,
    /** Only the kind of block looked at, inside the box (every leaf, every cobweb). */
    KIND;

    /** The style with this id; any number is a style. */
    public static VanishStyle byId(int id) {
        VanishStyle[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** The lang key of its name. */
    public String key() {
        return "screen.enhanced.build.style.vanish." + name().toLowerCase(Locale.ROOT);
    }
}
