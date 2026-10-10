package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the flight and movement gestures: each hero's take-off, braking, the two landings, the skid at the end of a
 * run, the quick turn of changing looks, and the arm gestures of Thor's weapon beyond throwing it (shattering Mjolnir,
 * its fragments coming together, pinning someone with it).
 *
 * <p>Each take-off ends exactly on its hero's hover pose (see {@link FlightProfile}), so the flight layer below shows
 * through without a jump. The ability gestures (the casts) are not here: see {@link WitchGestures} and its siblings.
 */
final class FlightGestures {

    static void register() {
        // A new look: a quick turn and straightening as the new suit shimmers in.
        define(GestureId.LOOK_CHANGE)
                .key(0, S).yaw(0).head(0, 0, 0).mAdd(0, 0, 0).oAdd(0, 0, 0)
                .key(4, S).head(-4, 0, 0).mAdd(0, 0, 4).oAdd(0, 0, -4)
                .key(5, O).yaw(24)
                .key(10, S).yaw(0).head(0, 0, 0).mAdd(0, 0, 0).oAdd(0, 0, 0);

        // ---- Take-offs: the push-off of a hero who is lifted at once ----

        // He lifts, arms opening, chin up: no crouch, no leap.
        define(GestureId.TAKEOFF_MAGNETO)
                .key(0, S).m(-4, 0, 12).o(-4, 0, -12).legs(8, 8)
                .key(6, O).m(-14, 0, 44).o(-14, 0, -44).legs(4, 4).head(-8, 0, 0)
                .key(18, S).m(-6, 0, 18).o(-6, 0, -18).legs(2, 2).head(0, 0, 0);

        // Palms pushed down and back at the ground, knees tucked, then opening.
        define(GestureId.TAKEOFF_WANDA)
                .key(0, S).m(10, 0, 14).o(10, 0, -14).legs(10, 10)
                .key(5, O).m(35, 6, 24).o(35, -6, -24).legs(-14, -6).head(-6, 0, 0)
                .key(9, S).m(35, 6, 24).o(35, -6, -24).legs(-14, -6)
                .key(20, S).m(-20, 6, 32).o(-20, -6, -32).legs(7, 4).head(0, 0, 0);

        // He levitates: no push, the arms just float a little away.
        define(GestureId.TAKEOFF_VISION)
                .key(0, S).m(2, 0, 8).o(2, 0, -8)
                .key(7, S).m(-22, 0, 16).o(-22, 0, -16).head(-4, 0, 0)
                .key(14, S).m(2, 0, 8).o(2, 0, -8).head(0, 0, 0);

        // A calm float: palms turning up, a lazy half twirl.
        define(GestureId.TAKEOFF_BILLY)
                .key(0, S).m(-28, -12, 28).o(-28, 12, -28).yaw(0)
                .key(8, E).m(-35, -22, 42).o(-35, 22, -42).legs(14, -8).head(-10, 0, 0).yaw(14)
                .key(22, S).m(-28, -12, 28).o(-28, 12, -28).legs(10, -6).head(0, 0, 0).yaw(0);

        // The weapon whirls at his side, then he is hauled up with the weapon arm leading.
        define(GestureId.TAKEOFF_THOR)
                .key(0, S).mWorld(60, 0, 0).o(-20, 0, -35).legs(10, 10).twist(-10).spin()
                .key(5, S).mWorld(70, 0, 0).o(-14, 0, -34).legs(-26, -14).twist(-10)
                .key(12, E).mWorld(120, 0, 0).o(-10, 0, -32).legs(-10, 0).twist(0)
                .key(18, O).mWorld(158, 0, 0).o(-8, 0, -30).legs(6, 6)
                .key(28, S).mWorld(158, 0, 0).o(-8, 0, -30).legs(0, 0);

        // ---- Braking: the pitch spring does most of the work; the legs swing forward and the arms flare ----

        define(GestureId.BRAKE)
                .key(0, O).mAdd(0, 0, 0).oAdd(0, 0, 0).legs(-16, -16)
                .key(3, S).mAdd(-18, 0, 22).oAdd(-18, 0, -22).legs(-16, -16).head(-4, 0, 0)
                .key(12, S).mAdd(0, 0, 0).oAdd(0, 0, 0).legs(-4, -4).head(0, 0, 0);

        // The weapon arm is not touched.
        define(GestureId.BRAKE_THOR)
                .key(0, O).oAdd(0, 0, 0).legs(-30, -30)
                .key(3, S).oAdd(-20, 0, -24).legs(-30, -30)
                .key(12, S).oAdd(0, 0, 0).legs(-4, -4);

        // ---- Landings ----

        // The legs absorb it, the arms open a little.
        define(GestureId.LAND_SOFT)
                .key(0, O).legs(-18, -18).mAdd(-10, 0, 12).oAdd(-10, 0, -12)
                .key(4, S).legs(-6, -6).mAdd(-10, 0, 12).oAdd(-10, 0, -12)
                .key(12, S).legs(0, 0).mAdd(0, 0, 0).oAdd(0, 0, 0);

        // The superhero landing: the weapon planted, one knee down.
        defineHeroLanding(GestureId.LAND_HERO);
        defineHeroLanding(GestureId.THOR_ARRIVAL);

        // ---- The end of a run: a skid ----

        define(GestureId.RUN_SKID)
                .key(0, O).mAdd(-18, 0, 24).oAdd(-18, 0, -24).legs(-24, -24).lean(-10)
                .key(4, S).mAdd(-18, 0, 24).oAdd(-18, 0, -24).legs(-24, -24).lean(-10)
                .key(10, S).mAdd(0, 0, 0).oAdd(0, 0, 0).legs(-4, -4).lean(0);

        // ---- Shattering Mjolnir, its fragments coming home, and pinning someone with it ----

        define(GestureId.THOR_SHATTER)
                .key(0, S).mWorld(100, 0, 0).lead().o(-20, 0, -30)
                .key(6, O).mWorld(140, 0, 0).lead().o(-60, 0, -60).head(-8, 0, 0).lean(-4)
                .key(10, E).mWorld(110, 0, 25).o(-110, 0, -70).twist(8)
                .key(22, S).mWorld(100, 0, 0).o(-30, 0, -40);

        define(GestureId.THOR_REFORM)
                .key(0, S).mWorld(80, 0, -12).o(-30, 0, -30)
                .key(18, S).mWorld(80, 0, -12);

        define(GestureId.THOR_PIN)
                .key(0, S).mWorld(100, 0, 0).lead().o(-10, 0, -24)
                .key(5, I).mWorld(40, 0, 0).lead().o(10, -10, -30).lean(20).twist(-12).legs(-30, 20)
                .key(14, S).mWorld(40, 0, 0).lead().o(10, -10, -30).lean(20).twist(-12);
    }

    /**
     * The landing of a hero from a dive: a 40 degree lean about the feet, the front leg planted (world elevation 60) and the
     * back leg trailing (-80), the weapon arm held at the planting angle of the weapon with the lead grip, so its head
     * touches down ahead of the front foot.
     */
    private static void defineHeroLanding(GestureId id) {
        define(id)
                .key(0, O).mPlant(0, 0, 0).lead().o(0, 0, -30).legs(-100, 40).lean(40).drop(0.28F)
                .key(8, S).mPlant(0, 0, 0).lead().o(0, 0, -30).legs(-100, 40).lean(40).drop(0.28F)
                .key(17, E).mPlant(-10, 0, 0).lead().o(-8, 0, -24).legs(-60, 10).lean(20).drop(0.14F)
                .key(26, S).mPlant(-10, 0, 0).o(0, 0, 0).legs(0, 0).lean(0).drop(0);
    }

    private FlightGestures() {
    }
}
