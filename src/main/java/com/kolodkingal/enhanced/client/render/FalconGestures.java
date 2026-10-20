package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.network.FalconFxPayload;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Falcon's gestures (3.8): a soldier's economy, quick and clean. The darts fire at the key press, the
 * right wrist {@code WristWeapons.PAIR_GAP} 3 ticks after the left, so each wrist's kick starts with its dart; the
 * Dive folds into its stance ({@link Stance#FALCON_DIVING}) and the Wing Shield's crossing into its own ({@link
 * Stance#FALCON_SHIELDING}), so neither pops as it hands over. Pure data (no Minecraft client classes), so the game
 * tests can check it. The main arm's {@code +z} swings it outward and {@code -y} turns it in; the off arm's signs are
 * mirrored by hand, as in the other heroes' tables.
 */
public final class FalconGestures {
    /** Behind the Wing Shield: the forearms crossed before the chest. */
    public static final float[] SHIELD_M = {-92, -52, 0};
    public static final float[] SHIELD_O = {-102, 52, 0};
    public static final float SHIELD_HEAD = 12.0F;
    /** In the Dive: the arms tight along his sides, a little back. */
    public static final float[] DIVE_M = {14, 0, 4};
    public static final float[] DIVE_O = {14, 0, -4};
    /** How far (degrees) the Dive tips his body over, head first, and how far up he looks out of it. */
    public static final float DIVE_LEAN = 72.0F;
    public static final float DIVE_HEAD = -48.0F;
    /** Gliding: the arms a little out and back, along the wings. */
    public static final float[] GLIDE_M = {12, 0, 34};
    public static final float[] GLIDE_O = {12, 0, -34};
    private static final float[] REST = {0, 0, 0};

    static void register() {
        // A dart from the left wrist: the off arm up along the look, a little kick, and down again.
        define(GestureId.FALCON_BLAST_L)
                .key(0, S).o(REST)
                .key(1, O).oAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(3, S).oAim(Gestures.Target.LOOK, -12, 0, 0)
                .key(5, S).oAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(8, S).o(REST);
        // The right wrist's, its mirror.
        define(GestureId.FALCON_BLAST_R)
                .key(0, S).m(REST)
                .key(1, O).mAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(3, S).mAim(Gestures.Target.LOOK, -12, 0, 0)
                .key(5, S).mAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(8, S).m(REST);
        // The Wrist Rocket: the right arm out along the look, the left hand under its wrist, a heavier kick.
        define(GestureId.FALCON_ROCKET)
                .key(0, S).m(REST).o(REST).lean(0)
                .key(2, O).mAim(Gestures.Target.LOOK, 0, 0, 0).oAim(Gestures.Target.LOOK, 8, 22, 0).lean(-2)
                .key(4, S).mAim(Gestures.Target.LOOK, -22, 0, 0).oAim(Gestures.Target.LOOK, -10, 22, 0).lean(-6)
                .key(9, S).mAim(Gestures.Target.LOOK, 0, 0, 0).oAim(Gestures.Target.LOOK, 8, 22, 0).lean(-2)
                .key(14, S).m(REST).o(REST).lean(0);
        // The Dive: the arms drawn in tight along him, the head up to watch the prey (into FALCON_DIVING).
        define(GestureId.FALCON_DIVE)
                .key(0, S).m(REST).o(REST).head(0, 0, 0).legs(0, 0)
                .key(4, O).m(-20, 0, 28).o(-20, 0, -28).head(-20, 0, 0).legs(-6, -6)
                .key(12, S).m(DIVE_M).o(DIVE_O).head(DIVE_HEAD, 0, 0).legs(0, 0);
        // The hit: both fists forward into the blow, then out of it.
        define(GestureId.FALCON_DIVE_HIT)
                .key(0, S).m(DIVE_M).o(DIVE_O).lean(0)
                .key(2, O).m(-95, -12, 6).o(-95, 12, -6).lean(8)
                .key(6, S).m(-80, -8, 10).o(-80, 8, -10).lean(4)
                .key(12, S).m(REST).o(REST).lean(0);
        // The Boost Climb: the arms down and back along him, the head up, the knees together; level again at the top.
        define(GestureId.FALCON_CLIMB)
                .key(0, S).m(REST).o(REST).head(0, 0, 0).legs(0, 0)
                .key(3, O).m(28, 0, 14).o(28, 0, -14).head(-32, 0, 0).legs(6, 6)
                .key(16, S).m(24, 0, 12).o(24, 0, -12).head(-26, 0, 0).legs(4, 4)
                .key(20, S).m(REST).o(REST).head(0, 0, 0).legs(0, 0);
        // The Wing Shield: the forearms crossed before the chest as the wings close round (into FALCON_SHIELDING).
        define(GestureId.FALCON_SHIELD)
                .key(0, S).m(REST).o(REST).head(0, 0, 0)
                .key(6, O).m(SHIELD_M).o(SHIELD_O).head(SHIELD_HEAD, 0, 0);
        // The Wing Slash: one whole turn with both arms flung out wide, the wings out with them.
        define(GestureId.FALCON_SLASH)
                .key(0, S).m(REST).o(REST).yaw(0).lean(0)
                .key(2, O).m(-10, 0, 82).o(-10, 0, -82).lean(-4)
                .key(10, I).m(-10, 0, 82).o(-10, 0, -82).yaw(360).lean(-4)
                .key(12, S).m(REST).o(REST).yaw(360).lean(0);
        // Redwing launched: a flick of the right hand up over the shoulder.
        define(GestureId.FALCON_REDWING_LAUNCH)
                .key(0, S).m(REST).head(0, 0, 0)
                .key(3, O).m(-168, -10, 22).head(-14, 0, 0)
                .key(6, E).m(-140, 0, 36).head(-10, 0, 0)
                .key(10, S).m(REST).head(0, 0, 0);
        // Redwing Strike: the right arm points at the foe, held while the drone goes.
        define(GestureId.FALCON_REDWING_POINT)
                .key(0, S).m(REST)
                .key(3, O).mAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(12, S).mAim(Gestures.Target.LOOK, -4, 0, 0)
                .key(16, S).m(REST);
    }

    /** The gesture an effect of his starts on him, or null. */
    @Nullable
    public static GestureId gestureOf(int kind, int arg) {
        return switch (kind) {
            case FalconFxPayload.BLAST -> arg == 1 ? GestureId.FALCON_BLAST_R : GestureId.FALCON_BLAST_L;
            case FalconFxPayload.ROCKET -> GestureId.FALCON_ROCKET;
            case FalconFxPayload.DIVE_START -> GestureId.FALCON_DIVE;
            case FalconFxPayload.DIVE_HIT -> arg == 1 ? GestureId.FALCON_DIVE_HIT : null;
            case FalconFxPayload.CLIMB -> arg == 0 ? GestureId.FALCON_CLIMB : null;
            case FalconFxPayload.SHIELD_ON -> GestureId.FALCON_SHIELD;
            case FalconFxPayload.SLASH -> GestureId.FALCON_SLASH;
            // (Redwing's launch is the toggle REDWING_OUT's, GestureRoutes.toggle.)
            // (Sent for Redwing: the gesture plays on its Falcon, only for the ram's start, not its hit.)
            case FalconFxPayload.REDWING_STRIKE -> arg >= 0 ? GestureId.FALCON_REDWING_POINT : null;
            default -> null;
        };
    }

    private FalconGestures() {
    }
}
