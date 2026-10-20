package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Red Hulk's gestures (3.8, lane L2). He is drawn by the Hulk's body and renderer, so what he shares with
 * the Hulk plays the Hulk's own gestures ({@link HulkGestures}: the Smash, timed to the same wind-up of 6 ticks, the
 * Roar, the Grab Smash and the Bound, which the Hulk's code swings and flies for him, the landing); these are only what
 * differs: the Heat Wave, the Super Jump's crouch and launch, the Anger Burst, the Energy Absorb, the change between
 * Ross and Red Hulk ({@code transformTicks}: 30) and Ross straightening his suit. Pure data (no Minecraft client
 * classes), so the game tests can check it.
 */
public final class RedHulkGestures {
    /** The arms at rest (wide of his bulk, as the Hulk's): {@code M}, then {@code O}. */
    public static final float[] REST_M = HulkGestures.REST_M;
    public static final float[] REST_O = HulkGestures.REST_O;
    /** Steaming hot: hunched lower than the Hulk, the fists clenched forward and out. */
    public static final float[] HEATED_M = {-30, 12, 30};
    public static final float[] HEATED_O = {-30, -12, -30};
    public static final float HEATED_LEAN = 16;
    /** The absorb: the fists drawn in before the chest, the elbows out. */
    public static final float[] ABSORB_M = {-70, -40, 20};
    public static final float[] ABSORB_O = {-70, 40, -20};
    /** The charge: the knuckles down to the ground before him. */
    public static final float[] CHARGE_M = {-28, 0, 20};
    public static final float[] CHARGE_O = {-28, 0, -20};
    public static final float CHARGE_LEAN = 30;
    public static final float CHARGE_DROP = 0.42F;
    public static final float CHARGE_LEGS = -62;
    /** In the air: the arms up and wide, the knees tucked. */
    public static final float[] AIR_M = {-160, 0, 30};
    public static final float[] AIR_O = {-160, 0, -30};
    public static final float AIR_LEG_R = -60;
    public static final float AIR_LEG_L = -35;

    static void register() {
        // The Heat Wave: the arms flung wide and the chest out (tick 5), then both palms driven forward as the heat
        // rolls out, held, and back.
        define(GestureId.RED_HULK_HEAT_WAVE)
                .key(0, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0)
                .key(5, O).m(-20, -30, 100).o(-20, 30, -100).lean(-14).head(-16, 0, 0)
                .key(8, I).m(-90, -18, 4).o(-90, 18, -4).lean(16).head(4, 0, 0)
                .key(15, S).m(-88, -16, 4).o(-88, 16, -4).lean(14).head(4, 0, 0).mTremble(1.5F, 1.6F)
                        .oTremble(1.5F, 1.6F)
                .key(20, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0);

        // The Super Jump's crouch: down onto his haunches, the knuckles to the ground (the stance RED_HULK_CHARGING
        // takes over at the same pose).
        define(GestureId.RED_HULK_JUMP_CROUCH)
                .key(0, S).m(REST_M).o(REST_O).drop(0).lean(0).legs(0, 0)
                .key(5, O).m(CHARGE_M).o(CHARGE_O).drop(CHARGE_DROP).lean(CHARGE_LEAN).legs(CHARGE_LEGS, CHARGE_LEGS)
                .key(8, S).m(CHARGE_M).o(CHARGE_O).drop(CHARGE_DROP).lean(CHARGE_LEAN).legs(CHARGE_LEGS, CHARGE_LEGS);

        // The launch: up out of the crouch, the arms thrown high (the stance RED_HULK_AIR takes over).
        define(GestureId.RED_HULK_JUMP)
                .key(0, S).m(CHARGE_M).o(CHARGE_O).drop(CHARGE_DROP).lean(CHARGE_LEAN).legs(CHARGE_LEGS, CHARGE_LEGS)
                .key(2, E).m(-170, 0, 16).o(-170, 0, -16).drop(0).lean(-8).legs(10, 10)
                .key(10, S).m(AIR_M).o(AIR_O).drop(0).lean(0).legs(AIR_LEG_R, AIR_LEG_L);

        // The Anger Burst: he gathers (the fists to the chest, tick 4), then throws the head back and the arms out and
        // down as the ring of fire goes (tick 6), shaking with it, and comes back.
        define(GestureId.RED_HULK_BURST)
                .key(0, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0).drop(0)
                .key(4, O).m(-70, -40, 20).o(-70, 40, -20).lean(18).head(14, 0, 0).drop(0.18F)
                .key(6, E).m(10, 0, 70).o(10, 0, -70).lean(-16).head(-40, 0, 0).drop(0)
                .key(18, S).m(12, 0, 72).o(12, 0, -72).lean(-14).head(-38, 0, 0).mTremble(3.0F, 1.8F)
                        .oTremble(3.0F, 1.8F)
                .key(24, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0);

        // The Energy Absorb begins: the feet planted, the fists drawn in (the stance RED_HULK_ABSORBING holds it).
        define(GestureId.RED_HULK_ABSORB)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0)
                .key(4, O).m(-40, 0, 60).o(-40, 0, -60).lean(-6).drop(0.08F)
                .key(10, S).m(ABSORB_M).o(ABSORB_O).lean(6).drop(0.12F);

