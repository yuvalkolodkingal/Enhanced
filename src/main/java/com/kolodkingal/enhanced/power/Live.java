package com.kolodkingal.enhanced.power;

/**
 * Momentary states of the newer heroes that clients must see (to draw effects and poses) but that are never saved:
 * they are recomputed by the server while the power is in use. Kept together as one bit set in {@link PowerData}.
 */
public enum Live {
    /** Tommy, or Quicksilver in Quicksilver Time or Super Speed, is running at super speed right now. */
    SPEEDING,
    /** Tommy is spinning a whirlwind around himself. */
    WHIRLWIND,
    /** Thor holds Mjolnir up to the sky, calling lightning. */
    CALLING_LIGHTNING,
    /** The Bifrost's light is gathering around its traveller (Thor, or anyone holding his weapon), about to take them. */
    BIFROST,
    /** Loki is invisible while his duplicates draw every eye: his held items, armour and name tag are hidden too. */
    HIDDEN,
    /** Loki has just thrown a dagger (a brief throwing pose). */
    THROWING,
    /** Casting a spell: Loki's raised, open hand (Vanish, Shapeshift); Billy's hand thrust forward (Hex Bolt). */
    CASTING,
    /** Billy's crown is out: while he flies, and for a moment after each spell. */
    CROWNED,
    /** Billy is holding the creatures in front of him spellbound (both hands raised). */
    FREEZING;

    final int bit() {
        return 1 << ordinal();
    }
}
