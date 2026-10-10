package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Toggle;

/**
 * Which gesture each change in a hero's synced data starts: the table that {@link GestureTable} reads. It is pure (no
 * Minecraft client classes), so the game tests can check that every ability, toggle, momentary state and flag is either
 * acted or has a reason why not ({@link #acting}).
 *
 * <p>Abilities that last (a beam held, a shield up, something held with telekinesis, the Bifrost gathering) are stances
 * ({@link Stance}); this table gives the gesture that plays when they begin or end, and the one-shots.
 */
public final class GestureRoutes {
    /**
     * Whether the Chaos Shield's flag rises at the key press (so every client starts its wind-up with the caster, the
     * preferred design when the shield's effect comes 15 ticks later) rather than after the wind-up (then the flag starts the
     * short snap and only the caster's predicted raise shows the whole wind-up).
     */
    public static final boolean SHIELD_FLAG_AT_PRESS = true;

    /** A Boolean flag of the synced power data (the ones that are not a {@link Toggle} or a {@link Live} bit). */
    public enum Flag {
        MAGNETIC_SHIELD, SHRAPNEL, BEAM, CHAOS_SHIELD, VISION_PHASED, DENSE, DISGUISE, QUICKSILVER_TIME, SUPER_SPEED, WALKMAN
    }

    /** What happened to Thor's weapon. */
    public enum Hammer {
        /** Thrown from a hand. */
        THROWN,
        /** Called to him from the ground, a block, a creature (it was not in his hand). */
        SUMMONED,
        /** Turned around in the air to come home. */
        RECALLED_IN_FLIGHT,
        /** Drawn from his inventory into his hand. */
        DRAWN,
        /** Caught: it came home. */
        CAUGHT
    }

    /** What happened to the Scarlet Witch's Hex. */
    public enum HexChange {
        RISES, DISSOLVES, TWEAKED, GROWS, SHRINKS
    }

    /** The gesture an ability's cooldown starting stands for, or null. {@code left} is the cooldown in ticks. */
    public static GestureId cooldown(Power power, int ability, long left) {
        return switch (power) {
            case MAGNETO -> ability == 3 ? GestureId.MAG_DISARM : null;
            case QUICKSILVER -> ability == 2 ? GestureId.QS_BLITZ : ability == 3 ? GestureId.QS_SNATCH : null;
            case TOMMY -> switch (ability) {
                case 2 -> GestureId.TOMMY_BLITZ;
                case 4 -> GestureId.TOMMY_VIBRATE;
                case 5 -> GestureId.TOMMY_ACCEL;
                default -> null;
            };
            case BILLY -> ability == 1 ? GestureId.BILLY_BOLT : ability == 4 ? GestureId.BILLY_FREEZE_CAST : null;
            // A single dagger has a short cooldown, a fan a long one.
            case LOKI -> ability == 1 ? (left <= 15 ? GestureId.LOKI_DAGGER : GestureId.LOKI_FAN) : null;
            default -> null;
        };
    }

    /** The gesture a toggle being switched stands for, or null. */
    public static GestureId toggle(Power power, Toggle toggle, boolean on) {
        return switch (toggle) {
            case FROST_GIANT -> power == Power.LOKI ? (on ? GestureId.LOKI_FROST_ON : GestureId.LOKI_FROST_OFF) : null;
            case GOD_OF_THUNDER -> power == Power.THOR ? (on ? GestureId.THOR_UNLEASH_ON : GestureId.THOR_UNLEASH_OFF) : null;
            case SUPER_SPEED -> power == Power.TOMMY && on ? GestureId.TOMMY_READY : null;
            case THE_NOISE -> power == Power.BILLY ? (on ? GestureId.BILLY_NOISE_ON : GestureId.BILLY_NOISE_OFF) : null;
        };
    }

