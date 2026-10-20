package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.IronFxPayload;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Iron Man's gestures (3.8, lane L3): the suit-up and take-off, the classic suits' moves (the Mark 1's
 * flamethrower, boost, crash, rocket, brace and punch, the Mark 3's repulsors, Unibeam, flares and missiles, the Mark
 * 5's dash and laser burst). Precise and mechanical: short attacks, firm holds, a suit's weight in every move. The keys
 * are timed to the server: a repulsor fires as the key is pressed (the gesture is the recoil), the boost's two parts
 * are 10 and 20 ticks. Pure data (no Minecraft client classes), so the game tests can check it. The main arm's {@code
 * +z} swings it outward and {@code -y} turns it in; the off arm's signs are mirrored by hand.
 */
public final class IronManGestures {
    /** The suit-up's stance: the arms a little out and down, still. */
    public static final float[] SUIT_M = {-8, 0, 28};
    public static final float[] SUIT_O = {-8, 0, -28};
    /** The Unibeam's stance: the arms swept back and out, the chest thrust forward. */
    public static final float[] BEAM_M = {28, 0, 22};
    public static final float[] BEAM_O = {28, 0, -22};
    /** The flamethrower's stance: both forearms out before him, a little in. */
    public static final float[] FLAME_M = {-82, -12, 0};
    public static final float[] FLAME_O = {-82, 12, 0};
    /** The Brace: the arms crossed before the chest. */
    public static final float[] BRACE_M = {-78, -48, 0};
    public static final float[] BRACE_O = {-72, 48, 0};
    private static final float[] REST = {0, 0, 0};

    static void register() {
        define(GestureId.IRON_SUIT_UP)
                .key(0, S).m(REST).o(REST).head(0, 0, 0)
                .key(5, O).m(SUIT_M).o(SUIT_O).head(-12, 0, 0)
                .key(20, S).m(SUIT_M).o(SUIT_O).head(-6, 0, 0);
        define(GestureId.IRON_SUIT_DOWN)
                .key(0, S).m(SUIT_M).o(SUIT_O).head(0, 0, 0)
                .key(5, O).m(-14, 0, 38).o(-14, 0, -38).head(10, 0, 0)
                .key(16, S).m(REST).o(REST).head(0, 0, 0);
        // A step into the Hulkbuster's open frame: the right leg forward, the arms reaching into its sleeves.
        define(GestureId.IRON_STEP_IN)
                .key(0, S).m(REST).o(REST).legs(0, 0).lean(0)
                .key(8, O).m(-40, 0, 12).o(-40, 0, -12).legs(-30, 8).lean(6)
                .key(16, E).m(-60, 0, 18).o(-60, 0, -18).legs(0, 0).lean(2)
                .key(24, S).m(SUIT_M).o(SUIT_O).legs(0, 0).lean(0);
        // A repulsor: the arm thrown up along the look, then the recoil kicks it up a little.
        define(GestureId.IRON_REPULSOR_R)
                .key(0, S).mAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(2, O).mAim(Gestures.Target.LOOK, -12, 0, 0)
                .key(6, E).mAim(Gestures.Target.LOOK, -2, 0, 0)
                .key(10, S).m(REST);
        define(GestureId.IRON_REPULSOR_L)
                .key(0, S).oAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(2, O).oAim(Gestures.Target.LOOK, -12, 0, 0)
                .key(6, E).oAim(Gestures.Target.LOOK, -2, 0, 0)
                .key(10, S).o(REST);
        define(GestureId.IRON_DOUBLE_BLAST)
                .key(0, S).mAim(Gestures.Target.LOOK, 0, -4, 0).oAim(Gestures.Target.LOOK, 0, 4, 0).lean(0)
                .key(3, O).mAim(Gestures.Target.LOOK, -14, -4, 0).oAim(Gestures.Target.LOOK, -14, 4, 0).lean(-6)
                .key(9, E).mAim(Gestures.Target.LOOK, -2, -4, 0).oAim(Gestures.Target.LOOK, -2, 4, 0).lean(-2)
                .key(14, S).m(REST).o(REST).lean(0);
        define(GestureId.IRON_UNIBEAM)
                .key(0, S).m(REST).o(REST).lean(0).head(0, 0, 0)
                .key(5, O).m(BEAM_M).o(BEAM_O).lean(-10).head(-6, 0, 0)
                .key(16, S).m(BEAM_M).o(BEAM_O).lean(-8).head(-4, 0, 0);
        define(GestureId.IRON_FLARES)
                .key(0, S).m(REST).o(REST).lean(0)
                .key(3, O).m(-10, 0, 35).o(-10, 0, -35).lean(8)
                .key(14, S).m(REST).o(REST).lean(0);
        define(GestureId.IRON_MISSILES)
                .key(0, S).m(REST).o(REST).head(0, 0, 0)
                .key(3, O).m(-18, 0, 22).o(-18, 0, -22).head(-4, 0, 0)
                .key(14, S).m(REST).o(REST).head(0, 0, 0);
        define(GestureId.IRON_FLAME)
                .key(0, S).m(REST).o(REST)
                .key(4, O).m(FLAME_M).o(FLAME_O)
                .key(12, S).m(FLAME_M).o(FLAME_O);
        define(GestureId.IRON_BOOST)
                .key(0, S).m(REST).o(REST).legs(0, 0).head(0, 0, 0)
                .key(3, O).m(8, 0, 10).o(8, 0, -10).legs(4, 4).head(-14, 0, 0)
                .key(20, S).m(8, 0, 10).o(8, 0, -10).legs(4, 4).head(-6, 0, 0);
        // The crash: down on one knee with a fist to the ground (the legs and the drop as the hero landing's).
        define(GestureId.IRON_CRASH_LAND)
                .key(0, S).m(REST).o(REST).legs(0, 0).drop(0).lean(0)
                .key(3, I).m(-35, 0, 15).o(-10, 0, -40).legs(-75, 40).drop(0.42F).lean(22)
                .key(16, S).m(-35, 0, 15).o(-10, 0, -40).legs(-75, 40).drop(0.42F).lean(22)
                .key(26, S).m(REST).o(REST).legs(0, 0).drop(0).lean(0);
        define(GestureId.IRON_ARM_ROCKET)
                .key(0, S).oAim(Gestures.Target.LOOK, 0, 0, 0)
                .key(2, O).oAim(Gestures.Target.LOOK, -16, 0, 0)
                .key(8, E).oAim(Gestures.Target.LOOK, -3, 0, 0)
                .key(12, S).o(REST);
        define(GestureId.IRON_BRACE)
                .key(0, S).m(REST).o(REST).drop(0)
                .key(4, O).m(BRACE_M).o(BRACE_O).drop(0.1F)
                .key(10, S).m(BRACE_M).o(BRACE_O).drop(0.1F);
        // The heavy right hook: drawn back, then a slow, crushing swing across.
        define(GestureId.IRON_HEAVY_PUNCH)
                .key(0, S).m(REST).twist(0).lean(0)
                .key(4, O).m(-50, 20, 35).twist(-18).lean(-2)
                .key(7, I).m(-95, -30, 0).twist(22).lean(8)
                .key(14, S).m(REST).twist(0).lean(0);
        define(GestureId.IRON_DASH_PUNCH)
                .key(0, S).m(REST).o(REST).lean(0)
                .key(2, O).mAim(Gestures.Target.LOOK, 0, 0, 0).o(25, 0, -12).lean(18)
                .key(8, S).mAim(Gestures.Target.LOOK, 0, 0, 0).o(25, 0, -12).lean(14)
                .key(12, S).m(REST).o(REST).lean(0);
        // The Laser Burst: the arms flung out to the sides and one full turn of the body.
        define(GestureId.IRON_LASER_BURST)
                .key(0, S).m(REST).o(REST).yaw(0)
                .key(3, O).m(-90, 0, 80).o(-90, 0, -80).yaw(0)
                .key(13, E).m(-90, 0, 80).o(-90, 0, -80).yaw(360)
                .key(20, S).m(REST).o(REST).yaw(360);
    }

