package com.kolodkingal.enhanced.power;

/**
 * On/off states of the newer heroes, saved together as one bit set in {@link PowerData}. Only ever append: a toggle's
 * bit is its position in this list, and that is what is saved.
 *
 * <p>The toggles of 3.0 sit in one region per hero (the {@code // =====} lines), so the five lanes that build those
 * heroes append to their own lines and never to the same one. The positions are fixed when a version is released.
 */
public enum Toggle {
    /** Loki: his true Frost Giant form. */
    FROST_GIANT,
    /** Thor: the God of Thunder unleashed (lightning eyes, lightning in every blow). */
    GOD_OF_THUNDER,
    /** Tommy: sprinting is super speed (on from the start). */
    SUPER_SPEED,
    /** Billy: hearing the thoughts of everything around him. */
    THE_NOISE,

    // ===== phoenix =====
    /** Phoenix: reading the mind of whoever she looks at. */
    MIND_READING,

    // ===== spider =====
    /** Spider-Man: sticking to walls and ceilings (on from the start). */
    WALL_CRAWL,
    /** Spider-Man: the tingle that warns him of danger (on from the start). */
    SPIDER_SENSE,
    /** Spider-Man: the Iron Spider's mechanical legs are out. */
    SPIDER_LEGS,

    // ===== thanos =====
    /** Thanos: the Reality Stone's veil of illusion is drawn around him. */
    REALITY_VEIL,

    // ===== ultron =====

    // ===== apocalypse =====

    // ===== professor_x =====
    /** Professor X: hearing the thoughts of the players and creatures around him (Billy's The Noise, with words). */
    HEAR_THOUGHTS,
    // ===== speed (3.3) =====
    /** Tommy: his slow time is on (the world around him runs slower), apart from his Super Speed. */
    TOMMY_TIME,
    /** Tommy: running up walls and along ceilings is switched off (it is on unless he turns it off). */
    TOMMY_WALL_RUN_OFF,
    ;

    final int bit() {
        return 1 << ordinal();
    }

    /**
     * The toggles a hero starts with: Tommy's sprint is super speed until he turns it off, Spider-Man clings and
     * senses.
     */
    static int defaults(Power power) {
        return switch (power) {
            case TOMMY -> SUPER_SPEED.bit();
            case SPIDER_MAN -> WALL_CRAWL.bit() | SPIDER_SENSE.bit();
            default -> 0;
        };
    }
}
