package com.kolodkingal.enhanced.client.render;

/**
 * A sustained pose that lasts while a synced state holds (a beam is on, something is held): it has an eased weight and
 * its own pose rows in {@link Stances}. Ambient stances are applied first; aimed and holding ones after, so they win where
 * they overlap (and their arms count as occupied for the swing restore).
 */
public enum Stance {
    MAG_FIELD(4, 5, false),
    VISION_PHASED(4, 5, false),
    VISION_DENSE(3, 5, false),
    LOKI_FROST(5, 6, false),
    THOR_UNLEASHED(4, 6, false),
    NOISE(6, 6, false),
    WANDA_READY(3, 8, false),
    WANDA_HEX_CONTROL(3, 4, false),
    THOR_BIFROST(5, 3, false),

    THOR_WAIT(2, 3, true),
    TK_HOLD(2.5F, 4, true),
    TK_STRAIN(6, 8, true),
    MAG_SHRAPNEL(1.5F, 3, true),
    MAG_CATCH(1, 3, true),
    BILLY_FREEZE(2, 4, true),
    VISION_BEAM(2, 3, true),
    BEAM_WANDA(1.2F, 3, true),
    SHIELD_WANDA(1.5F, 3, true),
    THOR_BEAM(1, 3, true),
    /** 3.2: a continuous beam (Billy's Hex Beam, or any other source: {@code Live.BEAMING}): one arm out along the look, the other braced. */
    BEAM_HAND(1, 3, true),
    /** 3.2: Billy's Hex Barrier is up: the off forearm raised forward holding the plate, the other arm braced back, the body turned a little. */
    BARRIER_BILLY(1.5F, 3, true),
    TOMMY_WHIRL(3, 5, true),

    // The stances of 3.0, in one region per hero (the // ===== lines), each lane's own. Whatever the order here, the
    // ambient ones are applied first and the aimed and holding ones after (see Stances).
    // ===== phoenix =====
    /** Phoenix: Mind Reading is on, the head tilted as if listening. */
    PHOENIX_READ(6, 6, false),
    /** Phoenix: the Burst gathers, a V of arms thrown up and out. */
    PHOENIX_CHARGE(1.5F, 4, true),

    // ===== spider =====
    /** Spider-Man clings to a wall: the arms reach up, the knees go out, the body faces the wall. */
    SPIDER_CLING_WALL(3, 5, false),
    /** He hangs from a ceiling: the body is flipped over, feet to the ceiling. */
    SPIDER_CLING_CEILING(6, 8, false),
    /** The Black Eyes: a predatory hunch, hands hanging forward and clawed. */
    SPIDER_SURGE(4, 8, false),
    /** Instant Kill: a menacing stillness. */
    SPIDER_KILL(3, 6, false),
    /** His spider-sense tingles: the guard goes up. */
    SPIDER_ALERT(1.5F, 5, false),
    /** A zip: the web arm points at the anchor, the body is turned to it. */
    SPIDER_ZIP(1.5F, 4, true),
    /** A swing: the web arm is up along the web, the knees tucked. */
    SPIDER_SWING(2, 5, true),

    // ===== thanos =====
    /** Thanos: the Reality Veil is up, the Gauntlet hand held out as if it held a thin film. */
    THANOS_VEIL(4, 5, false),
    /** Thanos: the Soul Drain holds, the Gauntlet arm along the look and the legs braced. */
    THANOS_DRAIN(2, 4, true),

    // ===== ultron =====
    /** Ultron: R is held, both palms raised along the look and firing plasma in turn. */
    ULTRON_FIRE(1, 3, true),
    /** Ultron: the Overcharge builds, one palm raised along the look and the other hand bracing its wrist. */
    ULTRON_CHARGE(2, 3, true),

    // ===== apocalypse =====
    /** Apocalypse's Shield: both forearms raised before the chest, palms out. */
    APOC_SHIELD(1.5F, 3.0F, true),
    /** The Sandstorm: both arms flung wide and up, the head back. */
    APOC_STORM(3.0F, 5.0F, true),

    // ===== cyclops (3.3) =====
    /** Cyclops's Optic Blast: two fingers held to the visor, the other arm braced, the feet planted. */
    CYCLOPS_BLAST(1.5F, 3.0F, true),
    /** The glasses are off and the giant beam pours out: both hands at his face, straining, braced against it. */
    CYCLOPS_UNLEASHED(2.0F, 4.0F, true),
    // ===== professor_x (3.3) =====
    /** The old Professor X sits in his wheelchair: the legs forward on the footrest, the forearms on the armrests. */
    XAVIER_SEATED(4.0F, 4.0F, false),
    /** His mind is in another body: two fingers at his temple, the head bowed, still. */
    XAVIER_TRANCE(2.0F, 4.0F, true),
    /** Cerebro's helmet is on: both hands at its sides. */
    XAVIER_CEREBRO(2.0F, 4.0F, true),
    // ===== agatha (3.3) =====
    /** Agatha's Witch's Shield: both hands held out before her, fingers spread, a little apart. */
    AGATHA_SHIELD(1.5F, 3.0F, true)
    ;

    public static final Stance[] VALUES = values();
    public static final int COUNT = VALUES.length;

    /** Time constants (ticks) of the weight rising and falling. */
    public final float tauIn;
    public final float tauOut;
    /** Aimed and holding stances: their arms are occupied, so the swing of an attack is mostly muted. */
    public final boolean aimed;

    Stance(float tauIn, float tauOut, boolean aimed) {
        this.tauIn = tauIn;
        this.tauOut = tauOut;
        this.aimed = aimed;
    }
}