    /** The gesture an effect of his starts, or null. */
    @Nullable
    public static GestureId gestureOf(int kind, int arg) {
        return switch (kind) {
            case IronFxPayload.SUIT_UP -> arg == SuitKind.HULKBUSTER.ordinal() ? GestureId.IRON_STEP_IN
                    : GestureId.IRON_SUIT_UP;
            case IronFxPayload.SUIT_DOWN -> GestureId.IRON_SUIT_DOWN;
            case IronFxPayload.REPULSOR -> arg == 1 ? GestureId.IRON_REPULSOR_L : GestureId.IRON_REPULSOR_R;
            case IronFxPayload.DOUBLE_BLAST -> GestureId.IRON_DOUBLE_BLAST;
            case IronFxPayload.FLARES -> GestureId.IRON_FLARES;
            case IronFxPayload.MISSILES -> GestureId.IRON_MISSILES;
            case IronFxPayload.FLAME_ON -> GestureId.IRON_FLAME;
            case IronFxPayload.BOOST -> GestureId.IRON_BOOST;
            case IronFxPayload.CRASH -> GestureId.IRON_CRASH_LAND;
            case IronFxPayload.ARM_ROCKET -> GestureId.IRON_ARM_ROCKET;
            case IronFxPayload.BRACE -> arg == 1 ? GestureId.IRON_BRACE : null;
            case IronFxPayload.PUNCH -> GestureId.IRON_HEAVY_PUNCH;
            case IronFxPayload.DASH -> GestureId.IRON_DASH_PUNCH;
            case IronFxPayload.LASER_BURST -> GestureId.IRON_LASER_BURST;
            default -> null;
        };
    }

    /** Whether the gesture is one of lane L3's (all started by the payloads, or by the Unibeam's state). */
    static boolean ours(GestureId id) {
        return switch (id) {
            case IRON_SUIT_UP, IRON_SUIT_DOWN, IRON_STEP_IN, IRON_REPULSOR_R, IRON_REPULSOR_L, IRON_DOUBLE_BLAST,
                    IRON_UNIBEAM, IRON_FLARES, IRON_MISSILES, IRON_FLAME, IRON_BOOST, IRON_CRASH_LAND,
                    IRON_ARM_ROCKET, IRON_BRACE, IRON_HEAVY_PUNCH, IRON_DASH_PUNCH, IRON_LASER_BURST -> true;
            default -> false;
        };
    }

    private IronManGestures() {
    }
}
