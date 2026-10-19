package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the Hulk's gestures (3.6): weight in every motion, the whole body behind each blow. The keys are timed to
 * the server's moves: the Smash's fists hit the ground at tick 6 ({@code HulkAbilities.SMASH_WINDUP}), the Clap's hands
 * meet at tick 5 ({@code ThunderClap.WINDUP}), the Grab Smash's slams fall at 10, 20 and 30 ({@code GrabSmash.SLAMS}),
 * the boulder leaves his hands 8 ticks after he bends for it ({@code HulkAbilities.RIP_TICKS}), and the transformation
 * lasts its 40 ticks. Pure data (no Minecraft client classes), so the game tests can check it.
 */
public final class HulkGestures {
    /** The arms at rest (they hang wide of his bulk): {@code M}, then {@code O}. */
    public static final float[] REST_M = {0, 0, 8};
    public static final float[] REST_O = {0, 0, -8};
    /** The idle stance: hunched, the arms hanging wide and a little forward, knuckles low. */
    public static final float[] IDLE_M = {-10, 6, 14};
    public static final float[] IDLE_O = {-10, -6, -14};
    public static final float IDLE_LEAN = 9;
    public static final float IDLE_HEAD = -6;
    /** Enraged: the arms out and forward, the fists ready. */
    public static final float[] RAGE_M = {-24, 10, 24};
    public static final float[] RAGE_O = {-24, -10, -24};
    /** In the air: the arms thrown up, the knees tucked. */
    public static final float[] AIR_M = {-150, 0, 18};
    public static final float[] AIR_O = {-150, 0, -18};
    public static final float AIR_LEG_R = -55;
    public static final float AIR_LEG_L = -25;

    static void register() {
        // Hulk Smash: both fists high, then down into the ground (tick 6), the knees giving, the dust.
        define(GestureId.HULK_SMASH)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0)
                .key(4, O).m(-176, 0, 10).o(-176, 0, -10).lean(-8).head(-14, 0, 0)
                .key(6, I).m(-24, 0, 6).o(-24, 0, -6).lean(24).drop(0.32F).head(10, 0, 0)
                .key(11, S).m(-22, 0, 8).o(-22, 0, -8).lean(22).drop(0.30F).head(8, 0, 0)
                .key(16, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0);

        // The Grab Smash: the grab (both hands forward and down), then the victim swung over his head to slam it down
        // at his left (10), his right (20) and his left again (30).
        define(GestureId.HULK_GRAB_SMASH)
                .key(0, S).m(REST_M).o(REST_O).twist(0).lean(0)
                .key(3, O).m(-58, 0, 4).o(-58, 0, -4).lean(14)
                .key(6, S).m(-150, -10, -10).o(-150, -10, 10).twist(10).lean(-4)
                .key(10, I).m(-40, -30, -30).o(-30, -30, 0).twist(-35).lean(22)
                .key(15, S).m(-175, 0, 0).o(-175, 0, 0).twist(0).lean(-6)
                .key(20, I).m(-30, 30, 0).o(-40, 30, 30).twist(35).lean(22)
                .key(25, S).m(-175, 0, 0).o(-175, 0, 0).twist(0).lean(-6)
                .key(30, I).m(-40, -30, -30).o(-30, -30, 0).twist(-35).lean(24)
                .key(32, S).m(REST_M).o(REST_O).twist(0).lean(0);

        // The Thunder Clap: the arms flung wide, then the hands slammed together before him (tick 5).
        define(GestureId.HULK_CLAP)
                .key(0, S).m(REST_M).o(REST_O).lean(0)
                .key(4, O).m(-12, -20, 96).o(-12, 20, -96).lean(-6)
                .key(5, I).m(-88, -24, 2).o(-88, 24, -2).lean(12)
                .key(9, S).m(-86, -22, 2).o(-86, 22, -2).lean(10)
                .key(14, S).m(REST_M).o(REST_O).lean(0);

        // The Roar: the chest out, the arms back and down, the head up, the whole body shaking with it.
        define(GestureId.HULK_ROAR)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(6, O).m(22, 0, 28).o(22, 0, -28).head(-38, 0, 0).lean(-12).mTremble(2.5F, 1.4F).oTremble(2.5F,
                        1.4F)
                .key(22, S).m(24, 0, 30).o(24, 0, -30).head(-36, 0, 0).lean(-12).mTremble(2.5F, 1.4F).oTremble(2.5F,
                        1.4F)
                .key(30, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0);

        // The Super Leap: he crouches, then launches with the arms thrown up (handing over to the air stance).
        define(GestureId.HULK_LEAP_CROUCH)
                .key(0, S).m(REST_M).o(REST_O).drop(0).lean(0).legs(0, 0)
                .key(4, O).m(28, 0, 16).o(28, 0, -16).drop(0.38F).lean(22).legs(-28, -28)
                .key(6, E).m(AIR_M).o(AIR_O).drop(0).lean(-4).legs(0, 0);

        // The landing: the three-point landing, his right fist in the ground, then up.
        define(GestureId.HULK_LAND)
                .key(0, E).m(-34, 0, 4).o(-70, 0, -40).drop(0.55F).lean(32).legs(-70, 18).head(-12, 0, 0)
                .key(8, S).m(-32, 0, 4).o(-68, 0, -40).drop(0.52F).lean(30).legs(-68, 18).head(-12, 0, 0)
                .key(14, S).m(REST_M).o(REST_O).drop(0).lean(0).legs(0, 0).head(0, 0, 0);