    /** The gesture a flag flipping stands for, or null. {@code white} is White Vision, {@code phased} Vision passing through things. */
    public static GestureId flag(Power power, Flag flag, boolean on, boolean white, boolean phased) {
        return switch (flag) {
            case MAGNETIC_SHIELD -> power == Power.MAGNETO ? (on ? GestureId.MAG_SHIELD_ON : GestureId.MAG_SHIELD_OFF) : null;
            case SHRAPNEL -> power == Power.MAGNETO && on ? GestureId.MAG_SHRAPNEL_START : null;
            case BEAM -> power == Power.SCARLET_WITCH && on ? GestureId.WANDA_BEAM_KICK
                    : power == Power.VISION && on && !white && !phased ? GestureId.VISION_FOCUS : null;
            case CHAOS_SHIELD -> power != Power.SCARLET_WITCH ? null
                    : on ? (SHIELD_FLAG_AT_PRESS ? GestureId.WANDA_SHIELD_RAISE : GestureId.WANDA_SHIELD_SNAP) : GestureId.WANDA_SHIELD_DROP;
            case VISION_PHASED -> power == Power.VISION ? (on ? GestureId.VISION_PHASE_IN : GestureId.VISION_PHASE_OUT) : null;
            case DENSE -> power == Power.VISION ? (on ? GestureId.VISION_DENSE_ON : GestureId.VISION_DENSE_OFF) : null;
            case DISGUISE -> power == Power.VISION ? GestureId.VISION_DISGUISE : null;
            case QUICKSILVER_TIME -> power == Power.QUICKSILVER && on ? GestureId.QS_GOGGLES : null;
            case SUPER_SPEED -> power == Power.QUICKSILVER && on ? GestureId.QS_START : null;
            case WALKMAN -> power == Power.QUICKSILVER && on ? GestureId.QS_WALKMAN : null;
        };
    }

    /** The gesture a momentary state beginning stands for, or null. {@code armed} is Thor with a weapon in a hand. */
    public static GestureId live(Power power, Live live, boolean armed) {
        return switch (live) {
            case WHIRLWIND -> power == Power.TOMMY ? GestureId.TOMMY_WHIRL_START : null;
            case CALLING_LIGHTNING -> power == Power.THOR ? (armed ? GestureId.THOR_BEAM_KICK : GestureId.THOR_BEAM_KICK_BARE) : null;
            // Both of Loki's tricks that cast (shapeshift and vanish) raise this.
            case CASTING -> power == Power.LOKI ? GestureId.LOKI_CAST : null;
            default -> null;
        };
    }

    /** The gesture of grabbing something with telekinesis, or null for a hero who has none. */
    public static GestureId grab(Power power) {
        return switch (power) {
            case MAGNETO -> GestureId.TK_GRAB_MAGNETO;
            case SCARLET_WITCH -> GestureId.TK_GRAB_WANDA;
            case BILLY -> GestureId.TK_GRAB_BILLY;
            default -> null;
        };
    }

    /** The gesture of throwing what was held, or null. */
    public static GestureId fling(Power power) {
        return switch (power) {
            case MAGNETO -> GestureId.TK_FLING_MAGNETO;
            case SCARLET_WITCH -> GestureId.TK_FLING_WANDA;
            case BILLY -> GestureId.TK_FLING_BILLY;
            default -> null;
        };
    }

    public static GestureId hammer(Hammer what) {
        return switch (what) {
            case THROWN -> GestureId.THOR_THROW;
            case SUMMONED, RECALLED_IN_FLIGHT -> GestureId.THOR_SUMMON;
            case DRAWN -> GestureId.THOR_RECALL_DRAW;
            case CAUGHT -> GestureId.THOR_CATCH;
        };
    }

    public static GestureId hex(HexChange change) {
        return switch (change) {
            case RISES -> GestureId.WANDA_HEX_RISE;
            case DISSOLVES -> GestureId.WANDA_HEX_DISSOLVE;
            case TWEAKED -> GestureId.WANDA_HEX_TWEAK;
            case GROWS -> GestureId.WANDA_HEX_GROW;
            case SHRINKS -> GestureId.WANDA_HEX_SHRINK;
        };
    }

