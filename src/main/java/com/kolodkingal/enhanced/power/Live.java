package com.kolodkingal.enhanced.power;

/**
 * Momentary states of the newer heroes that clients must see (to draw effects and poses) but that are never saved:
 * they are recomputed by the server while the power is in use. Kept together as one bit set in {@link PowerData}.
 */
public enum Live {
    /** Tommy is running at super speed right now. */
    SPEEDING,
    /** Tommy is spinning a whirlwind around himself. */
    WHIRLWIND,
    /** Thor holds Mjolnir up to the sky, calling lightning. */
    CALLING_LIGHTNING,
    /** The Bifrost's light is gathering around Thor, about to take him. */
    BIFROST,
    /** Loki is invisible while his duplicates draw every eye: his held items, armour and name tag are hidden too. */
    HIDDEN,
    /** Loki has just thrown a dagger (a brief throwing pose). */
    THROWING,
    /** Loki has just cast a spell with a raised, open hand (Vanish, Shapeshift). */
    CASTING;

    final int bit() {
        return 1 << ordinal();
    }
}
