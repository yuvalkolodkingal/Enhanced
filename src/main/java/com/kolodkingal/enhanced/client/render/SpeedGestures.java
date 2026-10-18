package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the gestures of the two speedsters. Quicksilver is cocky and quick (goggles, a sprinter's start, a flurry
 * of jabs, a darting hand, a tap at his headphones); Tommy is aggressive and exact (shaking his arms out, circling a room
 * of armed men in two turns, shivering through a wall, a vibrating palm).
 */
final class SpeedGestures {

    static void register() {
        quicksilver();
        tommy();
        strikes();
    }

    /** The speed strikes of 3.3 (and Tommy's slow time). */
    private static void strikes() {
        // The Dash Strike: a crouch to launch, then the shoulder thrown forward, the off fist cocked back.
        define(GestureId.SPEED_DASH)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6).lean(0).drop(0).legs(0, 0)
                .key(2, O).m(40, 0, 10).o(40, 0, -10).lean(18).drop(0.10F).legs(-30, 20)
                .key(4, E).m(-80, -20, 0).o(30, 0, -20).lean(30).twist(-18).drop(0).legs(-40, 30)
                .key(8, S).m(-80, -20, 0).o(30, 0, -20).lean(24).twist(-18)
                .key(12, S).m(-10, 0, 6).o(-10, 0, -6).lean(0).twist(0).legs(0, 0);

