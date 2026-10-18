package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.PowerData;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Hela's gestures (3.5). The Goddess of Death moves little and with contempt: the hands open and the blades
 * are there; the throws are flung from behind the shoulders with both arms, the walls swept along the floor, the form
 * drawn back over the head as the headdress grows (Ragnarok). The grasp ends on its stance ({@link #GRASP_M}), the form's
 * gesture on the blades' ({@link #BLADES_M}), so nothing pops when they hand over. No Minecraft client imports: the tables
 * and {@link #predicted} are checked by the game tests.
 */
public final class HelaGestures {
    /** The stance of the blades (form on): both hands low and out, the points turned outward. */
    public static final float[] BLADES_M = {-16, 6, 18};
    public static final float[] BLADES_O = {-16, -6, -18};
    /** The stance of the grasp: the main hand out along the look, fingers spread; the other drawn back. */
    public static final float[] GRASP_M = {-88, -4, 4};
    public static final float[] GRASP_O = {12, 0, -14};

    static void register() {
        // The volley: both hands drawn up behind the shoulders, then flung forward and outward at the look.
        define(GestureId.HELA_THROW)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10).lean(0).head(0, 0, 0)
                .key(4, O).m(-160, 0, 40).o(-160, 0, -40).lean(-5).head(-6, 0, 0)
                .key(7, O).mAim(LOOK, 0, -18, 0).oAim(LOOK, 0, 18, 0).lean(7).head(2, 0, 0)
                .key(16, S).m(-12, 0, 10).o(-12, 0, -10).lean(0).head(0, 0, 0);
        // The rain: an arm raised high, palm down, held while the blades hang, brought down as they fall.
        define(GestureId.HELA_RAIN)
                .key(0, S).m(-12, 0, 10).head(0, 0, 0)
                .key(5, O).m(-172, 0, 10).head(-18, 0, 0)
                .key(15, S).m(-172, 0, 10).mTremble(1.2F, 1.5F).head(-18, 0, 0)
                .key(19, E).m(-70, 0, 10).head(0, 0, 0)
                .key(22, S).m(-12, 0, 10);
        // The wall: the hand drawn low and back, then swept forward along the floor, the body bent into it.
        define(GestureId.HELA_WALL)
                .key(0, S).m(-12, 0, 10).lean(0).twist(0)
                .key(4, O).m(30, 0, 30).lean(10).twist(-14)
                .key(8, O).m(-55, -30, 6).lean(16).twist(16)
                .key(14, S).m(-55, -30, 6).lean(12).twist(10)
                .key(18, S).m(-12, 0, 10).lean(0).twist(0);
        // The burst: both arms flung wide and down, the head bowed: the ring rises round her.
        define(GestureId.HELA_BURST)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10).head(0, 0, 0)
                .key(4, O).m(-70, 0, 30).o(-70, 0, -30).head(-8, 0, 0)
                .key(8, O).m(10, 0, 70).o(10, 0, -70).head(14, 0, 0).lean(6)
                .key(18, S).m(-12, 0, 10).o(-12, 0, -10).head(0, 0, 0).lean(0);
        // The form: both hands sweep up the face and back over the head as the headdress grows, then down to the blades.
        define(GestureId.HELA_FORM_ON)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10).head(0, 0, 0).lean(0)
                .key(6, O).m(-150, 20, 0).o(-150, -20, 0).head(-10, 0, 0).lean(-4)
                .key(12, S).m(-185, -10, 30).o(-185, 10, -30).head(-16, 0, 0).lean(-6)
                .key(24, S).m(BLADES_M).o(BLADES_O).head(0, 0, 0).lean(0);
        define(GestureId.HELA_FORM_OFF)
                .key(0, S).m(BLADES_M).o(BLADES_O).head(0, 0, 0)
                .key(6, O).m(-120, 15, 0).o(-120, -15, 0).head(6, 0, 0)
                .key(14, S).m(-12, 0, 10).o(-12, 0, -10).head(0, 0, 0);
        // The dash: arms swept back, the body low and forward.
        define(GestureId.HELA_DASH)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10).lean(0)
                .key(3, O).m(40, 0, 20).o(40, 0, -20).lean(18)
                .key(12, S).m(-12, 0, 10).o(-12, 0, -10).lean(0);
        // Raise the Dead: both arms rise slowly, palms up, the head lifted.
        define(GestureId.HELA_RAISE)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10).head(0, 0, 0)
                .key(6, S).m(-25, 0, 40).o(-25, 0, -40)
                .key(18, E).m(-150, 0, 36).o(-150, 0, -36).head(-20, 0, 0)
                .key(26, S).m(-12, 0, 10).o(-12, 0, -10).head(0, 0, 0);
        // Fenris: a hand points where he comes.
        define(GestureId.HELA_FENRIS)
                .key(0, S).m(-12, 0, 10).head(0, 0, 0)
                .key(5, O).mAim(LOOK, -6, 0, 0).head(0, 0, -4)
                .key(13, S).mAim(LOOK, -6, 0, 0)
                .key(18, S).m(-12, 0, 10).head(0, 0, 0);
        // Death's Grasp: the hand thrust out, fingers spread, into the stance.
        define(GestureId.HELA_GRASP)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10)
                .key(4, O).m(-96, -4, 4).o(16, 0, -16)
                .key(8, S).m(GRASP_M).o(GRASP_O);
    }

    /**
     * The gesture the local Hela's own key press starts at once, or null: R (the volley) and G (the wall) when they have
     * recharged; the rest wait for the server's answer.
     */
    @Nullable
    public static GestureId predicted(PowerData data, int ability, long now, boolean sneaking) {
        if (ability == 1 && !sneaking && data.isReady(1, now)) {
            return GestureId.HELA_THROW;
        }
        if (ability == 2 && !sneaking && data.isReady(2, now)) {
            return GestureId.HELA_WALL;
        }
        return null;
    }

    private HelaGestures() {
    }
}
