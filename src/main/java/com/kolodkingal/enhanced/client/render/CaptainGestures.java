package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.network.CaptainFxPayload;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Captain America's gestures (3.8): a trained soldier's economy, square and strong. The keys are timed to
 * the server's moves: the shield leaves the hand at the throw's fling (tick 6), the slam's hop lasts until he lands
 * ({@code ShieldSlam}), the charge runs {@code ShieldCharge.CHARGE_TICKS} 12, a punch lands at once (its key at tick
 * 3). Pure data (no Minecraft client classes), so the game tests can check it.
 */
public final class CaptainGestures {
    /** The arms at rest: {@code M}, then {@code O}. */
    public static final float[] REST_M = {0, 0, 4};
    public static final float[] REST_O = {0, 0, -4};
    /** Behind the shield in the charge: the shield arm across before the chest, the fist low. */
    public static final float[] CHARGE_M = {-38, 8, 18};
    public static final float[] CHARGE_O = {-86, -26, 14};
    public static final float CHARGE_LEAN = 22;
    /** The guard: a little crouch behind the raised shield, leaning in. */
    public static final float GUARD_LEAN = 8;
    public static final float GUARD_DROP = 0.06F;

    static void register() {
        // The throw from the main hand: drawn back across the chest (tick 4), flung flat out (tick 6).
        define(GestureId.CAP_THROW)
                .key(0, S).m(REST_M).twist(0).lean(0)
                .key(4, O).m(-76, 70, -24).twist(24).lean(-2)
                .key(6, I).m(-92, -46, 26).twist(-20).lean(6)
                .key(12, S).m(REST_M).twist(0).lean(0);
        // From the off hand: the mirror.
        define(GestureId.CAP_THROW_OFF)
                .key(0, S).o(REST_O).twist(0).lean(0)
                .key(4, O).o(-76, -70, 24).twist(-24).lean(-2)
                .key(6, I).o(-92, 46, -26).twist(20).lean(6)
                .key(12, S).o(REST_O).twist(0).lean(0);
        // The catch: the arm out to meet it, the shield slaps on and gives a little.
        define(GestureId.CAP_CATCH)
                .key(0, S).o(-70, 0, -16)
                .key(2, O).o(-88, 6, -22)
                .key(4, I).o(-66, 14, -10).oTremble(1.2F, 2.0F)
                .key(10, S).o(REST_O);
        define(GestureId.CAP_CATCH_MAIN)
                .key(0, S).m(-70, 0, 16)
                .key(2, O).m(-88, -6, 22)
                .key(4, I).m(-66, -14, 10).mTremble(1.2F, 2.0F)
                .key(10, S).m(REST_M);
        // The slam's hop: a crouch, then up with the shield lifted over his head.
        define(GestureId.CAP_SLAM_LEAP)
                .key(0, S).m(REST_M).o(REST_O).drop(0).legs(0, 0)
                .key(2, O).m(-40, 0, 12).o(-40, 0, -12).drop(0.18F).legs(-30, -30)
                .key(5, E).m(-168, 0, 10).o(-168, 0, -10).drop(0).legs(-24, -16)
                .key(8, S).m(-170, 0, 10).o(-170, 0, -10).legs(-30, -20);
        // The landing: the shield driven down, one knee down, then up again.
        define(GestureId.CAP_SLAM_DOWN)
                .key(0, S).m(-170, 0, 10).o(-170, 0, -10).drop(0).lean(0).legs(0, 0)
                .key(2, I).m(-36, 0, 12).o(-44, 0, -10).drop(0.42F).lean(30).legs(-72, 28)
                .key(8, S).m(-34, 0, 12).o(-42, 0, -10).drop(0.38F).lean(26).legs(-68, 26)
                .key(14, S).m(REST_M).o(REST_O).drop(0).lean(0).legs(0, 0);
        // The Super-Soldier Leap: a long running bound, one knee up, the arms swinging.
        define(GestureId.CAP_LEAP)
                .key(0, S).m(REST_M).o(REST_O).lean(0).legs(0, 0)
                .key(2, O).m(42, 0, 14).o(-112, 0, -14).lean(18).legs(-72, 30)
                .key(10, S).m(36, 0, 14).o(-104, 0, -14).lean(14).legs(-60, 26)
                .key(14, S).m(REST_M).o(REST_O).lean(0).legs(0, 0);
        // The charge: the shoulder behind the shield, leaning in, the head down (it holds through the run).
        define(GestureId.CAP_CHARGE)
                .key(0, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0).drop(0)
                .key(3, O).m(CHARGE_M).o(CHARGE_O).lean(CHARGE_LEAN).head(16, 0, 0).drop(0.06F)
                .key(12, S).m(CHARGE_M).o(CHARGE_O).lean(CHARGE_LEAN).head(16, 0, 0).drop(0.06F)
                .key(16, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0).drop(0);
        // The bash: the shield drawn to the shoulder, then punched out.
        define(GestureId.CAP_BASH)
                .key(0, S).o(REST_O).lean(0).twist(0)
                .key(2, O).o(-34, -14, 14).lean(-4).twist(-14)
                .key(4, I).o(-96, -16, 6).lean(12).twist(12)
                .key(10, S).o(REST_O).lean(0).twist(0);
        // Assemble: the shield raised high, the other hand waving them on.
        define(GestureId.CAP_ASSEMBLE)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(5, O).m(-128, -26, 38).o(-174, 0, -8).head(-16, 0, 0)
                .key(11, S).m(-112, -8, 24).o(-176, 0, -8)
                .key(17, S).m(-130, -28, 40).o(-176, 0, -8)
                .key(23, S).m(-114, -8, 26).o(-174, 0, -8).head(-14, 0, 0)
                .key(30, S).m(REST_M).o(REST_O).head(0, 0, 0);
        // Inspire: the arms opened wide, the chest up, the head lifted.
        define(GestureId.CAP_INSPIRE)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(5, O).m(-34, 0, 68).o(-34, 0, -68).head(-20, 0, 0).lean(-6)
                .key(18, S).m(-36, 0, 70).o(-36, 0, -70).head(-22, 0, 0).lean(-6)
                .key(24, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0);
        // The jab: the main fist straight out (tick 3).
        define(GestureId.CAP_PUNCH_1)
                .key(0, S).m(REST_M).twist(0).lean(0)
                .key(2, O).m(-24, 0, 10).twist(-10)
                .key(3, I).m(-92, 4, 4).twist(16).lean(6)
                .key(8, S).m(REST_M).twist(0).lean(0);
        // The hook: the off fist round and across (tick 3).
        define(GestureId.CAP_PUNCH_2)
                .key(0, S).o(REST_O).twist(0).lean(0)
                .key(2, O).o(-32, 0, -32).twist(10)
                .key(3, I).o(-88, -40, 10).twist(-20).lean(6)
                .key(8, S).o(REST_O).twist(0).lean(0);
        // The spinning kick: a whole turn, the right leg out level, the arms out for balance.
        define(GestureId.CAP_KICK)
                .key(0, S).m(REST_M).o(REST_O).yaw(0).legs(0, 0)
                .key(4, O).m(-20, 0, 44).o(-20, 0, -44).legs(-96, 10)
                .key(10, S).yaw(360).legs(-90, 10)
                .key(14, S).m(REST_M).o(REST_O).legs(0, 0);
        // The flip kick: thrown back, the leg kicking high, the arms flung back.
        define(GestureId.CAP_FLIP_KICK)
                .key(0, S).m(REST_M).o(REST_O).lean(0).legs(0, 0).head(0, 0, 0)
                .key(3, O).m(40, 0, 32).o(40, 0, -32).lean(-46).legs(-132, 16).head(-30, 0, 0)
                .key(10, S).m(20, 0, 24).o(20, 0, -24).lean(-26).legs(-60, -20).head(-12, 0, 0)
                .key(16, S).m(REST_M).o(REST_O).lean(0).legs(0, 0).head(0, 0, 0);
    }

