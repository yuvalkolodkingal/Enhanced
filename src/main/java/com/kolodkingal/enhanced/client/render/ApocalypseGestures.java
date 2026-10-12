package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the gestures of En Sabah Nur: a god who does not hurry. Every gesture starts and ends on the same relaxed
 * pose, {@link #REST} (the arms a little out, as the hover pose of his flight), so one runs into the next without a pop;
 * the ones that hand over to a stance (the shield, the storm) end exactly on {@link StancePoses}, and the take-off ends
 * on the hover anchor of {@link FlightProfile}. Pure data (no Minecraft client classes), so the game tests can check it.
 */
final class ApocalypseGestures {
    /** The arms at rest: {@code M}, then {@code O}. */
    static final float[] REST_M = {-10, 0, 10};
    static final float[] REST_O = {-10, 0, -10};

    static void register() {
        // He rises as if lifted: the arms are swept up a little, then settle (the last key is the hover pose of his flight).
        define(GestureId.TAKEOFF_APOC)
                .key(0, S).m(-10, 0, 10).o(-10, 0, -10).legs(2, 1).head(0, 0, 0)
                .key(6, O).m(-40, 0, 34).o(-40, 0, -34).legs(4, -1).head(-4, 0, 0)
                .key(14, E).m(-26, 0, 26).o(-26, 0, -26).legs(4, -2)
                .key(22, S).m(-8, 0, 14).o(-8, 0, -14).legs(2, 1).head(0, 0, 0);

        // The Wave: the arms drawn back and low, then thrown forward and down, the palms to the ground.
        define(GestureId.APOC_SAND_WAVE)
                .key(0, S).m(REST_M).o(REST_O).lean(0)
                .key(4, O).m(20, 0, 24).o(20, 0, -24).lean(3)
                .key(9, O).m(-72, 0, 6).o(-72, 0, -6).lean(-5)
                .key(18, S).m(REST_M).o(REST_O).lean(0);

        // Entomb: the main arm reaches at the target, holds while the sand crawls, and the hand closes.
        define(GestureId.APOC_ENTOMB)
                .key(0, S).m(REST_M).twist(0)
                .key(5, O).mAim(LOOK, -6, 0, 0).twist(-6)
                .key(14, S).mAim(LOOK, -6, 0, 0).twist(-6)
                .key(18, O).mAim(LOOK, 14, 0, 0).twist(4)
                .key(26, S).m(REST_M).twist(0);

        // The Portal: the arms spread wide, then drawn across the body as he steps through.
        define(GestureId.APOC_PORTAL)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(5, O).m(-20, 0, 84).o(-20, 0, -84).head(-4, 0, 0)
                .key(12, O).m(-60, 0, 12).o(-60, 0, -12).lean(3)
                .key(20, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0);

        // The Shield goes up: the hands drawn to the chest, then opened with the palms out, exactly into the stance. Keyed
        // for fifteen ticks; played at ApocalypseActing.shieldRate so the arms are open as the sphere is complete.
        define(GestureId.APOC_SHIELD_RAISE)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(7, O).m(-62, 18, 6).o(-62, -18, -6).head(StancePoses.APOC_SHIELD_HEAD, 0, 0)
                .key(15, S).m(StancePoses.APOC_SHIELD_M).o(StancePoses.APOC_SHIELD_O).head(StancePoses.APOC_SHIELD_HEAD, 0, 0)
                .lean(StancePoses.APOC_SHIELD_LEAN);

        // The Shield is let go of: the hands fall.
        define(GestureId.APOC_SHIELD_DROP)
                .key(0, S).m(StancePoses.APOC_SHIELD_M).o(StancePoses.APOC_SHIELD_O).head(StancePoses.APOC_SHIELD_HEAD, 0, 0)
                .lean(StancePoses.APOC_SHIELD_LEAN)
                .key(4, O).m(-30, 0, 20).o(-30, 0, -20)
                .key(8, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0);

        // Bestowal: the main hand held out, palm up, to the one who is given his power; the other hand on the chest.
        define(GestureId.APOC_BESTOW)
                .key(0, S).m(REST_M).o(REST_O).twist(0)
                .key(6, O).mAim(LOOK, -10, -12, 0).o(-70, 20, -16).twist(-8)
                .key(14, S).mAim(LOOK, -10, -12, 0).o(-70, 20, -16).twist(-8)
                .key(22, S).m(REST_M).o(REST_O).twist(0);

        // The Sandstorm: the arms gather, then are flung wide and up into the stance.
        define(GestureId.APOC_STORM_START)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(5, O).m(-40, 0, 50).o(-40, 0, -50).head(-4, 0, 0).lean(3)
                .key(11, O).m(StancePoses.APOC_STORM_M).o(StancePoses.APOC_STORM_O).head(StancePoses.APOC_STORM_HEAD, 0, 0)
                .lean(StancePoses.APOC_STORM_LEAN)
                .key(18, S).m(StancePoses.APOC_STORM_M).o(StancePoses.APOC_STORM_O).head(StancePoses.APOC_STORM_HEAD, 0, 0)
                .lean(StancePoses.APOC_STORM_LEAN);
    }

    private ApocalypseGestures() {
    }
}
