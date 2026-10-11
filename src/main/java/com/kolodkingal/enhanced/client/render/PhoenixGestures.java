package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.HELD;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the Phoenix's gestures (DESIGN section 13): the unfurling of her wings at take-off, the grab and the throw
 * of her telekinesis, the two fingers to the temple of Mind Reading, the ripple of Mind Sense, the cast of Mind Control,
 * the push of the Psionic Field, and the V of arms that gathers the Burst and the scream that lets it go. Telepathy is
 * done with the head and the hands near it, the fire with the whole body thrown open: a glance at her arms tells which of
 * the two she is using.
 *
 * <p>The poses the gestures hand over to are here too ({@link #CHARGE_M}, {@link #CHARGE_O}), because the stance row
 * ({@link PhoenixPoses}) and the last key of {@link GestureId#PHOENIX_CHARGE_START} must be the same numbers. No Minecraft
 * client imports: the tables, and {@link #predicted}, are checked by the game tests.
 */
public final class PhoenixGestures {
    /** The arms of the charge stance: thrown up and out in a V (the pose the charge gesture hands over to). */
    public static final float[] CHARGE_M = {-150, 0, 52};
    public static final float[] CHARGE_O = {-150, 0, -52};
    /** How far the head is thrown back and the body leans (degrees) in the charge. */
    public static final float CHARGE_HEAD = -14;
    public static final float CHARGE_LEAN = -4;

    /** The gestures that carry the V of the charge in their own keys: while one plays, the stance's head and lean give way. */
    private static final GestureId[] OWNS_THE_V = {GestureId.PHOENIX_CHARGE_START, GestureId.PHOENIX_BURST};

    static void register() {
        flight();
        telekinesis();
        telepathy();
        fire();
    }

    // ---- Flight ----

    private static void flight() {
        // The wings unfurl: the arms thrown overhead, then swept down and out to the hover pose (FlightProfile).
        define(GestureId.TAKEOFF_PHOENIX)
                .key(0, S).m(-10, 0, 24).o(-10, 0, -24).legs(4, 2).head(0, 0, 0)
                .key(6, O).m(-168, 6, 16).o(-168, -6, -16).legs(8, -2).head(-12, 0, 0)
                .key(14, E).m(-8, 0, 66).o(-8, 0, -66).legs(10, -4).head(-8, 0, 0)
                .key(24, S).m(-10, 0, 24).o(-10, 0, -24).legs(4, 2).head(0, 0, 0);
    }

    // ---- Telekinesis: an open, steady palm, the other hand raised a little ----

    private static void telekinesis() {
        define(GestureId.TK_GRAB_PHOENIX)
                .key(0, S).m(-14, 0, 10)
                .key(3, O).mAim(HELD, -12, -4, 0).o(-14, 0, -24)
                .key(12, S).mAim(HELD, -8, -4, 0).o(-14, 0, -24);
        // The arm sweeps down the line of the throw, a little less hard than the Scarlet Witch's.
        float g = 0.9F;
        define(GestureId.TK_FLING_PHOENIX)
                .key(0, S).mAim(LOOK, -28 * g, 0, 0).twist(-10 * g).lean(0)
                .key(2, O).mAim(LOOK, 12 * g, 0, 0).twist(8 * g).lean(3 * g)
                .key(10, S).mAim(LOOK, 0, 0, 0).twist(0).lean(0);
    }

    // ---- Telepathy: the head, the hands at the temples ----

    private static void telepathy() {
        // Two fingers to the temple, then released into the stance that tilts the head.
        define(GestureId.PHOENIX_READ_ON)
                .key(0, S).m(-10, 0, 6)
                .key(5, O).m(-160, 10, 2).head(0, 0, -5)
                .key(10, S).m(-160, 10, 2).head(0, 0, -5)
                .key(14, S).m(-10, 0, 6).head(0, 0, 0);
        // A small shake of the head, the way Billy shakes the Noise off.
        define(GestureId.PHOENIX_READ_OFF)
                .key(0, S).head(0, 0, 0)
                .key(2, O).head(0, 9, 0)
                .key(4, O).head(0, -9, 0)
                .key(6, O).head(0, 5, 0)
                .key(8, S).head(0, 0, 0);
        // Mind Sense: both hands to the temples, then the arms sweep out like a ripple.
        define(GestureId.PHOENIX_SENSE)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6)
                .key(5, O).m(-150, 20, 0).o(-150, -20, 0).head(-6, 0, 0)
                .key(12, S).m(-150, 20, 0).o(-150, -20, 0).head(-6, 0, 0)
                .key(20, S).m(-30, 0, 40).o(-30, 0, -40).head(0, 0, 0);
        // Mind Control: the fingers to the temple, the head a little to one side, the mind reaching out.
        define(GestureId.PHOENIX_MIND_CAST)
                .key(0, S).m(-10, 0, 6)
                .key(4, O).m(-168, 0, 2).head(0, 0, -4)
                .key(9, S).m(-168, 0, 2).head(0, 0, -4)
                .key(16, S).m(-10, 0, 6).head(0, 0, 0);
        // The Psionic Field: both arms up, then thrown out and down with a push.
        define(GestureId.PHOENIX_PSIONIC_CAST)
                .key(0, S).m(-8, 0, 10).o(-8, 0, -10)
                .key(6, O).m(-165, 0, 20).o(-165, 0, -20).head(-12, 0, 0).lean(-3)
                .key(13, O).m(-70, -6, 64).o(-70, 6, -64).head(-4, 0, 0).lean(4)
                .key(24, S).m(-10, 0, 10).o(-10, 0, -10).head(0, 0, 0).lean(0);
    }

    // ---- Fire: the Burst ----

    private static void fire() {
        // The arms gather at the chest, then sweep out and up into the V of the stance.
        define(GestureId.PHOENIX_CHARGE_START)
                .key(0, S).m(-6, 0, 12).o(-6, 0, -12)
                .key(6, O).m(-60, 0, 70).o(-60, 0, -70).head(-6, 0, 0)
                .key(14, S).m(CHARGE_M).o(CHARGE_O).head(CHARGE_HEAD, 0, 0).lean(CHARGE_LEAN);
        // The scream: the V thrown wide open, the back arched, then relaxed.
        define(GestureId.PHOENIX_BURST)
                .key(0, S).m(CHARGE_M).o(CHARGE_O).head(CHARGE_HEAD, 0, 0).lean(CHARGE_LEAN)
                .key(2, O).m(-12, 0, 88).o(-12, 0, -88).head(-22, 0, 0).lean(-8)
                .key(9, S).m(-12, 0, 88).o(-12, 0, -88).head(-22, 0, 0).lean(-8)
                .key(24, S).m(-10, 0, 24).o(-10, 0, -24).head(0, 0, 0).lean(0);
    }

    // ---- What the key presses start, before the server has answered ----

    /**
     * The gesture the local Phoenix's own key press starts at once, or null: N (the Burst) when the Burst is rested, she
     * is fed and not already charging; B (Mind Control, not Sneak + B) when it is rested and she has a mind in her
     * sight. The two guards are told by the caller ({@code fed}, {@code mindInSight}), because only a client can see them;
     * the server's refusals (a boss, a Spectator) cannot be seen, and then the gesture plays out and costs nothing.
     */
    @Nullable
    public static GestureId predicted(PowerData data, int ability, long now, boolean sneaking, boolean fed, boolean mindInSight) {
        if (ability == 5 && data.isReady(5, now) && !data.is(Live.PHOENIX_CHARGING) && fed) {
            return GestureId.PHOENIX_CHARGE_START;
        }
        if (ability == 4 && !sneaking && data.isReady(4, now) && mindInSight) {
            return GestureId.PHOENIX_MIND_CAST;
        }
        return null;
    }

    /**
     * How much a gesture that carries the V of the charge in its own keys is playing now (0 to 1): the stance's head and
     * lean are given up by this much, because the gesture's head and lean are added to the stance's and the V would
     * otherwise be thrown back twice as far while the one hands over to the other.
     */
    public static float owningTheV(HeroAnimState st) {
        float own = 0.0F;
        for (HeroAnimState.Slot slot : st.slots) {
            if (slot.active && slot.weight > own) {
                for (GestureId id : OWNS_THE_V) {
                    if (slot.id == id) {
                        own = slot.weight;
                    }
                }
            }
        }
        return Math.min(own, 1.0F);
    }

    private PhoenixGestures() {
    }
}
