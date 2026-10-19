package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Doctor Strange's gestures and the poses of his stances (3.6). A surgeon's hands: every spell is drawn by
 * the hands and the fingers, close before the chest, the arms rarely thrown wide (the films' sorcerers of Kamar-Taj); the
 * whip is a flick of the wrist from the hip, the Sling Ring's circle a small, quick sweep of the ring hand, the Eye of
 * Agamotto opened between both hands. No Minecraft client imports: the tables are checked by the game tests.
 */
public final class StrangeGestures {
    // ---- the hover pose of his flight profile (the take-off ends on it) ----
    public static final float[] HOVER_M = {-12, 0, 18};
    public static final float[] HOVER_O = {-12, 0, -18};
    public static final float HOVER_LEG = 3;

    // ---- the stances' poses (main arm, off arm: x pitch, y yaw, z roll in degrees) ----
    /** Casting: both hands up before the chest, the fingers in the mudra. */
    public static final float[] CAST_M = {-62, -26, 8};
    public static final float[] CAST_O = {-62, 26, -8};
    /** The Shield: the off palm pushed out along the look (aimed), the main hand circling beside it. */
    public static final float[] SHIELD_M = {-78, -14, 18};
    /** Levitating: the hands relaxed, a little open; the legs together, the knees a little bent. */
    public static final float[] LEVITATE_M = {-8, 0, 14};
    public static final float[] LEVITATE_O = {-8, 0, -14};
    public static final float LEVITATE_LEG = 8;
    /** The rest the gestures come back to. */
    private static final float[] REST_M = {-8, 0, 8};
    private static final float[] REST_O = {-8, 0, -8};

    static void register() {
        flight();
        whip();
        shields();
        bands();
        portal();
        eye();
    }

    private static void flight() {
        // The Cloak lifts him: the arms float open as he rises, the head lifts, then he settles on the hover pose.
        define(GestureId.TAKEOFF_STRANGE)
                .key(0, S).m(REST_M).o(REST_O).legs(0, 0).head(0, 0, 0)
                .key(5, O).m(-30, 0, 46).o(-30, 0, -46).legs(6, 6).head(-8, 0, 0)
                .key(11, E).m(-20, 0, 28).o(-20, 0, -28).legs(4, 4).head(-3, 0, 0)
                .key(18, S).m(HOVER_M).o(HOVER_O).legs(HOVER_LEG, HOVER_LEG).head(0, 0, 0);
    }

    private static void whip() {
        // The first lash: the hand drawn back past the hip, then flicked out along the look.
        define(GestureId.STRANGE_WHIP_1)
                .key(0, S).m(REST_M).twist(0)
                .key(2, E).m(22, -10, 30).twist(-10)
                .key(5, O).mAim(LOOK, -6, 0, 0).twist(8)
                .key(10, S).m(REST_M).twist(0);
        // The second: a backhand from the far shoulder out to his side.
        define(GestureId.STRANGE_WHIP_2)
                .key(0, S).m(REST_M).twist(0)
                .key(2, E).m(-78, 64, -18).twist(14)
                .key(5, O).m(-70, -46, 40).twist(-12)
                .key(10, S).m(REST_M).twist(0);
        // The third: raised overhead, then cracked down at the creature (it wraps and lifts it).
        define(GestureId.STRANGE_WHIP_3)
                .key(0, S).m(REST_M).lean(0).head(0, 0, 0)
                .key(4, E).m(-170, 8, 14).lean(-4).head(-6, 0, 0)
                .key(7, I).mAim(LOOK, 10, 0, 0).lean(8).head(4, 0, 0)
                .key(10, S).mAim(LOOK, 6, 0, 0).lean(6)
                .key(14, S).m(REST_M).lean(0).head(0, 0, 0);
        // The Grapple: the whip cast at the anchor, then both hands haul on it, the body leaning in.
        define(GestureId.STRANGE_GRAPPLE)
                .key(0, S).m(REST_M).o(REST_O).lean(0)
                .key(3, O).mAim(LOOK, -8, 0, 0).o(REST_O).lean(4)
                .key(8, E).mAim(LOOK, -2, 0, 0).oAim(LOOK, 2, 0, 0).lean(10)
                .key(12, S).m(-60, 10, 10).o(-60, -10, -10).lean(8)
                .key(16, S).m(REST_M).o(REST_O).lean(0);
    }

    private static void shields() {
        // The Shield of the Seraphim: the off palm pushed out as the mandala unfolds (the stance STRANGE_SHIELD holds it).
        define(GestureId.STRANGE_SHIELD_UP)
                .key(0, S).m(REST_M).o(REST_O)
                .key(3, O).m(-50, -30, 10).oAim(LOOK, 6, 4, 0)
                .key(8, S).m(SHIELD_M).oAim(LOOK, 0, 4, 0);
        // The Seraphim Dome: both arms swept wide and up, the head back, as the sphere closes round him.
        define(GestureId.STRANGE_DOME)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(4, E).m(-40, -30, 10).o(-40, 30, -10).head(4, 0, 0)
                .key(9, O).m(-150, 0, 58).o(-150, 0, -58).head(-12, 0, 0).lean(-4)
                .key(16, S).m(-140, 0, 62).o(-140, 0, -62).head(-10, 0, 0).lean(-4)
                .key(22, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0);
    }

    private static void bands() {
        // The Crimson Bands: both hands drawn in, then thrown forward at the creature, the fingers spread.
        define(GestureId.STRANGE_BANDS)
                .key(0, S).m(REST_M).o(REST_O).lean(0)
                .key(3, E).m(-50, -40, 6).o(-50, 40, -6).lean(-3)
                .key(6, O).mAim(LOOK, 0, -6, 0).oAim(LOOK, 0, 6, 0).lean(8)
                .key(9, S).mAim(LOOK, 0, -6, 0).mTremble(1.5F, 2.4F).oAim(LOOK, 0, 6, 0).oTremble(1.5F, 2.4F).lean(6)
                .key(14, S).m(REST_M).o(REST_O).lean(0);
        // The spirit rises out of the body: the arms float open and up, the head thrown back.
        define(GestureId.STRANGE_ASTRAL)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(6, O).m(-40, 0, 50).o(-40, 0, -50).head(-16, 0, 0).lean(-6)
                .key(14, S).m(-30, 0, 44).o(-30, 0, -44).head(-10, 0, 0).lean(-4)
                .key(20, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0);
        // The spirit returns: the arms drawn in to the chest, a short bow.
        define(GestureId.STRANGE_ASTRAL_IN)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(4, O).m(-60, -36, 4).o(-60, 36, -4).head(10, 0, 0)
                .key(12, S).m(REST_M).o(REST_O).head(0, 0, 0);
    }

    private static void portal() {
        // The Sling Ring's circle: the ring hand held up as an anchor, the other sweeps a circle of sparks before him.
        define(GestureId.STRANGE_SLING)
                .key(0, S).m(REST_M).o(REST_O).twist(0)
                .key(3, O).m(-110, -20, 10).o(-52, 18, -6).twist(-4)
                .key(6, S).m(-128, 4, 22).o(-52, 18, -6)
                .key(9, S).m(-102, 26, 30).o(-52, 18, -6).twist(4)
                .key(12, S).m(-76, 6, 18).o(-52, 18, -6)
                .key(14, O).m(-96, -18, 8).o(-56, 18, -6).twist(0)
                .key(18, S).m(REST_M).o(REST_O);
        // The Mirror Dimension: both palms pressed together before the chest, then pulled apart, the world bending.
        define(GestureId.STRANGE_MIRROR)
                .key(0, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0)
                .key(5, E).m(-84, -34, 0).o(-84, 34, 0).lean(2)
                .key(9, S).m(-86, -36, 0).o(-86, 36, 0).mTremble(1.0F, 2.0F).oTremble(1.0F, 2.0F)
                .key(14, O).m(-80, 30, 46).o(-80, -30, -46).lean(-2).head(-4, 0, 0)
                .key(22, S).m(REST_M).o(REST_O).lean(0).head(0, 0, 0);
    }

    private static void eye() {
        // The Eye of Agamotto opens: both hands frame it before the chest, the forearms turning in the mudra.
        define(GestureId.STRANGE_EYE_OPEN)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(5, O).m(-58, -38, -10).o(-58, 38, 10).head(6, 0, 0)
                .key(10, S).m(-64, -30, 18).o(-64, 30, -18)
                .key(15, S).m(-58, -40, -14).o(-58, 40, 14)
                .key(19, S).m(CAST_M).o(CAST_O).head(2, 0, 0)
                .key(24, S).m(CAST_M).o(CAST_O).head(0, 0, 0);
        // The Rewind: the main forearm turns back over the Eye, twice, the rings running backward.
        define(GestureId.STRANGE_REWIND)
                .key(0, S).m(REST_M).o(REST_O)
                .key(4, O).m(-70, -30, 30).o(-50, 30, -6)
                .key(8, S).m(-70, -30, -20).o(-50, 30, -6)
                .key(12, S).m(-70, -30, 30).o(-50, 30, -6)
                .key(16, S).m(-70, -30, -20).o(-50, 30, -6)
                .key(20, S).m(REST_M).o(REST_O);
    }

    private StrangeGestures() {
    }
}