        // Ross grows into Red Hulk over 30 ticks: doubled over, clenched, trembling as he grows, then up and roaring.
        define(GestureId.RED_HULK_TRANSFORM_OUT)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0)
                .key(5, O).m(14, 0, 24).o(14, 0, -24).lean(30).drop(0.22F).head(24, 0, 0)
                .key(16, S).m(-30, 0, 40).o(-30, 0, -40).lean(22).drop(0.16F).head(12, 0, 0).mTremble(3.0F, 1.8F)
                        .oTremble(3.0F, 1.8F)
                .key(23, E).m(16, 0, 50).o(16, 0, -50).lean(-12).drop(0).head(-36, 0, 0)
                .key(27, S).m(16, 0, 48).o(16, 0, -48).lean(-12).head(-34, 0, 0)
                .key(30, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0);

        // Red Hulk shrinks into Ross: to one knee, the hands on the knee, the head bowed, then up.
        define(GestureId.RED_HULK_TRANSFORM_IN)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0).legs(0, 0).head(0, 0, 0)
                .key(8, O).m(-44, 0, 10).o(-44, 0, -10).lean(26).drop(0.45F).legs(-80, 12).head(26, 0, 0)
                .key(25, S).m(-42, 0, 10).o(-42, 0, -10).lean(24).drop(0.44F).legs(-80, 12).head(24, 0, 0)
                .key(30, S).m(REST_M).o(REST_O).lean(0).drop(0).legs(0, 0).head(0, 0, 0);

        // Ross again: both hands to the lapels, a tug down on the jacket, a touch at the tie, the chin up.
        define(GestureId.RED_HULK_ROSS)
                .key(0, S).m(0, 0, 0).o(0, 0, 0).head(0, 0, 0)
                .key(4, O).m(-58, -38, 0).o(-58, 38, 0).head(10, 0, 0)
                .key(8, S).m(-40, -30, 0).o(-40, 30, 0).head(8, 0, 0)
                .key(11, S).m(-62, -44, 0).o(-6, 0, -4).head(-6, 0, 0)
                .key(16, S).m(0, 0, 0).o(0, 0, 0).head(0, 0, 0);
    }

    /** The gesture a payload of his starts, or null (the Hulk's own payloads start the grab's and the Bound's). */
    @Nullable
    public static GestureId gestureOf(int kind, int arg) {
        return switch (kind) {
            case RedHulkFxPayload.SMASH -> arg == 0 ? GestureId.HULK_SMASH : null;
            case RedHulkFxPayload.HEAT_WAVE -> GestureId.RED_HULK_HEAT_WAVE;
            case RedHulkFxPayload.ROAR -> GestureId.HULK_ROAR;
            case RedHulkFxPayload.JUMP -> GestureId.RED_HULK_JUMP;
            case RedHulkFxPayload.LAND -> GestureId.HULK_LAND;
            case RedHulkFxPayload.BURST -> GestureId.RED_HULK_BURST;
            case RedHulkFxPayload.TRANSFORM_OUT -> arg > 0 ? GestureId.RED_HULK_TRANSFORM_OUT : null;
            case RedHulkFxPayload.TRANSFORM_IN -> arg > 0 ? GestureId.RED_HULK_TRANSFORM_IN : null;
            default -> null;
        };
    }

    private RedHulkGestures() {
    }
}
