package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.network.WolverineFxPayload;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Wolverine's gestures (3.7): quick and low, a brawler's weight on the balls of his feet. The keys are
 * timed to the server's moves: a slash strikes at once (its keys cut across at tick 3), the spin turns once over 10
 * ticks, the Pounce crouches {@code Pounce.CROUCH} 4 ticks before it flies {@code Pounce.FLIGHT} 12, the claws come out
 * over {@code Claws.OUT_TICKS} 3. Pure data (no Minecraft client classes), so the game tests can check it.
 */
public final class WolverineGestures {
    /** The arms at rest: {@code M}, then {@code O}. */
    public static final float[] REST_M = {0, 0, 4};
    public static final float[] REST_O = {0, 0, -4};
    /** The claws-out guard: the fists low, a little forward and out. */
    public static final float[] GUARD_M = {-28, -8, 14};
    public static final float[] GUARD_O = {-22, 8, -14};
    public static final float GUARD_LEAN = 6;
    /** The Rage: hunched, the claws wide and forward. */
    public static final float[] RAGE_M = {-40, -10, 42};
    public static final float[] RAGE_O = {-40, 10, -42};
    public static final float RAGE_LEAN = 16;
    /** Climbing: both hands high on the wall. */
    public static final float[] CLIMB_M = {-165, 0, 12};
    public static final float[] CLIMB_O = {-165, 0, -12};

