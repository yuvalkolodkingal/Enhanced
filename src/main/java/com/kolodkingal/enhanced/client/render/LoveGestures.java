package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Love's gestures (3.8): Thor's throw and catch for a girl of ten (the same arcs, a little quicker), the
 * axe raised to the sky for the Thunder Strike, the kick of her lightning, and the Eye Laser's chin thrust forward with
 * the fists at her sides. Pure data (no Minecraft client classes), so the game tests can check it.
 */
public final class LoveGestures {
    /** The Eye Laser's hold: the fists pulled down and a little back, the chin forward (head pitch). */
    public static final float[] EYES_M = {12, 0, 10};
    public static final float[] EYES_O = {12, 0, -10};
    public static final float EYES_HEAD = 10;

    static void register() {
        // The throw: the axe hauled back over the shoulder, then whipped forward with the whole body (tick 2).
        define(GestureId.LOVE_THROW)
                .key(0, S).mWorld(150, -6, 0).lead().twist(-14).lean(0)
                .key(2, O).mAim(LOOK, 0, 0, 0).twist(10).lean(6)
                .key(5, S).mAim(LOOK, 0, 0, 0)
                .key(12, S).mWorld(60, 0, 0).twist(0).lean(0);

        // The catch: the hand reaches up and forward, the axe slaps in, the arm gives with it.
        define(GestureId.LOVE_CATCH)
                .key(0, S).mAim(LOOK, 10, 0, 0).lead().lean(0).twist(0)
                .key(2, O).mAim(LOOK, 30, 0, 0).lean(-4).twist(-6)
                .key(5, S).mAim(LOOK, 12, 0, 0).lean(0).twist(0)
                .key(12, S).mWorld(60, 0, 0);

        // The Thunder Strike: the axe raised straight to the sky (tick 3, the bolt falls), held, then swung down.
        define(GestureId.LOVE_STRIKE)
                .key(0, S).m(-20, 0, 8).o(-10, 0, -8).head(0, 0, 0).lean(0)
                .key(3, O).m(-178, 0, 6).o(-30, 0, -24).head(-16, 0, 0).lean(-6)
                .key(9, S).m(-176, 0, 8).o(-30, 0, -24).head(-14, 0, 0).lean(-5)
                .key(12, O).m(-70, 0, 4).o(-14, 0, -12).head(4, 0, 0).lean(8)
                .key(16, S).m(-20, 0, 8).o(-10, 0, -8).head(0, 0, 0).lean(0);

        // The lightning leaves her hand: the arm kicks up, the body twists, then the arm points down the look.
        define(GestureId.LOVE_LIGHTNING)
                .key(0, S).mAim(LOOK, 14, 0, 0).twist(-14).lean(-5)
                .key(2, O).mAim(LOOK, 0, 0, 0).twist(12).lean(0)
                .key(8, S).mAim(LOOK, 0, 0, 0).twist(12);

        // The Eye Laser: a jolt of the head back as the eyes light, then the chin forward, the fists down at her sides.
        define(GestureId.LOVE_EYE_LASER)
                .key(0, S).m(0, 0, 6).o(0, 0, -6).head(0, 0, 0).lean(0)
                .key(2, O).m(20, 0, 16).o(20, 0, -16).head(-12, 0, 0).lean(-4)
                .key(10, S).m(EYES_M).o(EYES_O).head(EYES_HEAD, 0, 0).lean(3);
    }

    private LoveGestures() {
    }
}
