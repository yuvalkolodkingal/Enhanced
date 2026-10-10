package com.kolodkingal.enhanced.power;

/**
 * On/off states of the newer heroes, saved together as one bit set in {@link PowerData}. Only ever append: a
 * toggle's bit is its position in this list, and that is what is saved.
 */
public enum Toggle {
    /** Loki: his true Frost Giant form. */
    FROST_GIANT,
    /** Thor: the God of Thunder unleashed (lightning eyes, lightning in every blow). */
    GOD_OF_THUNDER,
    /** Tommy: sprinting is super speed (on from the start). */
    SUPER_SPEED,
    /** Billy: hearing the thoughts of everything around him. */
    THE_NOISE;

    final int bit() {
        return 1 << ordinal();
    }

    /** The toggles a hero starts with: Tommy's sprint is super speed until he turns it off. */
    static int defaults(Power power) {
        return power == Power.TOMMY ? SUPER_SPEED.bit() : 0;
    }
}