        // Slow time: both palms pushed out at the world, then drawn back slowly, as if holding it back.
        define(GestureId.SPEED_TIME)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6).head(0, 0, 0)
                .key(4, O).mAim(LOOK, 0, -10, 0).oAim(LOOK, 0, 10, 0).head(-6, 0, 0)
                .key(12, S).m(-60, -15, 0).o(-60, 15, 0)
                .key(18, S).m(-10, 0, 6).o(-10, 0, -6).head(0, 0, 0);

        // The Pebble Barrage: the hand scoops low, then flicks along the look three times in a blur.
        define(GestureId.QS_BARRAGE)
                .key(0, S).m(-10, 0, 6).lean(0).head(0, 0, 0)
                .key(2, O).m(30, 10, 10).lean(14).head(14, 0, 0)
                .key(4, E).mAim(LOOK, 0, 0, 0).lean(6).head(0, 0, 0)
                .key(6, O).m(-60, -10, 10)
                .key(8, E).mAim(LOOK, 0, 0, 0)
                .key(10, O).m(-60, -10, 10)
                .key(14, S).m(-10, 0, 6).lean(0);

        // The Sonic Boom: braced to a dead stop, arms flung out and down, the head thrown back by the blast.
        define(GestureId.TOMMY_BOOM)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6).lean(0).drop(0).legs(0, 0).head(0, 0, 0)
                .key(2, O).m(-20, 0, 80).o(-20, 0, -80).lean(-14).drop(0.12F).legs(-24, 24).head(-16, 0, 0)
                .key(8, S).m(-20, 0, 70).o(-20, 0, -70).lean(-8).drop(0.06F).legs(-18, 18).head(-10, 0, 0)
                .key(14, S).m(-10, 0, 6).o(-10, 0, -6).lean(0).drop(0).legs(0, 0).head(0, 0, 0);
    }

    private static void quicksilver() {
        // Quicksilver Time: the hand to the goggles on his forehead.
        define(GestureId.QS_GOGGLES)
                .key(0, S).m(-10, 0, 6).head(0, 0, 0)
                .key(4, O).m(-158, 0, 10).head(4, 0, 0)
                .key(10, S).m(-158, 0, 10)
                .key(16, S).m(-10, 0, 6).head(0, 0, 0);

        // Super Speed: a sprinter's start.
        define(GestureId.QS_START)
                .key(0, S).legs(-18, 8).lean(8).drop(0.06F)
                .key(4, O).legs(-24, 10).lean(12).drop(0.08F)
                .key(8, S).legs(0, 0).lean(0).drop(0);

        // The speed blitz: six hits in about eight ticks, alternating fists.
        define(GestureId.QS_BLITZ)
                .key(0.0F, O).m(-95, -6, 0).o(-10, 0, -20).twist(-12).lean(16).head(8, 0, 0)
                .key(1.6F, O).m(-20, 0, 8).o(-92, 6, 0).twist(12)
                .key(3.2F, O).m(-95, -6, 0).o(-10, 0, -20).twist(-12)
                .key(4.8F, O).m(-20, 0, 8).o(-92, 6, 0).twist(12)
                .key(6.4F, O).m(-95, -6, 0).o(-10, 0, -20).twist(-12)
                .key(8.0F, O).m(-20, 0, 8).o(-92, 6, 0).twist(12)
                .key(12, S).m(-10, 0, 6).o(-10, 0, -6).twist(0).lean(0).head(0, 0, 0);

        // The pickpocket: the hand darts out and back with a flourish, a grin.
        define(GestureId.QS_SNATCH)
                .key(0, S).m(-10, 0, 6).head(0, 0, 0)
                .key(1.5F, O).mAim(LOOK, 0, 0, 0)
                .key(3, S).mAim(LOOK, 0, 0, 0)
                .key(5, O).m(-60, -10, 10).head(0, 0, 6)
                .key(8, S).m(-60, -10, 10);

        // The Walkman: a tap at the headphone.
        define(GestureId.QS_WALKMAN)
                .key(0, S).m(-10, 0, 6)
                .key(3, O).m(-150, 0, 12)
                .key(8, S).m(-10, 0, 6);
    }

    private static void tommy() {
        // Super Speed on: he shakes his arms out.
        define(GestureId.TOMMY_READY)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6)
                .key(2, O).m(-50, 0, 10).o(20, 0, -10)
                .key(4, O).m(20, 0, 6).o(-50, 0, -6)
                .key(6, O).m(-30, 0, 8).o(-30, 0, -8)
                .key(8, S).m(-10, 0, 6).o(-10, 0, -6);

        // The whirlwind begins: a crouch to launch, springing up into the stance.
        define(GestureId.TOMMY_WHIRL_START)
                .key(0, S).legs(-30, 30).drop(0.10F).lean(6)
                .key(3, O).legs(-10, 10).drop(0)
                .key(6, S).legs(-10, 10).drop(0).lean(6);

        // The disarm blitz: two full turns in eight ticks with the arms thrown out, then the weapons hugged to his chest.
        define(GestureId.TOMMY_BLITZ)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6).yaw(0).lean(0)
                .key(8, E).m(-30, 0, 70).o(-30, 0, -70).yaw(720).lean(20)
                .key(10, O).m(-48, -42, 0).o(-48, 42, 0).yaw(720)
                .key(26, S).m(-48, -42, 0).o(-48, 42, 0).yaw(720)
                .key(34, S).m(-10, 0, 6).o(-10, 0, -6).yaw(720).lean(0);

        // Vibrate: his molecules shiver (the shiver itself is added by HeroPoses), about to go through the wall.
        define(GestureId.TOMMY_VIBRATE)
                .key(0, S).m(-40, -30, 0).o(-40, 30, 0).lean(6)
                .key(10, S).m(-40, -30, 0).o(-40, 30, 0).lean(6);

        // Molecular acceleration: a palm thrust out, the fingers a blur.
        define(GestureId.TOMMY_ACCEL)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6).lean(0)
                .key(2, O).mAim(LOOK, 0, -6, 0).mTremble(10, 2.2F).o(20, 0, -25).lean(10)
                .key(7, S).mAim(LOOK, 0, -6, 0).mTremble(10, 2.2F).o(20, 0, -25).lean(10)
                .key(12, S).m(-10, 0, 6).o(-10, 0, -6).lean(0);
    }

    private SpeedGestures() {
    }
}