        // The Bound: a quick crouch and a long stride into the air.
        define(GestureId.HULK_BOUND)
                .key(0, S).m(REST_M).o(REST_O).drop(0).lean(0).legs(0, 0)
                .key(2, O).m(22, 0, 14).o(22, 0, -14).drop(0.22F).lean(18).legs(-20, -20)
                .key(4, E).m(-120, 0, 20).o(40, 0, -20).drop(0).lean(14).legs(-50, 30)
                .key(10, S).m(REST_M).o(REST_O).drop(0).lean(0).legs(0, 0);

        // The Boulder Throw: both hands down to the ground, then the boulder torn up and lifted over his head (tick
        // 8)...
        define(GestureId.HULK_RIP)
                .key(0, S).m(REST_M).o(REST_O).drop(0).lean(0)
                .key(3, O).m(-8, 0, 18).o(-8, 0, -18).drop(0.36F).lean(28)
                .key(8, E).m(-176, 0, 14).o(-176, 0, -14).drop(0).lean(-6)
                .key(10, S).m(-176, 0, 14).o(-176, 0, -14).lean(-8);

        // ...and hurled from overhead with both hands.
        define(GestureId.HULK_THROW)
                .key(0, S).m(-176, 0, 14).o(-176, 0, -14).lean(-8)
                .key(3, O).m(-64, 0, 10).o(-64, 0, -10).lean(18)
                .key(10, S).m(REST_M).o(REST_O).lean(0);

        // The Gamma Charge: the shoulder down and forward, the body leaning into it.
        define(GestureId.HULK_CHARGE)
                .key(0, S).m(REST_M).o(REST_O).twist(0).lean(0)
                .key(2, O).m(24, 0, 20).o(-48, 0, -24).twist(-32).lean(30)
                .key(12, S).m(26, 0, 22).o(-46, 0, -24).twist(-32).lean(30)
                .key(14, S).m(REST_M).o(REST_O).twist(0).lean(0);

        // Hulk out: he hunches over, clenches, trembles as he grows, and stands to roar at the end.
        define(GestureId.HULK_OUT)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0)
                .key(6, O).m(12, 0, 22).o(12, 0, -22).lean(28).drop(0.22F).head(22, 0, 0)
                .key(20, S).m(-28, 0, 38).o(-28, 0, -38).lean(22).drop(0.16F).head(12, 0, 0).mTremble(3.0F, 1.8F)
                        .oTremble(3.0F, 1.8F)
                .key(30, E).m(14, 0, 46).o(14, 0, -46).lean(-10).drop(0).head(-34, 0, 0)
                .key(36, S).m(14, 0, 44).o(14, 0, -44).lean(-10).head(-32, 0, 0)
                .key(40, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0);

        // Calm down: he sinks to one knee as he shrinks, the hands on his knees, the head bowed; then stands.
        define(GestureId.HULK_CALM)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0).legs(0, 0).head(0, 0, 0)
                .key(10, O).m(-42, 0, 10).o(-42, 0, -10).lean(26).drop(0.45F).legs(-80, 12).head(26, 0, 0)
                .key(34, S).m(-40, 0, 10).o(-40, 0, -10).lean(24).drop(0.44F).legs(-80, 12).head(24, 0, 0)
                .key(40, S).m(REST_M).o(REST_O).lean(0).drop(0).legs(0, 0).head(0, 0, 0);

        // Switch Hulk: the arms out and flexed, the head back, in the green shimmer.
        define(GestureId.HULK_SWITCH)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(5, O).m(-22, 0, 82).o(-22, 0, -82).head(-16, 0, 0).mTremble(2.0F, 1.6F).oTremble(2.0F, 1.6F)
                .key(15, S).m(-20, 0, 80).o(-20, 0, -80).head(-14, 0, 0).mTremble(2.0F, 1.6F).oTremble(2.0F, 1.6F)
                .key(20, S).m(REST_M).o(REST_O).head(0, 0, 0);
    }

    /** The gesture a payload of his starts, or null. */
    @org.jetbrains.annotations.Nullable
    public static GestureId gestureOf(int kind, int arg) {
        return switch (kind) {
            case com.kolodkingal.enhanced.network.HulkFxPayload.SMASH -> arg == 0 ? GestureId.HULK_SMASH : null;
            case com.kolodkingal.enhanced.network.HulkFxPayload.GRAB_SLAM -> arg > 0 ? GestureId.HULK_GRAB_SMASH : null;
            case com.kolodkingal.enhanced.network.HulkFxPayload.CLAP -> arg == 0 ? GestureId.HULK_CLAP : null;
            case com.kolodkingal.enhanced.network.HulkFxPayload.ROAR -> GestureId.HULK_ROAR;
            case com.kolodkingal.enhanced.network.HulkFxPayload.LEAP -> GestureId.HULK_LEAP_CROUCH;
            case com.kolodkingal.enhanced.network.HulkFxPayload.LAND -> arg > 0 ? GestureId.HULK_LAND : null;
            case com.kolodkingal.enhanced.network.HulkFxPayload.BOUND -> GestureId.HULK_BOUND;
            case com.kolodkingal.enhanced.network.HulkFxPayload.RIP -> GestureId.HULK_RIP;
            case com.kolodkingal.enhanced.network.HulkFxPayload.CHARGE -> GestureId.HULK_CHARGE;
            case com.kolodkingal.enhanced.network.HulkFxPayload.TRANSFORM_OUT -> GestureId.HULK_OUT;
            case com.kolodkingal.enhanced.network.HulkFxPayload.TRANSFORM_IN -> GestureId.HULK_CALM;
            case com.kolodkingal.enhanced.network.HulkFxPayload.SWITCH -> GestureId.HULK_SWITCH;
            default -> null;
        };
    }

    private HulkGestures() {
    }
}
