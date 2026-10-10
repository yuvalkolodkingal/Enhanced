package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.B;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.BOLT;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.HELD;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the gestures of the spellcasters who work with their hands: the Scarlet Witch (her beam, her shield and her
 * Hex), Billy (his bolt, his freeze, the Noise) and the telekinesis the three of them and Magneto share, each with a style
 * of his or her own: Magneto's steady open hand, the Witch's raised shaking claw, Billy's palm held out to stop things.
 *
 * <p>Where a gesture hands over to a stance, its last key is the stance's own pose ({@link StancePoses}).
 */
final class WitchGestures {

    static void register() {
        telekinesis();
        wanda();
        billy();
    }

    // ---- Telekinesis ----

    private static void telekinesis() {
        // Grabbing: the hand comes up, then the arm aims at the thing and holds it.
        define(GestureId.TK_GRAB_MAGNETO)
                .key(0, S).m(-10, 0, 5).lean(0)
                .key(3, O).mAim(HELD, -8, 0, 0).o(-12, 0, -28).lean(2)
                .key(12, S).mAim(HELD, 0, 0, 0).o(-6, 0, -22);
        define(GestureId.TK_GRAB_WANDA)
                .key(0, S).m(-40, 15, 10)
                .key(2, O).mAim(HELD, -35, -6, 0)
                .key(10, S).mAim(HELD, -20, -6, 0).mTremble(2.9F, 0.9F);
        define(GestureId.TK_GRAB_BILLY)
                .key(0, S).m(-20, 0, 8)
                .key(3, O).mAim(HELD, -4, -4, 0).o(-6, 0, -20)
                .key(12, S).mAim(HELD, -4, -4, 0).mTremble(1.7F, 0.9F);

        // Setting it down, or letting it drop: the hand opens and lifts, then the arm lowers.
        define(GestureId.TK_RELEASE)
                .key(0, S).mAim(LOOK, -6, 0, 0)
                .key(3, O).mAim(LOOK, -22, 0, 0);

        // Throwing it: the arm whips down the line of the throw (the stronger the hero, the harder).
        fling(GestureId.TK_FLING_MAGNETO, 0.6F);
        fling(GestureId.TK_FLING_BILLY, 0.8F);
        fling(GestureId.TK_FLING_WANDA, 1.0F);
    }

    private static void fling(GestureId id, float g) {
        define(id)
                .key(0, S).mAim(LOOK, -28 * g, 0, 0).twist(-10 * g).lean(0)
                .key(2, O).mAim(LOOK, 12 * g, 0, 0).twist(8 * g).lean(3 * g)
                .key(10, S).mAim(LOOK, 0, 0, 0).twist(0).lean(0);
    }

    // ---- The Scarlet Witch ----

