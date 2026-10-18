package com.kolodkingal.enhanced.client.render;

import org.jetbrains.annotations.Nullable;

/**
 * Which gestures move the hands in first person (3.4): the rows of {@link FirstPersonArms} that every hero has, and the
 * rows it draws for a custom hero who plays a move of the catalogue ({@code AnimCatalog.GESTURES}) whose own hero's
 * plug-in does not draw it for him. Pure (no client class), so the tests can check that every move of the catalogue has
 * its hands.
 */
public final class FirstPersonMoves {
    /** The gestures that have a row of their own here, for every hero (the switch of {@code FirstPersonArms.gesture}). */
    static final java.util.Set<GestureId> SHARED_ROWS = java.util.EnumSet.of(GestureId.WANDA_SHIELD_RAISE, GestureId.BILLY_BOLT,
            GestureId.LOKI_DAGGER, GestureId.LOKI_FAN, GestureId.QS_BLITZ, GestureId.TOMMY_ACCEL, GestureId.SPEED_DASH, GestureId.QS_BARRAGE,
            GestureId.TOMMY_BOOM, GestureId.SPEED_TIME, GestureId.THOR_THROW);

    /** How a move of the catalogue moves a custom hero's hands when its own hero's plug-in does not draw it. */
    enum Row {
        /** One hand thrust out along the look. */
        POINT,
        /** One hand up to the temple. */
        TEMPLE,
        /** Both hands up to the temples. */
        TEMPLES,
        /** One arm raised to the sky. */
        RAISE,
        /** Both arms raised. */
        RAISE_BOTH,
        /** Both palms pushed out. */
        PUSH,
        /** Both hands up, then brought down with a stamp. */
        SLAM,
        /** The hand circling before him. */
        CIRCLE,
        /** The palm out, then drawn back. */
        PULL,
        /** Both hands out, then dragged back. */
        DRAG,
        /** Both arms thrown wide. */
        WIDE,
        /** Both hands sweeping forward and down. */
        SWEEP,
        /** A hand offered. */
        OFFER,
        /** Both hands swung back, then up with a leap. */
        LEAP
    }

    /** The row of each move of the catalogue ({@code AnimCatalog.GESTURES}) that has none here for every hero. */
    private static final java.util.Map<GestureId, Row> CATALOGUE = new java.util.EnumMap<>(GestureId.class);

    static {
        CATALOGUE.put(GestureId.LOKI_CAST, Row.POINT);
        CATALOGUE.put(GestureId.AGATHA_HEX, Row.POINT);
        CATALOGUE.put(GestureId.ULTRON_HACK, Row.POINT);
        CATALOGUE.put(GestureId.SPIDER_SHOOT, Row.POINT);
        CATALOGUE.put(GestureId.ULTRON_BLAST, Row.POINT);
        CATALOGUE.put(GestureId.XAVIER_BLAST, Row.TEMPLE);
        CATALOGUE.put(GestureId.PHOENIX_MIND_CAST, Row.TEMPLE);
        CATALOGUE.put(GestureId.CYCLOPS_CONCUSSION, Row.TEMPLES);
        CATALOGUE.put(GestureId.THOR_SUMMON, Row.RAISE);
        CATALOGUE.put(GestureId.CYCLOPS_RALLY, Row.RAISE);
        CATALOGUE.put(GestureId.ULTRON_ARMY, Row.RAISE_BOTH);
        CATALOGUE.put(GestureId.WANDA_BEAM_KICK, Row.PUSH);
        CATALOGUE.put(GestureId.PHOENIX_PSIONIC_CAST, Row.PUSH);
        CATALOGUE.put(GestureId.BILLY_FREEZE_CAST, Row.PUSH);
        CATALOGUE.put(GestureId.THANOS_SLAM, Row.SLAM);
        CATALOGUE.put(GestureId.ULTRON_SLAM, Row.SLAM);
        CATALOGUE.put(GestureId.THANOS_TIMELOCK, Row.SLAM);
        CATALOGUE.put(GestureId.THANOS_STEP, Row.CIRCLE);
        CATALOGUE.put(GestureId.THANOS_REWIND, Row.CIRCLE);
        CATALOGUE.put(GestureId.AGATHA_RUNES, Row.CIRCLE);
        CATALOGUE.put(GestureId.THANOS_PULL, Row.PULL);
        CATALOGUE.put(GestureId.AGATHA_DRAIN, Row.DRAG);
        CATALOGUE.put(GestureId.PHOENIX_BURST, Row.WIDE);
        CATALOGUE.put(GestureId.APOC_SAND_WAVE, Row.SWEEP);
        CATALOGUE.put(GestureId.APOC_BESTOW, Row.OFFER);
        CATALOGUE.put(GestureId.CYCLOPS_LEAP, Row.LEAP);
        // 3.5 (the creator lane): the moves added to the catalogue.
        CATALOGUE.put(GestureId.MAG_PULL, Row.PULL);
        CATALOGUE.put(GestureId.MAG_DISARM, Row.POINT);
        CATALOGUE.put(GestureId.QS_SNATCH, Row.PULL);
        CATALOGUE.put(GestureId.QS_BARRAGE, Row.PUSH);
        CATALOGUE.put(GestureId.TOMMY_BLITZ, Row.SWEEP);
        CATALOGUE.put(GestureId.TOMMY_VIBRATE, Row.PUSH);
        CATALOGUE.put(GestureId.TOMMY_ACCEL, Row.POINT);
        CATALOGUE.put(GestureId.LOKI_FAN, Row.SWEEP);
        CATALOGUE.put(GestureId.THANOS_BLAST, Row.POINT);
        CATALOGUE.put(GestureId.THANOS_WARP, Row.POINT);
        CATALOGUE.put(GestureId.THANOS_SIGHT, Row.TEMPLE);
        CATALOGUE.put(GestureId.APOC_ENTOMB, Row.PUSH);
        CATALOGUE.put(GestureId.APOC_PORTAL, Row.CIRCLE);
        CATALOGUE.put(GestureId.CYCLOPS_RICOCHET, Row.TEMPLE);
        CATALOGUE.put(GestureId.XAVIER_FREEZE, Row.TEMPLES);
        CATALOGUE.put(GestureId.XAVIER_CEREBRO, Row.TEMPLES);
        CATALOGUE.put(GestureId.ULTRON_UPLOAD, Row.RAISE);
        CATALOGUE.put(GestureId.SPIDER_BURST, Row.WIDE);
    }

    /** Whether a gesture moves the hands in first person, for any hero who plays it (the custom hero included). */
    public static boolean covers(GestureId id) {
        return SHARED_ROWS.contains(id) || CATALOGUE.containsKey(id);
    }

    /** The catalogue's row of a gesture, or null (it has a row for every hero, or none). */
    @Nullable
    static Row row(GestureId id) {
        return CATALOGUE.get(id);
    }

    private FirstPersonMoves() {
    }
}
