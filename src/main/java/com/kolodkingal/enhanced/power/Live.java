package com.kolodkingal.enhanced.power;

/**
 * Momentary states of the newer heroes (and of whoever Mjolnir pins down) that clients must see (to draw effects and
 * poses, or to stop a pinned player's own movement) but that are never saved: they are recomputed by the server while
 * the power is in use. Kept together as one bit set in {@link PowerData}.
 */
public enum Live {
    /** Tommy, or Quicksilver in Quicksilver Time or Super Speed, is running at super speed right now. */
    SPEEDING,
    /** Tommy is spinning a whirlwind around himself. */
    WHIRLWIND,
    /**
     * Thor is firing his lightning beam from his hand (or from the weapon in it): V is held down, and every client
     * draws the beam to whatever he looks at. (The name is older than the beam, which used to call lightning down from
     * the sky.)
     */
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
    FREEZING,
    /**
     * Mjolnir lies on this player and pins them in place: their client stops their movement (any player, not only
     * heroes).
     */
    PINNED;

    final int bit() {
        return 1 << ordinal();
    }
}