    private static void wanda() {
        // The beam begins: the hands snap out from the sides and settle on the look.
        define(GestureId.WANDA_BEAM_KICK)
                .key(0, S).m(-20, 0, 25).o(-20, 0, -25).lean(-6)
                .key(2, O).mAim(LOOK, -6, -10, 0).oAim(LOOK, -6, 10, 0).lean(2)
                .key(8, S).mAim(LOOK, 0, -10, 0).oAim(LOOK, 0, 10, 0).lean(0);

        // The wind-up of the Chaos Shield: the arms gather across the chest, then are thrown open, exactly into the stance.
        define(GestureId.WANDA_SHIELD_RAISE)
                .key(0, S).m(-8, 0, 10).o(-8, 0, -10).lean(0).head(0, 0, 0)
                .key(4, E).m(-72, -38, 0).o(-72, 38, 0).lean(3)
                .key(8, S).m(-78, -40, 0).o(-78, 40, 0).lean(3)
                .key(15, B).m(StancePoses.SHIELD_WANDA_M).o(StancePoses.SHIELD_WANDA_O).head(StancePoses.SHIELD_WANDA_HEAD[0], 0, 0)
                .lean(StancePoses.SHIELD_WANDA_LEAN)
                .key(18, S).m(StancePoses.SHIELD_WANDA_M).o(StancePoses.SHIELD_WANDA_O).head(StancePoses.SHIELD_WANDA_HEAD[0], 0, 0)
                .lean(StancePoses.SHIELD_WANDA_LEAN);

        // When the shield comes up at once (the server has no wind-up): a short snap into the stance.
        define(GestureId.WANDA_SHIELD_SNAP)
                .key(0, S).m(-20, 0, 14).o(-20, 0, -14)
                .key(4, B).m(StancePoses.SHIELD_WANDA_M).o(StancePoses.SHIELD_WANDA_O).head(StancePoses.SHIELD_WANDA_HEAD[0], 0, 0)
                .lean(StancePoses.SHIELD_WANDA_LEAN)
                .key(8, S).m(StancePoses.SHIELD_WANDA_M).o(StancePoses.SHIELD_WANDA_O).head(StancePoses.SHIELD_WANDA_HEAD[0], 0, 0)
                .lean(StancePoses.SHIELD_WANDA_LEAN);

        // The shield drops: a push, then the arms fall.
        define(GestureId.WANDA_SHIELD_DROP)
                .key(0, S).m(StancePoses.SHIELD_WANDA_M).o(StancePoses.SHIELD_WANDA_O)
                .key(4, O).m(-25, 0, 12).o(-25, 0, -12).lean(2)
                .key(10, S).m(-25, 0, 12).o(-25, 0, -12).lean(2);

        // The Hex rises: arms overhead, trembling, the body lifting a hand's breadth as the wall grows around her.
        define(GestureId.WANDA_HEX_RISE)
                .key(0, S).m(-8, 0, 12).o(-8, 0, -12).lean(0).drop(0)
                .key(8, S).m(-150, 8, 24).o(-150, -8, -24).head(-10, 0, 0).lean(-3)
                .key(24, E).m(-172, 0, 20).mTremble(2.9F, 1.7F).o(-172, 0, -20).oTremble(2.9F, 1.7F).head(-12, 0, 0).lean(-4).drop(-0.04F)
                .key(36, S).m(-165, 0, 26).mTremble(2.9F, 1.7F).o(-165, 0, -26).oTremble(2.9F, 1.7F).drop(-0.02F)
                .key(44, S).m(-140, 0, 24).o(-140, 0, -24).drop(0);

        // The Hex dissolves: the hands come together in front of her.
        define(GestureId.WANDA_HEX_DISSOLVE)
                .key(0, S).m(-165, 0, 26).o(-165, 0, -26).lean(0)
                .key(8, O).m(-62, -26, 0).o(-62, 26, 0).lean(3)
                .key(16, S).m(-40, -20, 0).o(-40, 20, 0).lean(0);

        // Changing who may cross: one hand conducts, like a snap of the fingers.
        define(GestureId.WANDA_HEX_TWEAK)
                .key(0, S).m(-95, -15, 0).o(-30, 0, -15)
                .key(4, O).m(-112, 5, 8)
                .key(8, O).m(-90, -20, 0)
                .key(12, O).m(-105, 0, 6)
                .key(14, S).m(-105, 0, 6);

        // Making the wall bigger: the hands move apart. Smaller: they come together.
        define(GestureId.WANDA_HEX_GROW)
                .key(0, S).m(-95, -28, 0).o(-95, 28, 0)
                .key(10, O).m(-95, 34, 0).o(-95, -34, 0)
                .key(16, S).m(-95, 34, 0).o(-95, -34, 0);
        define(GestureId.WANDA_HEX_SHRINK)
                .key(0, S).m(-95, 34, 0).o(-95, -34, 0)
                .key(10, O).m(-95, -28, 0).o(-95, 28, 0)
                .key(16, S).m(-95, -28, 0).o(-95, 28, 0);
    }

    // ---- Billy ----

    private static void billy() {
        // The Hex Bolt: the hand pulled back, then thrust at the target with a little kick.
        define(GestureId.BILLY_BOLT)
                .key(0, S).m(-60, 0, 6)
                .key(1, O).mAim(BOLT, -10, -3, 0).o(-6, 0, -14)
                .key(3, S).mAim(BOLT, 0, -3, 0).o(-6, 0, -14)
                .key(8, S).mAim(BOLT, 0, -3, 0);

        // The freeze: both hands thrown up toward the creatures, then the trembling stance.
        define(GestureId.BILLY_FREEZE_CAST)
                .key(0, S).m(-10, 0, 10).o(-10, 0, -10).head(0, 0, 0)
                .key(5, O).m(-130, 8, 14).o(-130, -8, -14).head(-6, 0, 0)
                .key(22, S).m(StancePoses.BILLY_FREEZE_M).o(StancePoses.BILLY_FREEZE_O);

        // The Noise: both hands pressed to the sides of his head, then released.
        define(GestureId.BILLY_NOISE_ON)
                .key(0, S).m(-10, 0, 6).o(-10, 0, -6)
                .key(5, O).m(-150, 20, 0).o(-150, -20, 0).head(0, 0, 0)
                .key(10, S).m(-150, 20, 0).o(-150, -20, 0)
                .key(14, S).m(-10, 0, 6).o(-10, 0, -6);
        // He shakes the noise out of his head.
        define(GestureId.BILLY_NOISE_OFF)
                .key(0, S).head(0, 0, 0)
                .key(2, O).head(0, 10, 0)
                .key(4, O).head(0, -10, 0)
                .key(6, O).head(0, 6, 0)
                .key(8, S).head(0, 0, 0);
    }

    private WitchGestures() {
    }
}
