package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the gestures of the two heroes who command fields rather than throw spells: Magneto, who draws his
 * magnetic field out around him, rips metal away and pulls it to him, and Vision, who is serene (White Vision mechanical: his arms move
 * with one easing only, see {@link Gestures.Def#sampleArm}).
 *
 * <p>Where a gesture hands over to a stance, its last key is the stance's own pose ({@link StancePoses}), so nothing pops.
 */
final class FieldGestures {

    static void register() {
        magneto();
        vision();
    }

    private static void magneto() {
        // The shrapnel storm begins: the hand comes up and thrusts at what he looks at.
        define(GestureId.MAG_SHRAPNEL_START)
                .key(0, S).m(-10, 0, 10).lean(0)
                .key(2, O).mAim(LOOK, -8, 0, 0).lean(-3)
                .key(6, S).mAim(LOOK, 0, -2, 0).lean(-2);

        // The armour and weapon strip: the claw closes on the metal, then the wrench, the other arm flung out.
        define(GestureId.MAG_DISARM)
                .key(0, S).mAim(LOOK, -6, 0, 0).o(0, 0, -6).head(-4, 0, 0).lean(0)
                .key(3, S).mAim(LOOK, -6, 0, 0)
                .key(6, O).mAim(LOOK, 38, 0, 0).o(-20, 0, -35).lean(-6)
                .key(14, S).mAim(LOOK, 38, 0, 0).o(-20, 0, -35).lean(-6);

        // He draws the field out around him: arms swept out and up, then settling into the field's pose.
        define(GestureId.MAG_SHIELD_ON)
                .key(0, S).m(-6, 0, 18).o(-6, 0, -18)
                .key(5, O).m(-100, 12, 50).o(-100, -12, -50).head(-5, 0, 0)
                .key(11, E).m(-25, 0, 45).o(-25, 0, -45)
                .key(18, S).m(StancePoses.MAG_FIELD_M).o(StancePoses.MAG_FIELD_O).head(0, 0, 0);

        // He lets the field go: a flick outward, then the arms fall.
        define(GestureId.MAG_SHIELD_OFF)
                .key(0, S).m(StancePoses.MAG_FIELD_M).o(StancePoses.MAG_FIELD_O)
                .key(2, O).m(-10, 0, 30).o(-10, 0, -30)
                .key(5, S).m(0, 0, 5).o(0, 0, -5)
                .key(10, S).m(0, 0, 5).o(0, 0, -5);

        // The Magnetic Pull: both arms flung wide and up, palms out, as his field reaches round him; then both fists drawn
        // in to the chest and a lean back as the metal comes to him; held while it flies, then let go into the stance.
        define(GestureId.MAG_PULL)
                .key(0, S).m(-8, 0, 10).o(-8, 0, -10).head(0, 0, 0).lean(0)
                .key(5, O).m(-125, 28, 62).o(-125, -28, -62).head(-8, 0, 0).lean(-1)
                .key(12, E).m(-74, -40, 0).o(-74, 40, 0).head(4, 0, 0).lean(-4)
                .key(16, S).m(-74, -40, 0).o(-74, 40, 0).head(4, 0, 0).lean(-4)
                .key(20, S).m(-8, 0, 10).o(-8, 0, -10).head(0, 0, 0).lean(0);
    }

    private static void vision() {
        // The temple: fingers to the Mind Stone as the beam builds (White Vision has none).
        define(GestureId.VISION_FOCUS)
                .key(0, S).m(-10, 0, 6).head(0, 0, 0)
                .key(4, O).m(-168, 0, 2).head(0, 0, -4)
                .key(8, S).m(-168, 0, 2)
                .key(12, S).m(-168, 0, 2);

        // He lets go of the world: arms float wide, palms down, then into the phased pose.
        define(GestureId.VISION_PHASE_IN)
                .key(0, S).m(2, 0, 8).o(2, 0, -8)
                .key(8, E).m(-30, 0, 45).o(-30, 0, -45).head(-6, 0, 0)
                .key(16, S).m(StancePoses.VISION_PHASED_M).o(StancePoses.VISION_PHASED_O).head(0, 0, 0);

        // He solidifies: the arms snap in and a small settle.
        define(GestureId.VISION_PHASE_OUT)
                .key(0, S).m(StancePoses.VISION_PHASED_M).o(StancePoses.VISION_PHASED_O).drop(0)
                .key(4, O).m(5, 0, 3).o(5, 0, -3).drop(0.03F)
                .key(10, S).m(5, 0, 3).o(5, 0, -3).drop(0);

        // He becomes heavy: a brace, the body sinks, then the dense stance.
        define(GestureId.VISION_DENSE_ON)
                .key(0, S).m(0, 0, 6).o(0, 0, -6).drop(0)
                .key(4, O).m(-30, 0, 30).o(-30, 0, -30).drop(0.05F).head(4, 0, 0)
                .key(14, S).m(StancePoses.VISION_DENSE_M).o(StancePoses.VISION_DENSE_O).drop(0.03F);

        // The weight goes: the arms lift and the body rises a little.
        define(GestureId.VISION_DENSE_OFF)
                .key(0, S).m(StancePoses.VISION_DENSE_M).o(StancePoses.VISION_DENSE_O).drop(0.03F)
                .key(3, O).m(-24, 0, 12).o(-24, 0, -12).drop(-0.02F)
                .key(8, S).m(0, 0, 6).o(0, 0, -6).drop(0);

        // A hand wipes down his face: the human disguise.
        define(GestureId.VISION_DISGUISE)
                .key(0, S).m(-10, 0, 6).head(0, 0, 0)
                .key(3, O).m(-150, -28, 0)
                .key(8, E).m(-55, -30, 0).head(6, 0, 0)
                .key(12, S).m(-10, 0, 6).head(0, 0, 0);
    }

    private FieldGestures() {
    }
}
