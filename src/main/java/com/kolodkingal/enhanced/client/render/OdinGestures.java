package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Odin's gestures (3.5): an old king's economy, every motion carried by the spear. The arm that holds Gungnir is
 * {@code M}. The gestures that hand over to a stance (the shield's, the Odinsleep's) end exactly on that stance's pose, so
 * nothing pops; the others start and end on the arms at rest. Pure data (no Minecraft client classes), so the game tests
 * can check it.
 */
public final class OdinGestures {
    /** The arms at rest: {@code M}, then {@code O}. */
    static final float[] REST_M = {0, 0, 4};
    static final float[] REST_O = {0, 0, -4};
    /** The Odinforce Shield: Gungnir planted upright before him, the other hand open toward the dome. */
    public static final float[] SHIELD_M = {-28, -12, 6};
    public static final float[] SHIELD_O = {-72, 24, -28};
    /** The Odinsleep: kneeling, both hands resting on the spear's shaft before him. */
    public static final float[] SLEEP_M = {-42, -16, 6};
    public static final float[] SLEEP_O = {-40, 26, -6};
    /** The Odinsleep's legs (right forward, its foot planted; left knee down), its sink and bow. */
    public static final float SLEEP_LEG_R = -82;
    public static final float SLEEP_LEG_L = 12;
    public static final float SLEEP_DROP = 0.42F;
    public static final float SLEEP_LEAN = 9;
    public static final float SLEEP_HEAD = 24;
    /** At rest with Gungnir: both hands on the upright spear. */
    public static final float[] REGAL_M = {-24, -18, 8};
    public static final float[] REGAL_O = {-30, 30, -10};

    static void register() {
        // The blast: the spear levelled along the look and thrust, held while the bolt leaves its tip.
        define(GestureId.ODIN_BLAST)
                .key(0, S).m(REST_M).lean(0)
                .key(3, O).mAim(LOOK, -2, 0, 0).lean(4)
                .key(8, S).mAim(LOOK, -2, 0, 0).lean(3)
                .key(14, S).m(REST_M).lean(0);

        // The throw: hauled back over the shoulder, then hurled with the whole body behind it.
        define(GestureId.ODIN_THROW)
                .key(0, S).m(-168, 0, 6).twist(-14).lean(0)
                .key(3, O).mAim(LOOK, 0, 0, 0).twist(10).lean(6)
                .key(6, S).mAim(LOOK, 0, 0, 0)
                .key(16, S).m(REST_M).twist(0).lean(0);

        // The catch: the hand held out, the spear slaps into it, the arm gives and comes down.
        define(GestureId.ODIN_CATCH)
                .key(0, S).m(-78, 0, 10)
                .key(2, O).m(-56, 0, 10)
                .key(12, S).m(REST_M);

        // The shield: the spear raised a little, then its butt struck down and planted; the other hand opens.
        define(GestureId.ODIN_SHIELD_UP)
                .key(0, S).m(REST_M).o(REST_O)
                .key(4, O).m(-62, -10, 6).o(-40, 14, -16)
                .key(7, E).m(SHIELD_M).o(SHIELD_O)
                .key(10, S).m(SHIELD_M).o(SHIELD_O);

        // The shield let go of: the spear lifts, the hand falls.
        define(GestureId.ODIN_SHIELD_DOWN)
                .key(0, S).m(SHIELD_M).o(SHIELD_O)
                .key(8, S).m(REST_M).o(REST_O);

        // The heal: the open palm swept slowly round before him, from his left to his right.
        define(GestureId.ODIN_HEAL)
                .key(0, S).o(REST_O).head(0, 0, 0)
                .key(6, O).o(-82, 42, -12).head(4, 0, 0)
                .key(16, S).o(-82, -18, -12).head(4, 0, 0)
                .key(22, S).o(REST_O).head(0, 0, 0);

        // Banish: the spear's point levelled at the one he banishes, held, then raised away.
        define(GestureId.ODIN_BANISH)
                .key(0, S).m(REST_M).head(0, 0, 0)
                .key(5, O).mAim(LOOK, 0, 0, 0).head(-4, 0, 0)
                .key(12, S).mAim(LOOK, 0, 0, 0).head(-4, 0, 0)
                .key(18, S).m(REST_M).head(0, 0, 0);

        // The wave: the spear raised, then its butt driven into the ground with a dip of the whole body.
        define(GestureId.ODIN_WAVE)
                .key(0, S).m(REST_M).lean(0).drop(0)
                .key(5, O).m(-150, 0, 10).lean(-4).drop(0)
                .key(9, O).m(-12, 0, 10).lean(10).drop(0.16F)
                .key(16, S).m(REST_M).lean(0).drop(0);

        // The ravens: the free arm flung out from the shoulder and up; they leave it.
        define(GestureId.ODIN_RAVENS)
                .key(0, S).o(REST_O).head(0, 0, 0)
                .key(5, O).o(-104, 0, -58).head(-8, 0, 0)
                .key(10, S).o(-104, 0, -58).head(-8, 0, 0)
                .key(16, S).o(REST_O).head(0, 0, 0);

        // The command: Gungnir raised high over his head and held, the head up.
        define(GestureId.ODIN_COMMAND)
                .key(0, S).m(REST_M).head(0, 0, 0)
                .key(7, O).m(-174, 0, 8).head(-10, 0, 0)
                .key(18, S).m(-172, 0, 8).head(-10, 0, 0)
                .key(26, S).m(REST_M).head(0, 0, 0);

        // The Odinsleep: down on one knee, the head bowed, the hands come to rest on the spear.
        define(GestureId.ODIN_KNEEL)
                .key(0, S).m(REST_M).o(REST_O).legs(0, 0).drop(0).lean(0).head(0, 0, 0)
                .key(8, O).m(SLEEP_M).o(SLEEP_O).legs(SLEEP_LEG_R, SLEEP_LEG_L).drop(SLEEP_DROP).lean(SLEEP_LEAN).head(SLEEP_HEAD, 0, 0)
                .key(18, S).m(SLEEP_M).o(SLEEP_O).legs(SLEEP_LEG_R, SLEEP_LEG_L).drop(SLEEP_DROP).lean(SLEEP_LEAN).head(SLEEP_HEAD, 0, 0);

        // He wakes: the head lifts, he pushes up on the spear and stands.
        define(GestureId.ODIN_RISE)
                .key(0, S).m(SLEEP_M).o(SLEEP_O).legs(SLEEP_LEG_R, SLEEP_LEG_L).drop(SLEEP_DROP).lean(SLEEP_LEAN).head(SLEEP_HEAD, 0, 0)
                .key(5, O).m(SLEEP_M).o(SLEEP_O).legs(SLEEP_LEG_R, SLEEP_LEG_L).drop(SLEEP_DROP).lean(SLEEP_LEAN).head(-4, 0, 0)
                .key(16, S).m(REST_M).o(REST_O).legs(0, 0).drop(0).lean(0).head(0, 0, 0);

        // The coronation: the spear lifted high in both hands, held in the light, then brought down to rest upright.
        define(GestureId.ODIN_CROWN)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0)
                .key(10, O).m(-170, 0, 8).o(-170, 0, -8).head(-14, 0, 0)
                .key(28, S).m(-168, 0, 8).o(-168, 0, -8).head(-14, 0, 0)
                .key(40, S).m(REST_M).o(REST_O).head(0, 0, 0);
    }

    private OdinGestures() {
    }
}
