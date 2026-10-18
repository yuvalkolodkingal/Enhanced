package com.kolodkingal.enhanced.hex;

/** Changes Wanda can make from the Hex Control screen, and the orders she gives the Vision her Hex raised. */
public enum HexAction {
    /** Allow (value 1) or block (value 0) a player. */
    SET_PLAYER,
    /** Allow (1) or block (0) mobs and animals. */
    SET_CREATURES,
    /** Set the radius to value. */
    SET_RADIUS,
    DISSOLVE,
    /** Vision holds his post (1) or follows Wanda (0). */
    SET_VISION_STAYS,
    /** "Vision, that one!": he attacks the creature Wanda looks at (or, with none in view, comes back to her side). */
    VISION_TARGET,
    /** 3.3: inscribe (1) or wipe (0) the Hex Runes on the four walls. */
    SET_RUNES
}
