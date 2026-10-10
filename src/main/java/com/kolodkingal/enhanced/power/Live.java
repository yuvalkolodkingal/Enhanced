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
    /** Billy's crown is out: while he flies, and for a moment after each spell. */
    CROWNED,
    /** Billy is casting a Hex Bolt (his hand thrust forward). */
    CASTING,
    /** Billy is holding the creatures in front of him spellbound (both hands raised). */
    FREEZING;

    final int bit() {
        return 1 << ordinal();
    }
}
