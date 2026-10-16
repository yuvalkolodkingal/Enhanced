package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
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
    /** The wind-up (ticks) the keys of {@link GestureId#WANDA_SHIELD_RAISE} are made for: arms open at this tick. */
    public static final int SHIELD_KEYED_WIND_UP = 15;
    /** A shield that forms in fewer ticks than this is not acted out with a wind-up: the arms just snap into place. */
    public static final int SHIELD_SHORTEST_WIND_UP = 4;
    /** How long a raise that the data does not (yet) show is believed, in ticks (see {@link #shieldRaiseAborted}). */
    public static final int SHIELD_DATA_GRACE = 12;

    /**
     * What stands in for a gesture a hero of 3.0 does not have yet: the toggles, abilities and states of the five new
     * heroes are routed (so the table is complete) but pending, until the lane that builds the hero routes them to its
     * own gestures. {@code SkeletonTests.noAnimationIsPendingAtTheRelease} fails at the release while any of them is
     * left.
     */
    public static final GestureId PENDING_GESTURE = GestureId.LOOK_CHANGE;
    /** The way {@link #acting} says an ability of a hero of 3.0 is not acted yet. */
    public static final String PENDING_ACTING = "none: pending lane";
    /** What {@link #reason} says of a momentary state of a hero of 3.0 that is not acted yet (it begins with this). */
    public static final String PENDING_REASON = "pending lane";

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
        CAUGHT,
        /** Mjolnir burst into fragments in his hand (Sneak + R). */
        SHATTERED,
        /** The last fragment came home and Mjolnir is whole in his hand again. */
        REFORMED
    }

    /** What happened to the Scarlet Witch's Hex. */
    public enum HexChange {
        RISES, DISSOLVES, TWEAKED, GROWS, SHRINKS
    }

    /** The gesture of raising a Chaos Shield that takes {@code windUp} ticks to form: the wind-up, or a snap. */
    public static GestureId shieldRaise(int windUp) {
        return windUp < SHIELD_SHORTEST_WIND_UP ? GestureId.WANDA_SHIELD_SNAP : GestureId.WANDA_SHIELD_RAISE;
    }

    /**
     * How fast the keys of {@link GestureId#WANDA_SHIELD_RAISE} are played so that the arms are thrown open at the
     * very tick the shield is complete, whatever the wind-up in the config: 1 for the keyed 15 ticks, 0.5 for a wind-up
     * of 30.
     */
    public static float shieldRate(int windUp) {
        return windUp <= 0 ? 1.0F : (float) SHIELD_KEYED_WIND_UP / windUp;
    }

    /**
     * Whether the effects of a Chaos Shield being raised (the motes gathering, the flash of it thrown open) are over
     * because it was let go of, or refused, before it formed. {@code flagUp} is whether the synced data says the shield
     * is being held, {@code seenUp} whether it has said so since the raise began, {@code sinceStart} the ticks since
     * the raise began. The local player's raise starts at the key press, a round trip before the data can say so: it is
     * given {@link #SHIELD_DATA_GRACE} ticks to.
     */
    public static boolean shieldRaiseAborted(boolean flagUp, boolean seenUp, long sinceStart) {
        return !flagUp && (seenUp || sinceStart > SHIELD_DATA_GRACE);
    }

    /**
     * The gesture Magneto's own key press starts before the server answers ({@link GestureTable#predicted}), or null:
     * Sneak + R with R holding nothing, the pull rested and something in his hunger bar ({@code fed}: food left, or he
     * cannot starve, as the server asks it) is the Magnetic Pull ({@link GestureId#MAG_PULL}). A rider's press is never
     * predicted (vanilla owns his body then). The client does not know the server's second of grace after a grip, nor the
     * short wait after a pull that found nothing: a press the server ignores then shows the gesture to him alone, as any
     * predicted gesture the server turns down.
     */
    public static GestureId magnetoPredicted(PowerData data, int ability, long now, boolean sneaking, boolean fed) {
        return ability == 1 && sneaking && fed && data.heldEntityId() == -1 && data.isReady(1, now) ? GestureId.MAG_PULL : null;
    }

    /** The gesture an ability's cooldown starting stands for, or null. {@code left} is the cooldown in ticks. */
    public static GestureId cooldown(Power power, int ability, long left) {
        HeroClient hero = HeroClients.of(power); // (the heroes of 3.0 answer for themselves)
        if (hero != null) {
            return hero.cooldownGesture(ability, left);
        }
        return switch (power) {
            // R's cooldown is the Magnetic Pull's alone (it starts only when a pull affected something).
            case MAGNETO -> ability == 1 ? GestureId.MAG_PULL : ability == 3 ? GestureId.MAG_DISARM : null;
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
            // The toggles of 3.0 (one region per hero): pending, routed to the stand-in whoever has them, until the
            // hero's lane routes each to a gesture of its own (for its own hero only, as above).
            // ===== phoenix =====
            case MIND_READING -> power == Power.PHOENIX ? (on ? GestureId.PHOENIX_READ_ON : GestureId.PHOENIX_READ_OFF) : null;
            // ===== spider =====
            case WALL_CRAWL -> power == Power.SPIDER_MAN ? (on ? GestureId.SPIDER_CRAWL_ON : GestureId.SPIDER_CRAWL_OFF) : null;
            case SPIDER_SENSE -> power == Power.SPIDER_MAN ? GestureId.SPIDER_SENSE_TOGGLE : null;
            case SPIDER_LEGS -> power == Power.SPIDER_MAN ? (on ? GestureId.SPIDER_LEGS_OUT : GestureId.SPIDER_LEGS_IN) : null;
            // ===== thanos =====
            case REALITY_VEIL -> power == Power.THANOS ? (on ? GestureId.THANOS_VEIL_ON : GestureId.THANOS_VEIL_OFF) : null;
            // ===== ultron =====
            // ===== apocalypse =====
        };
    }

    /** The gesture a flag flipping stands for, or null. {@code white} is White Vision, {@code phased} Vision passing through things. */
    public static GestureId flag(Power power, Flag flag, boolean on, boolean white, boolean phased) {
        HeroClient hero = HeroClients.of(power);
        if (hero != null) {
            return hero.flagGesture(flag, on);
        }
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
        HeroClient hero = HeroClients.of(power);
        if (hero != null) {
            return hero.liveGesture(live);
        }
        return switch (live) {
            case WHIRLWIND -> power == Power.TOMMY ? GestureId.TOMMY_WHIRL_START : null;
            case CALLING_LIGHTNING -> power == Power.THOR ? (armed ? GestureId.THOR_BEAM_KICK : GestureId.THOR_BEAM_KICK_BARE) : null;
            // 3.2: the Hex Beam begins as the Hex Bolt was cast (the hand pulled back, then thrust out); the stance BEAM_HAND holds it.
            case BEAMING -> power == Power.BILLY ? GestureId.BILLY_BOLT : null;
            // Both of Loki's tricks that cast (shapeshift and vanish) raise this.
            case CASTING -> power == Power.LOKI ? GestureId.LOKI_CAST : null;
            default -> null;
        };
    }

    /** The gesture of grabbing something with telekinesis, or null for a hero who has none. */
    public static GestureId grab(Power power) {
        HeroClient hero = HeroClients.of(power);
        if (hero != null) {
            return hero.grabGesture();
        }
        return switch (power) {
            case MAGNETO -> GestureId.TK_GRAB_MAGNETO;
            case SCARLET_WITCH -> GestureId.TK_GRAB_WANDA;
            case BILLY -> GestureId.TK_GRAB_BILLY;
            default -> null;
        };
    }

    /** The gesture of throwing what was held, or null. */
    public static GestureId fling(Power power) {
        HeroClient hero = HeroClients.of(power);
        if (hero != null) {
            return hero.flingGesture();
        }
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
            case SHATTERED -> GestureId.THOR_SHATTER;
            case REFORMED -> GestureId.THOR_REFORM;
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
            // (Sneak + R, the Magnetic Pull, is the cooldown of R: MAG_PULL.)
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
                case 1 -> "live:BEAMING";
                case 4 -> "cooldown";
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
            // The heroes of 3.0 (one arm each): pending until the hero's lane says how each ability is acted.
            // ===== phoenix =====
            // (Sneak + R, Mind Sense, is live:MIND_SENSING; Sneak + B, the Psionic Field, is live:PHOENIX_PSIONIC.)
            case PHOENIX -> switch (ability) {
                case 1 -> "toggle:MIND_READING";
                case 2 -> "flight";
                case 3 -> "held";
                case 4 -> "live:CASTING";
                default -> "live:PHOENIX_CHARGING";
            };
            // ===== spider =====
            // (R's Web Shot is live:CASTING and Sneak + R's burst the cooldown; G's swing is live:SWINGING; Sneak + V is
            // toggle:SPIDER_SENSE; Sneak + B, Instant Kill, is live:INSTANT_KILL: all routed by his plug-in.)
            case SPIDER_MAN -> switch (ability) {
                case 1 -> "cooldown";
                case 2 -> "live:ZIPPING";
                case 3 -> "toggle:WALL_CRAWL";
                case 4 -> "toggle:SPIDER_LEGS";
                default -> "live:BLACK_EYES";
            };
            // ===== thanos =====
            // (Sneak + V, the Reality Veil, is toggle:REALITY_VEIL; Sneak + B, the Soul Sight, is live:SOUL_SIGHT; the second use of
            // R, G and N is told from the first by the length of the cooldown, see ThanosGestures.cooldown.)
            case THANOS -> switch (ability) {
                case 1, 2, 3, 5 -> "cooldown";
                default -> "live:SOUL_DRAINING";
            };
            // ===== ultron =====
            // (Sneak + R, the Overcharge, is live:ULTRON_CHARGING and its blast the cooldown of R; Sneak + B, the Hack, is
            // live:CASTING; Sneak + V, the Slam, and Sneak + N, Command, are told by the payload of their effect.)
            case ULTRON -> switch (ability) {
                case 1 -> "flag:BEAM";
                case 2 -> "flight";
                case 3 -> "none: Gravity Pull and Gravity Slam are told by the payload of their effect, which names the caster";
                default -> "cooldown";
            };
            // ===== apocalypse =====
            case APOCALYPSE -> switch (ability) {
                case 1, 3, 5 -> "cooldown";
                case 2 -> "flight";
                default -> "flag:CHAOS_SHIELD";
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
            case BEAMING -> "a stance of any beam (BEAM_HAND, which holds the arm out along the look); Billy's Hex Beam also starts with his BILLY_BOLT gesture";
            case PINNED -> "Mjolnir lying on them is the whole picture: they can't move, so they have no move to make";
            // The states of 3.0 (one region per hero): pending until the hero's lane gives them a gesture or a reason.
            // ===== phoenix =====
            case PHOENIX_CHARGING, MIND_SENSING, PHOENIX_PSIONIC -> null;
            case PHOENIX_BLAZE -> "the flames are drawn by a layer (PhoenixFlameLayer) and the eyes by GlowingEyesLayer, not by a pose";
            // ===== spider =====
            case CLINGING_WALL, CLINGING_CEILING -> "stances (SPIDER_CLING_WALL, SPIDER_CLING_CEILING): he is already in the pose of the surface he walks"
                    + " on; the arrival is the animator's SPIDER_ARRIVE and the push off it SPIDER_LEAP";
            case ZIPPING, SWINGING, SENSING, BLACK_EYES, INSTANT_KILL -> null;
            // ===== thanos =====
            case SOUL_DRAINING, SOUL_SIGHT -> null;
            // ===== ultron =====
            case ULTRON_CHARGING -> null;
            case LIFTED -> "Sokovia carries them: like a pinned player they can't move, so they have no move to make";
            // ===== apocalypse =====
            case SANDSTORM -> null;
        };
    }

    /** The gestures that other code than the table starts: the animator's own edges and the payloads. */
    public static boolean startedElsewhere(GestureId id) {
        for (HeroClient hero : HeroClients.all()) {
            if (hero.startedElsewhere(id)) {
                return true; // (the heroes of 3.0 say which of their gestures a payload or an edge starts)
            }
        }
        return switch (id) {
            case LOOK_CHANGE, TAKEOFF_MAGNETO, TAKEOFF_WANDA, TAKEOFF_VISION, TAKEOFF_BILLY, TAKEOFF_THOR, BRAKE, BRAKE_THOR, LAND_SOFT,
                 LAND_HERO, THOR_ARRIVAL, RUN_SKID -> true;
            // Let go of what telekinesis held; the snap of a shield that comes up at once; Thor setting Mjolnir down on
            // someone (a payload: nothing in his data changes but his empty hand).
            case TK_RELEASE, WANDA_SHIELD_SNAP, THOR_PIN -> true;
            default -> false;
        };
    }

    private GestureRoutes() {
    }
}