    static void register() {
        // Snikt: both fists brought up before the chest, the jolt as the blades shoot (tick 3), the arms settle.
        define(GestureId.WOLVERINE_SNIKT)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(3, O).m(-74, -14, 4).o(-74, 14, -4).head(8, 0, 0)
                .key(5, S).m(-82, -18, 8).o(-82, 18, -8).head(4, 0, 0).mTremble(1.5F, 2.0F).oTremble(1.5F, 2.0F)
                .key(12, S).m(GUARD_M).o(GUARD_O).head(0, 0, 0);

        // Shink: the fists lowered, a short shake of the hands as the blades go home.
        define(GestureId.WOLVERINE_SHINK)
                .key(0, S).m(GUARD_M).o(GUARD_O)
                .key(3, O).m(-42, -6, 8).o(-42, 6, -8).mTremble(1.0F, 2.0F).oTremble(1.0F, 2.0F)
                .key(10, S).m(REST_M).o(REST_O);

        // The first slash: the main hand high and out, then down and across his body (tick 3).
        define(GestureId.WOLVERINE_SLASH_1)
                .key(0, S).m(GUARD_M).twist(0).lean(0)
                .key(2, O).m(-158, -20, 46).twist(-18).lean(-2)
                .key(3, I).m(-58, 38, -28).twist(22).lean(10)
                .key(6, S).m(-50, 34, -24).twist(18).lean(8)
                .key(10, S).m(GUARD_M).twist(0).lean(0);

        // The second: the other hand, the mirror of the first.
        define(GestureId.WOLVERINE_SLASH_2)
                .key(0, S).o(GUARD_O).twist(0).lean(0)
                .key(2, O).o(-158, 20, -46).twist(18).lean(-2)
                .key(3, I).o(-58, -38, 28).twist(-22).lean(10)
                .key(6, S).o(-50, -34, 24).twist(-18).lean(8)
                .key(10, S).o(GUARD_O).twist(0).lean(0);

        // The cross-slash: both hands crossed high over his head, then torn down and apart (tick 4).
        define(GestureId.WOLVERINE_SLASH_3)
                .key(0, S).m(GUARD_M).o(GUARD_O).lean(0).drop(0)
                .key(3, O).m(-168, 24, -22).o(-168, -24, 22).lean(-6).drop(0)
                .key(4, I).m(-46, -30, 62).o(-46, 30, -62).lean(18).drop(0.18F)
                .key(8, S).m(-40, -26, 58).o(-40, 26, -58).lean(16).drop(0.16F)
                .key(14, S).m(GUARD_M).o(GUARD_O).lean(0).drop(0);

        // The spin: both arms flung out level, one full turn of the body, a little crouch.
        define(GestureId.WOLVERINE_SPIN)
                .key(0, S).m(GUARD_M).o(GUARD_O).yaw(0).drop(0)
                .key(2, O).m(-84, 0, 84).o(-84, 0, -84).drop(0.14F)
                .key(10, S).m(-82, 0, 82).o(-82, 0, -82).yaw(360).drop(0.12F)
                .key(14, S).m(GUARD_M).o(GUARD_O).drop(0);

        // The Pounce's crouch: low, the arms drawn back, the weight forward (handing over to the flight).
        define(GestureId.WOLVERINE_POUNCE_CROUCH)
                .key(0, S).m(REST_M).o(REST_O).drop(0).lean(0).legs(0, 0)
                .key(3, O).m(34, 0, 18).o(34, 0, -18).drop(0.36F).lean(30).legs(-34, -34)
                .key(6, E).m(-104, 0, 14).o(-104, 0, -14).drop(0).lean(16).legs(-60, -36);

        // In the air: both claws forward, the knees tucked.
        define(GestureId.WOLVERINE_POUNCE_AIR)
                .key(0, S).m(-104, 0, 14).o(-104, 0, -14).lean(16).legs(-60, -36)
                .key(6, S).m(-118, 0, 16).o(-118, 0, -16).lean(20).legs(-72, -44)
                .key(14, S).m(-150, 0, 16).o(-150, 0, -16).lean(12).legs(-50, -30);

        // The stab: both claws driven down into the prey, the landing crouched, then up.
        define(GestureId.WOLVERINE_POUNCE_STAB)
                .key(0, S).m(-150, 0, 16).o(-150, 0, -16).lean(12).drop(0).legs(-50, -30)
                .key(2, I).m(-46, 0, 10).o(-46, 0, -10).lean(36).drop(0.42F).legs(-64, 20)
                .key(8, S).m(-44, 0, 10).o(-44, 0, -10).lean(32).drop(0.38F).legs(-60, 18)
                .key(14, S).m(GUARD_M).o(GUARD_O).lean(0).drop(0).legs(0, 0);

        // The Feral Leap: a long running stride into the air, the off arm reaching forward.
        define(GestureId.WOLVERINE_FERAL_LEAP)
                .key(0, S).m(REST_M).o(REST_O).lean(0).legs(0, 0)
                .key(2, O).m(40, 0, 16).o(-128, 0, -14).lean(24).legs(-64, 34)
                .key(10, S).m(36, 0, 16).o(-120, 0, -14).lean(20).legs(-58, 30)
                .key(14, S).m(REST_M).o(REST_O).lean(0).legs(0, 0);

        // The Rage: the head thrown back, the arms wide and shaking, a roar; then the hunch of his Rage.
        define(GestureId.WOLVERINE_BERSERK_ROAR)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(6, O).m(18, 0, 64).o(18, 0, -64).head(-40, 0, 0).lean(-12).mTremble(2.5F, 1.6F)
                        .oTremble(2.5F, 1.6F)
                .key(22, S).m(20, 0, 66).o(20, 0, -66).head(-38, 0, 0).lean(-12).mTremble(2.5F, 1.6F)
                        .oTremble(2.5F, 1.6F)
                .key(30, S).m(RAGE_M).o(RAGE_O).head(0, 0, 0).lean(RAGE_LEAN);

        // The Sniff: the chin up, the head turning one way, the other, back.
        define(GestureId.WOLVERINE_SNIFF)
                .key(0, S).head(0, 0, 0).m(REST_M)
                .key(5, O).head(-24, -28, 0).m(-12, 0, 6)
                .key(13, S).head(-26, 26, 0)
                .key(21, S).head(-20, 0, 0)
                .key(30, S).head(0, 0, 0).m(REST_M);

        // Track: the head down a moment, then the main hand points along his look.
        define(GestureId.WOLVERINE_TRACK)
                .key(0, S).head(0, 0, 0).m(REST_M)
                .key(4, O).head(16, 0, 0).m(-20, 0, 8)
                .key(8, O).head(-6, 0, 0).m(-88, 0, 4)
                .key(16, S).head(0, 0, 0).m(REST_M);

        // Claw Climb on: a claw hooked high, then the other.
        define(GestureId.WOLVERINE_CLIMB)
                .key(0, S).m(REST_M).o(REST_O)
                .key(3, O).m(CLIMB_M).o(-120, 0, -12)
                .key(7, O).m(-120, 0, 12).o(CLIMB_O)
                .key(12, S).m(CLIMB_M).o(CLIMB_O);
    }

    /** The gesture a payload of his starts, or null (the claws' out and in are the toggle's, the Rage the state's). */
    @Nullable
    public static GestureId gestureOf(int kind, int arg) {
        return switch (kind) {
            case WolverineFxPayload.SLASH -> arg == 0 ? GestureId.WOLVERINE_SLASH_1 : arg == 1
                    ? GestureId.WOLVERINE_SLASH_2 : GestureId.WOLVERINE_SLASH_3;
            case WolverineFxPayload.SPIN -> GestureId.WOLVERINE_SPIN;
            case WolverineFxPayload.POUNCE -> arg == 0 ? GestureId.WOLVERINE_POUNCE_AIR
                    : GestureId.WOLVERINE_FERAL_LEAP;
            case WolverineFxPayload.POUNCE_HIT -> arg > 0 ? GestureId.WOLVERINE_POUNCE_STAB : null;
            case WolverineFxPayload.SNIFF -> GestureId.WOLVERINE_SNIFF;
            default -> null;
        };
    }

    private WolverineGestures() {
    }
}