    /**
     * How an ability of a hero is acted, for the completeness check of the game tests: one of {@code cooldown},
     * {@code flag:<NAME>}, {@code toggle:<NAME>}, {@code live:<NAME>}, {@code held}, {@code hammer}, {@code hex},
     * {@code flight}, or {@code none: <why not>}.
     */
    public static String acting(Power power, int ability) {
        return switch (power) {
            case MAGNETO -> switch (ability) {
                case 1 -> "held";
                case 2 -> "flag:SHRAPNEL";
                case 3 -> "cooldown";
                case 4 -> "flag:MAGNETIC_SHIELD";
                default -> "flight";
            };
            case SCARLET_WITCH -> switch (ability) {
                case 1 -> "flag:BEAM";
                case 2 -> "flight";
                case 3 -> "held";
                case 4 -> "flag:CHAOS_SHIELD";
                default -> "hex";
            };
            case QUICKSILVER -> switch (ability) {
                case 1 -> "flag:QUICKSILVER_TIME";
                case 2, 3 -> "cooldown";
                case 4 -> "none: running up walls and over water is only a message and a change in how he runs";
                default -> "flag:WALKMAN";
            };
            case VISION -> switch (ability) {
                case 1 -> "flag:BEAM";
                case 2 -> "flight";
                case 3 -> "flag:VISION_PHASED";
                case 4 -> "flag:DENSE";
                default -> "flag:DISGUISE";
            };
            case TOMMY -> switch (ability) {
                case 1 -> "toggle:SUPER_SPEED";
                case 2, 4, 5 -> "cooldown";
                default -> "live:WHIRLWIND";
            };
            case BILLY -> switch (ability) {
                case 1, 4 -> "cooldown";
                case 2 -> "flight";
                case 3 -> "held";
                default -> "toggle:THE_NOISE";
            };
            case LOKI -> switch (ability) {
                case 1 -> "cooldown";
                case 2 -> "none: he is invisible while his duplicates draw every eye, so nobody sees him";
                case 3, 4 -> "live:CASTING";
                default -> "toggle:FROST_GIANT";
            };
            case THOR -> switch (ability) {
                case 1 -> "hammer";
                case 2 -> "flight";
                case 3 -> "live:CALLING_LIGHTNING";
                case 4 -> "toggle:GOD_OF_THUNDER";
                default -> "none: the Bifrost is a stance and its arrival a gesture of the payload";
            };
            default -> "none: no abilities";
        };
    }

    /** Why a momentary state has no gesture of its own (it is acted by something else), or null when it has one. */
    public static String reason(Live live) {
        return switch (live) {
            case SPEEDING -> "the run cycle, its start and its skid are driven by the animator from the motion";
            case WHIRLWIND, CALLING_LIGHTNING, CASTING -> null;
            case BIFROST -> "a stance (THOR_BIFROST); the arrival is a gesture of the payload";
            case HIDDEN -> "Loki is invisible: nobody sees him";
            case THROWING -> "the cooldown of the daggers starts at the same moment and plays the throw";
            case CROWNED -> "Billy's crown is drawn by its own layer, not a pose";
            case FREEZING -> "the cooldown of Freeze plays the cast; the stance BILLY_FREEZE holds the pose";
        };
    }

    /** The gestures that other code than the table starts: the animator's own edges, the payloads and the reserved moves. */
    public static boolean startedElsewhere(GestureId id) {
        return switch (id) {
            case LOOK_CHANGE, TAKEOFF_MAGNETO, TAKEOFF_WANDA, TAKEOFF_VISION, TAKEOFF_BILLY, TAKEOFF_THOR, BRAKE, BRAKE_THOR, LAND_SOFT,
                 LAND_HERO, THOR_ARRIVAL, RUN_SKID -> true;
            // Let go of what telekinesis held; the snap of a shield that comes up at once; the moves other branches start.
            case TK_RELEASE, WANDA_SHIELD_SNAP, THOR_SHATTER, THOR_REFORM, THOR_PIN -> true;
            default -> false;
        };
    }

    private GestureRoutes() {
    }
}
