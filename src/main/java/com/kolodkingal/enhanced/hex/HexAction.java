package com.kolodkingal.enhanced.hex;

/** Changes Wanda can make from the Hex Control screen. */
public enum HexAction {
    /** Allow (value 1) or block (value 0) a player. */
    SET_PLAYER,
    /** Allow (1) or block (0) mobs and animals. */
    SET_CREATURES,
    /** Set the radius to value. */
    SET_RADIUS,
    DISSOLVE
}