    /** The gesture a payload of his starts, or null (a ricochet, a block or a reflection is the shield's own). */
    @Nullable
    public static GestureId gestureOf(int kind, int arg) {
        return switch (kind) {
            case CaptainFxPayload.THROW -> (arg & 1) != 0 ? GestureId.CAP_THROW_OFF : GestureId.CAP_THROW;
            case CaptainFxPayload.CATCH -> arg == 1 ? GestureId.CAP_CATCH : GestureId.CAP_CATCH_MAIN;
            case CaptainFxPayload.SLAM -> arg == 0 ? GestureId.CAP_SLAM_LEAP : GestureId.CAP_SLAM_DOWN;
            case CaptainFxPayload.LEAP -> GestureId.CAP_LEAP;
            case CaptainFxPayload.CHARGE -> arg == 0 ? GestureId.CAP_CHARGE : null;
            case CaptainFxPayload.BASH -> arg == 1 ? null : GestureId.CAP_BASH;
            case CaptainFxPayload.ASSEMBLE -> GestureId.CAP_ASSEMBLE;
            case CaptainFxPayload.INSPIRE -> GestureId.CAP_INSPIRE;
            case CaptainFxPayload.PUNCH -> arg == 1 ? GestureId.CAP_PUNCH_2 : GestureId.CAP_PUNCH_1;
            case CaptainFxPayload.KICK -> GestureId.CAP_KICK;
            case CaptainFxPayload.FLIP -> GestureId.CAP_FLIP_KICK;
            default -> null;
        };
    }

    private CaptainGestures() {
    }
}
