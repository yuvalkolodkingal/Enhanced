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

    /** The cooldowns (ticks) that tell two abilities on one key apart (3.3; the abilities' own constants). */
    static final int QS_BLITZ_COOLDOWN = com.kolodkingal.enhanced.ability.QuicksilverAbilities.BLITZ_COOLDOWN;
    static final int QS_PICKPOCKET_COOLDOWN = com.kolodkingal.enhanced.ability.QuicksilverAbilities.PICKPOCKET_COOLDOWN;
    static final int TOMMY_VIBRATE_COOLDOWN = com.kolodkingal.enhanced.ability.TommyAbilities.VIBRATE_COOLDOWN;
    static final int TOMMY_BLITZ_COOLDOWN = com.kolodkingal.enhanced.ability.TommyAbilities.BLITZ_COOLDOWN;

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
            // 3.3: Sneak + G is the Dash Strike (G itself has no cooldown); V's cooldown is the Speed Blitz's (80 ticks) or
            // Sneak + V's Pickpocket's (100); N is the Pebble Barrage.
            case QUICKSILVER -> switch (ability) {
                case 2 -> GestureId.SPEED_DASH;
                case 3 -> left > QS_BLITZ_COOLDOWN ? GestureId.QS_SNATCH : GestureId.QS_BLITZ;
                case 5 -> GestureId.QS_BARRAGE;
                default -> null;
            };
            // 3.3: Sneak + G is the Dash Strike; B's cooldown is Vibrate's (40 ticks) or Sneak + B's Disarm Blitz's (160); the
            // Whirlwind and the Sonic Boom (V, Sneak + V) are acted by their momentary states.
            case TOMMY -> switch (ability) {
                case 2 -> GestureId.SPEED_DASH;
                case 4 -> left > TOMMY_VIBRATE_COOLDOWN ? GestureId.TOMMY_BLITZ : GestureId.TOMMY_VIBRATE;
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
            // ===== professor_x =====
            case HEAR_THOUGHTS -> power == Power.PROFESSOR_X ? (on ? GestureId.XAVIER_THOUGHTS_ON : GestureId.XAVIER_THOUGHTS_OFF) : null;
            // ===== speed (3.3) =====
            case TOMMY_TIME -> power == Power.TOMMY && on ? GestureId.SPEED_TIME : null;
            // (Wall Run on or off: he shakes his arms out, as for his Super Speed.)
            case TOMMY_WALL_RUN_OFF -> power == Power.TOMMY ? GestureId.TOMMY_READY : null;
            // ===== heimdall (3.5) =====
            case HEIMDALL_SIGHT -> power == Power.HEIMDALL ? (on ? GestureId.HEIMDALL_SIGHT_ON : GestureId.HEIMDALL_SIGHT_OFF) : null;
            // (The toggles of 3.5 stand in with the pending gesture, for their own hero only, until their lanes route them.)
            // ===== odin (3.5) =====
            // (The Odinsleep: he sinks to one knee, and rises when he wakes.)
            case ODINSLEEP -> power == Power.ODIN ? (on ? GestureId.ODIN_KNEEL : GestureId.ODIN_RISE) : null;
            // ===== hela (3.5) =====
            case HELA_FORM -> power == Power.HELA ? (on ? GestureId.HELA_FORM_ON : GestureId.HELA_FORM_OFF) : null;
            // (The toggles of 3.6 stand in with the pending gesture, for their own hero only, until their lanes route
            // them.)
            // ===== strange (3.6) =====
            case STRANGE_ASTRAL -> power == Power.DOCTOR_STRANGE ? (on ? GestureId.STRANGE_ASTRAL
                    : GestureId.STRANGE_ASTRAL_IN) : null;
            case STRANGE_MIRROR -> power == Power.DOCTOR_STRANGE && on ? GestureId.STRANGE_MIRROR : null;
            // ===== stones (3.6) =====
            // (Slow Time: Doctor Strange's Y; the stones' own gesture is lane L2's StoneGestures.)
            case TIME_SLOW -> power == Power.DOCTOR_STRANGE && on ? GestureId.STRANGE_EYE_OPEN : null;
            // ===== hulk (3.6) =====
            // (A custom hero's Human Form: he calms down into it, and Hulks out of it.)
            case HUMAN_FORM -> power == Power.HULK ? (on ? GestureId.HULK_CALM : GestureId.HULK_OUT) : null;
            // (The toggles of 3.7 stand in with the pending gesture, for their own hero only, until their lanes route
            // them.)
            // ===== healing (3.7) =====
            // ===== wolverine (3.7) =====
            // (The claws out and in; Claw Climb hooks a claw high; Track's head goes down to the scent.)
            case CLAWS_OUT -> power == Power.WOLVERINE ? (on ? GestureId.WOLVERINE_SNIKT : GestureId.WOLVERINE_SHINK)
                    : null;
            case CLAW_CLIMB -> power == Power.WOLVERINE && on ? GestureId.WOLVERINE_CLIMB : null;
            case SCENT_TRACK -> power == Power.WOLVERINE && on ? GestureId.WOLVERINE_TRACK : null;
            // ===== deadpool (3.7) =====
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
            // (3.3: Super Speed is G now, a key of its own.)
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
            case SONIC_BOOM -> power == Power.TOMMY ? GestureId.TOMMY_BOOM : null;
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
            // (3.3: Sneak + R, the Walkman, is flag:WALKMAN; Sneak + G, the Dash Strike, the cooldown of G; Sneak + V, Pickpocket, the
            // cooldown of V told by its length.)
            case QUICKSILVER -> switch (ability) {
                case 1 -> "flag:QUICKSILVER_TIME";
                case 2 -> "flag:SUPER_SPEED";
                case 3, 5 -> "cooldown";
                default -> "none: Wall & Water Run is a message; running on a wall or a ceiling is the stance of the surface (SPEED_WALL, SPEED_CEILING)";
            };
            case VISION -> switch (ability) {
                case 1 -> "flag:BEAM";
                case 2 -> "flight";
                case 3 -> "flag:VISION_PHASED";
                case 4 -> "flag:DENSE";
                default -> "flag:DISGUISE";
            };
            // (3.3: Sneak + R, Wall Run, is a message; Sneak + G, the Dash Strike, the cooldown of G; Sneak + V, the Sonic Boom,
            // live:SONIC_BOOM; Sneak + B, the Disarm Blitz, the cooldown of B told by its length.)
            case TOMMY -> switch (ability) {
                case 1 -> "toggle:TOMMY_TIME";
                case 2 -> "toggle:SUPER_SPEED";
                case 4, 5 -> "cooldown";
                default -> "live:WHIRLWIND";
            };
            case BILLY -> switch (ability) {
                case 1 -> com.kolodkingal.enhanced.ability.BillyAbilities.beamEnabled() ? "live:BEAMING" : "cooldown"; // (the single bolt: its cooldown)
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
            // ===== cyclops (3.3) =====
            // (R held, the Optic Blast, is the stance CYCLOPS_BLAST: the hand eases up to the visor with it; R's cooldown is the
            // Ricochet's. N's cooldown tells the glasses coming off from going on by its length, see CyclopsHeroMath.)
            case CYCLOPS -> "cooldown";
            // 3.4: a custom hero's keys are whatever powers he put on them: every key that acts moves his cast marker, and
            // the binding it names plays its gesture (CustomActing.castEdge: the move chosen, the borrowed ability's own, a
            // module's default); the cooldown's route stands for that (CustomClient.cooldownGesture, a generic press's cast).
            // The bits a borrowed ability flips are routed by the hero who owns them in his kit.
            case CUSTOM -> "cooldown";
            // ===== professor_x =====
            // (The Mental Blast and Freeze Minds play their gestures when their cooldowns start; the trance and Cerebro are
            // stances, begun by the gestures of their states.)
            case PROFESSOR_X -> switch (ability) {
                case 1, 4 -> "cooldown";
                case 2 -> "toggle:HEAR_THOUGHTS";
                case 3 -> "live:MIND_LINKED";
                default -> "live:CEREBRO";
            };
            // ===== agatha (3.3) =====
            // (Sneak + R, the hex mark or the mind glimpse, is live:CASTING: routed by her plug-in.)
            case AGATHA -> switch (ability) {
                case 1 -> "live:BEAMING";
                case 2 -> "flight";
                case 3, 5 -> "cooldown";
                default -> "flag:CHAOS_SHIELD";
            };
            // ===== heimdall (3.5) =====
            // (R's cooldown is the Lunge's (the blows' are too short to tell, see HeimdallClient): the three blows are told by
            // live:HEIMDALL_SLASHING, which his plug-in's edges count into the combo; G is the Sight's switch (Far Sight a
            // stance); V the Bifrost's state; B held is a stance and Sneak + B, N and Sneak + N are told by the payloads of their
            // light, which name the caster.)
            case HEIMDALL -> switch (ability) {
                case 1 -> "cooldown";
                case 2 -> "toggle:HEIMDALL_SIGHT";
                case 3 -> "live:BIFROST";
                case 4 -> "none: the Guardian's Ward is a stance (HEIMDALL_WARD); the Gatekeeper's Call is told by the payload of its golden flash";
                default -> "none: the Bifrost Strike and the Bifrost for a friend are told by the payloads of their light, which name the caster";
            };
            // ===== odin (3.5) =====
            // (R's cooldown tells the blast from a new Gungnir coming to his hand by its length; Sneak + R, the throw, is
            // live:ODIN_THROWN. Sneak + G, the Heal, is the cooldown of G told by its length; V's cooldown tells Banish from the
            // Odinforce Wave. Sneak + B, the Ravens' Report, is live:ODIN_RAVENS too; Sneak + N, the Odinsleep, is
            // toggle:ODINSLEEP. See OdinHeroMath.)
            case ODIN -> switch (ability) {
                case 2 -> "flag:CHAOS_SHIELD";
                case 4 -> "live:ODIN_RAVENS";
                default -> "cooldown";
            };
            // ===== hela (3.5) =====
            // (Sneak + V, the Death Dash, and Sneak + B, Fenris, are told by the payload of their effect, as R and G are; the
            // form's toggle plays the headdress's gesture, the raising of the dead and the grasp their states'.)
            case HELA -> switch (ability) {
                case 1 -> "none: the volley and the rain are told by the payload of their blades, which names the caster";
                case 2 -> "none: the wall and the burst are told by the payload of their spikes, which names the caster";
                case 3 -> "toggle:HELA_FORM";
                case 4 -> "live:HELA_SUMMONING";
                default -> "live:HELA_GRASPING";
            };
            // ===== doctor_strange (3.6) =====
            // (R's lashes and grapple, Sneak + V's Dome, B's bands and N's portals are told by the payloads of their
            // sparks, which name the caster; V held is the Shield's state; Sneak + B, the Astral Projection, and
            // Sneak + N, the Mirror Dimension, are their switches; his Y, the Eye, is toggle:TIME_SLOW.)
            case DOCTOR_STRANGE -> switch (ability) {
                case 1 -> "none: the lashes and the Whip Grapple are told by the payload of the whip, which names the caster";
                case 2 -> "flight";
                case 3 -> "live:STRANGE_SHIELD";
                case 4 -> "none: the Crimson Bands are told by their payload; the Astral Projection is toggle:STRANGE_ASTRAL";
                default -> "none: the portals are told by their payload; the Mirror Dimension is toggle:STRANGE_MIRROR";
            };
            // ===== hulk (3.6) =====
            // (Every move of his is told by the payload of its effect, which names him: the Smash and the Grab Smash,
            // the Clap and the Roar, the leap, its landing and the Bound, the boulder torn up and thrown, the charge,
            // the transformation and the switch. See HulkActing.gestureOf.)
            case HULK -> "none: every move of the Hulk is told by the payload of its effect, which names him"
                    + " (HulkGestures.gestureOf)";
            // ===== wolverine (3.7) =====
            // (R's three slashes and Sneak + R's spin, Sneak + V's Feral Leap and N's Sniff are told by the payloads of
            // their effects, which name him; G is the claws' switch (Sneak + G the climb's); V and B are his states,
            // whose gestures his plug-in gives (WolverineClient.liveGesture); Sneak + N, Track, is toggle:SCENT_TRACK.)
            case WOLVERINE -> switch (ability) {
                case 1 -> "none: the slashes and the spin are told by the payload of their claws, which names him"
                        + " (WolverineGestures.gestureOf)";
                case 2 -> "toggle:CLAWS_OUT";
                case 3 -> "live:WOLVERINE_POUNCING";
                case 4 -> "live:WOLVERINE_BERSERK";
                default -> "none: the Sniff is told by its payload (WOLVERINE_SNIFF); Track is toggle:SCENT_TRACK";
            };
            // ===== deadpool (3.7) =====
            case DEADPOOL -> "none: Deadpool's keys are acted by his plug-in (lane L3 of 3.7)";
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
            // ===== cyclops (3.3) =====
            case CYCLOPS_UNLEASHED -> "a stance (CYCLOPS_UNLEASHED, both hands at his face while the giant beam pours out); the glasses"
                    + " coming off are N's gesture (its cooldown), and the beam is drawn by the shared beam renderer";
            // ===== professor_x =====
            case MIND_LINKED, CEREBRO -> null;
            case MIND_CONTROLLED -> "Professor X steers the body: it walks and strikes as he makes it, so its own walk is its pose";
            case XAVIER_FOCUS -> "the cooldowns of the Mental Blast and Freeze Minds start at the same moment and play the gesture";
            // ===== speed (3.3) =====
            case SURFACE_WALL, SURFACE_CEILING -> "stances (SPEED_WALL, SPEED_CEILING): the body is turned to the surface he runs on, in his run"
                    + " cycle; the turn on and off is the stance's easing";
            case SONIC_BOOM -> "Tommy's (a hero of before 3.0): his live gesture TOMMY_BOOM acts it, and the shockwave is the server's particles";
            // ===== heimdall (3.5) =====
            // (Until each lane of 3.5 gives its states a gesture or a reason of their own; not the 3.0 pending marker, which
            // fails a release.)
            case HEIMDALL_WARD -> "a stance (HEIMDALL_WARD): both hands on the pommel of Hofund planted before him";
            case HEIMDALL_FARSIGHT -> "a stance (HEIMDALL_FARSIGHT): the hand shading his eyes while he looks far";
            case HEIMDALL_SLASHING -> "each blow of the combo: HeimdallClient's edges count the rises of the state into HEIMDALL_SLASH_1, 2 and 3";
            // ===== odin (3.5) =====
            // (Both have gestures of his own, ODIN_THROW and ODIN_RAVENS, which his plug-in's liveGesture gives for Odin; the
            // checks that ask only the heroes of 3.0 and 3.3 read this instead.)
            case ODIN_THROWN -> "Odin's own gesture ODIN_THROW, from his plug-in (OdinClient.liveGesture)";
            case ODIN_RAVENS -> "Odin's own gesture ODIN_RAVENS, from his plug-in (OdinClient.liveGesture)";
            // ===== hela (3.5) =====
            case HELA_GRASPING, HELA_SUMMONING -> null;
            // ===== strange (3.6) =====
            // (Until each lane of 3.6 gives its states a gesture or a reason of their own; not the 3.0 pending marker,
            // which fails a release.)
            case STRANGE_SHIELD -> null;
            case STRANGE_ASTRAL -> "the switch STRANGE_ASTRAL plays its gestures; the spirit is AstralLayer's glow";
            case STRANGE_MIRROR -> "the switch STRANGE_MIRROR plays its gesture; the sphere is MirrorView's";
            case STRANGE_CASTING -> "a stance (STRANGE_CASTING): his hands up in the mudra while a spell lasts";
            // ===== stones (3.6) =====
            case TIME_SLOWING -> "drawn, not acted: the Time Stone's two green rings turn round the wrist of the"
                    + " stone's arm (StoneArmLayer) and the zone's edge glows; the body goes on as it was";
            case POWER_VEINS -> "drawn, not acted: violet veins crawl up the stone's arm (StoneArmLayer) while the"
                    + " Purple Beam's own stance (BEAM_HAND, from BEAMING) holds the arm out";
            // ===== hulk (3.6) =====
            case HULK_TRANSFORMING -> "the transformation's payload plays HULK_OUT, HULK_CALM or HULK_SWITCH (it names"
                    + " the Hulk)";
            case HULK_LEAPING -> "the leap's payload plays HULK_LEAP_CROUCH or HULK_BOUND; in the air the stance"
                    + " HULK_AIR holds him";
            case HULK_GRABBING -> "the grab's payload plays HULK_GRAB_SMASH, timed to the three slams";
            case HULK_CHARGING -> "the charge's payload plays HULK_CHARGE";
            case HULK_ENRAGED -> "a stance (HULK_ENRAGED), and his eyes glow on GlowingEyesLayer";
            // ===== healing (3.7) =====
            // (Until each lane of 3.7 gives its states a gesture or a reason of their own; not the 3.0 pending marker,
            // which fails a release.)
            case HEALING_DOWN -> "the healing factor's payload plays HEALING_DOWN for every healer (to one knee,"
                    + " onto his side), held while the state lasts; when it ends he gets up and HEALING_RISE follows"
                    + " (client/healing/HealingClient)";
            // ===== wolverine (3.7) =====
            // (Both have gestures of his own, from his plug-in: WolverineClient.liveGesture.)
            case WOLVERINE_BERSERK, WOLVERINE_POUNCING -> null;
            // ===== deadpool (3.7) =====
            case DEADPOOL_PARRYING, DEADPOOL_AIMING -> "pending: Deadpool's lane (L3 of 3.7) acts it";
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
            // ===== healing (3.7) =====
            // (Every healer's: the healing factor's payloads and the end of the DOWN state start them,
            // client/healing/HealingClient.)
            case HEALING_DOWN, HEALING_RISE, HEALING_BURST -> true;
            default -> false;
        };
    }

    private GestureRoutes() {
    }
}
