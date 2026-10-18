package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Cyclops's gestures (3.3): a soldier's economy of movement. The hand that goes to the visor goes to exactly
 * the pose of the Optic Blast's stance ({@link StancePoses#CYCLOPS_BLAST_M}), so a ricochet fired while the blast is held,
 * or the glasses touched, never jumps; every gesture starts and ends on the arms at rest. Pure data (no Minecraft client
 * classes), so the game tests can check it.
 */
final class CyclopsGestures {
    /** The arms at rest: {@code M}, then {@code O}. */
    static final float[] REST_M = {0, 0, 4};
    static final float[] REST_O = {0, 0, -4};

    static void register() {
        // The Ricochet: two fingers snap to the visor, hold while the bolt leaves, the hand drops.
        define(GestureId.CYCLOPS_RICOCHET)
                .key(0, S).m(REST_M)
                .key(3, O).m(StancePoses.CYCLOPS_BLAST_M)
                .key(8, S).m(StancePoses.CYCLOPS_BLAST_M)
                .key(14, S).m(REST_M);

        // The Concussion Blast: both hands to the visor, the blast throws his shoulders back, and he straightens.
        define(GestureId.CYCLOPS_CONCUSSION)
                .key(0, S).m(REST_M).o(REST_O).lean(0).legs(0, 0)
                .key(3, O).m(StancePoses.CYCLOPS_UNLEASHED_M).o(StancePoses.CYCLOPS_UNLEASHED_O).lean(3).legs(-8, 8)
                .key(6, O).m(StancePoses.CYCLOPS_UNLEASHED_M).o(StancePoses.CYCLOPS_UNLEASHED_O).lean(-6).legs(-10, 10)
                .key(16, S).m(REST_M).o(REST_O).lean(0).legs(0, 0);

        // The Optic Leap: a crouch with the arms swung back, then flung up and forward as he is thrown into the air.
        define(GestureId.CYCLOPS_LEAP)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0)
                .key(3, O).m(38, 0, 10).o(38, 0, -10).lean(14).drop(0.18F)
                .key(7, O).m(-150, 0, 18).o(-150, 0, -18).lean(-6).drop(0)
                .key(12, E).m(-60, 0, 22).o(-60, 0, -22).lean(4)
                .key(18, S).m(REST_M).o(REST_O).lean(0);

        // The Rally: the main fist thrust up over his head, held while he calls, brought down.
        define(GestureId.CYCLOPS_RALLY)
                .key(0, S).m(REST_M).head(0, 0, 0)
                .key(6, O).m(-172, 0, 12).head(-8, 0, 0)
                .key(16, S).m(-168, 0, 10).head(-8, 0, 0)
                .key(24, S).m(REST_M).head(0, 0, 0);

        // The glasses come off: the hand to the visor, a grip, then drawn down and out holding them.
        define(GestureId.CYCLOPS_GLASSES_OFF)
                .key(0, S).m(REST_M)
                .key(5, O).m(StancePoses.CYCLOPS_BLAST_M)
                .key(9, S).m(StancePoses.CYCLOPS_BLAST_M)
                .key(15, O).m(-70, -12, 22)
                .key(24, S).m(REST_M);

        // The glasses go on: the hand brings them up to the face, settles them, and drops.
        define(GestureId.CYCLOPS_GLASSES_ON)
                .key(0, S).m(REST_M)
                .key(4, O).m(StancePoses.CYCLOPS_BLAST_M)
                .key(7, S).m(StancePoses.CYCLOPS_BLAST_M)
                .key(12, S).m(REST_M);
    }

    private CyclopsGestures() {
    }
}
