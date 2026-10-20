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
    // ===== heimdall (3.5) =====
    /** Heimdall: his All-Seeing Sight is open (creatures glow through walls, far players are marked). */
    HEIMDALL_SIGHT,
    // ===== odin (3.5) =====
    /** Odin: he kneels in the Odinsleep (cannot act, heals). */
    ODINSLEEP,
    // ===== hela (3.5) =====
    /** Hela: the Goddess of Death's form is on (the headdress has grown). */
    HELA_FORM,
    // ===== strange (3.6) =====
    /** Doctor Strange: his spirit is out (the Astral Projection, Sneak + B): pressed again, it returns to his body. */
    STRANGE_ASTRAL,
    /** Doctor Strange: the Mirror Dimension is open round him (Sneak + N). */
    STRANGE_MIRROR,
    // ===== stones (3.6) =====
    /**
     * The Time Stone's Slow Time is on (the stone, the Eye of Agamotto, Doctor Strange's Y or a custom hero's stone
     * row).
     */
    TIME_SLOW,
    // ===== hulk (3.6) =====
    /** A custom hero with {@code hulk.transform}: his human form is on (every power off, his own skin). */
    HUMAN_FORM,
    // ===== healing (3.7) =====
    // ===== wolverine (3.7) =====
    /** Wolverine: his claws are out (G). */
    CLAWS_OUT,
    /** Wolverine: he climbs walls by his claws (Sneak + G, the claws out). */
    CLAW_CLIMB,
    /** Wolverine: he follows one scent trail (Sneak + N, Track). */
    SCENT_TRACK,
    // ===== deadpool (3.7) =====
    // ===== red_hulk (3.8) =====
    /** Red Hulk is Thaddeus Ross (N: his human form; every key but N refused, anger fills at half rate). */
    ROSS_FORM,
    // ===== falcon (3.8) =====
    /** Falcon's drone Redwing is out (N): it marks monsters and zaps them. */
    REDWING_OUT,
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
