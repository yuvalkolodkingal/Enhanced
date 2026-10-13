package com.kolodkingal.enhanced.build;

import java.util.Locale;

/**
 * How {@link Spell#FILL} fills a box. A selected thing (a magic selection) is filled whole, and the styles grey out. The
 * ordinal travels in a recipe's {@code style}: append only.
 */
public enum FillStyle {
    /** Every cell (5 x 4 x 5: 100). The default. */
    SOLID,
    /** The four sides, full height, with no floor or roof (5 x 4 x 5: 64). */
    WALLS,
    /** Walls, floor and roof, with the inside emptied to air: the instant house (5 x 4 x 5: 82 placed, 18 emptied). */
    ROOM,
    /** The bottom layer (5 x 4 x 5: 25). */
    FLOOR,
    /** The twelve edges (5 x 4 x 5: 40). */
    FRAME,
    /** Only the cells that are air or replaceable (grass, flowers, snow layers, water), keeping what is there. */
    EMPTY;

    /** The style with this id; any number is a style. */
    public static FillStyle byId(int id) {
        FillStyle[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** The lang key of its name. */
    public String key() {
        return "screen.enhanced.build.style.fill." + name().toLowerCase(Locale.ROOT);
    }
}
