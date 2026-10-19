package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.deadpool.DeadpoolLines;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Deadpool's gestures (3.7): quick, loose and showy, a mercenary who enjoys the audience. The keys are timed
 * to the server's moves: a cut falls at tick 4-7 of its gesture (the blow is dealt as the key is pressed, so the swing
 * is the follow-through), the right pistol fires 3 ticks after the left ({@code Pistols.PAIR_GAP}), the reload's twirl
 * lasts the default {@code reloadTicks} 40 as two twirls. Pure data (no Minecraft client classes), so the game tests can
 * check it. The main arm's {@code +z} swings it outward and {@code -y} turns it in; the off arm's signs are mirrored by
 * hand, as in the other heroes' tables.
 */
public final class DeadpoolGestures {
    /** The Parry's stance: both arms forward, a little in, the swords spinning before the chest. */
    public static final float[] SWORDS_M = {-75, -15, 5};
    public static final float[] SWORDS_O = {-75, 15, -5};
    /** How far (degrees) the drawn pistols are spread from the line of his look. */
    public static final float GUNS_SPREAD = 6.0F;
    private static final float[] REST = {0, 0, 0};

    static void register() {
        // The first cut: the right sword raised high on the right, then swept down and across to the left.
        define(GestureId.DEADPOOL_SLASH_1)
                .key(0, S).m(REST).lean(0).twist(0)
                .key(2, O).m(-150, 10, 40).lean(-4).twist(8)
                .key(5, I).m(-45, -40, -15).lean(8).twist(-15)
                .key(10, S).m(REST).lean(0).twist(0);
        // The second: the left sword, the mirror of the first.
        define(GestureId.DEADPOOL_SLASH_2)
                .key(0, S).o(REST).lean(0).twist(0)
                .key(2, O).o(-150, -10, -40).lean(-4).twist(-8)
                .key(5, I).o(-45, 40, 15).lean(8).twist(15)
                .key(10, S).o(REST).lean(0).twist(0);
        // The X-cut: both swords crossed high over the head, then down and out together.
        define(GestureId.DEADPOOL_SLASH_3)
                .key(0, S).m(REST).o(REST).lean(0).drop(0)
                .key(3, O).m(-165, -25, -10).o(-165, 25, 10).lean(-6).drop(0)
                .key(7, I).m(-35, 30, 45).o(-35, -30, -45).lean(12).drop(0.12F)
                .key(14, S).m(REST).o(REST).lean(0).drop(0);
        // The Parry: both swords brought up before him, into the spinning stance (DEADPOOL_SWORDS).
        define(GestureId.DEADPOOL_PARRY)
                .key(0, S).m(REST).o(REST)
                .key(8, O).m(SWORDS_M).o(SWORDS_O);
        // A shot: the pistol kicks up a little and comes back down (added to the aimed stance).
        define(GestureId.DEADPOOL_SHOOT_L)
                .key(0, S).oAdd(0, 0, 0)
                .key(1, O).oAdd(-14, 0, 0)
                .key(6, S).oAdd(0, 0, 0);
        define(GestureId.DEADPOOL_SHOOT_R)
                .key(0, S).mAdd(0, 0, 0)
                .key(1, O).mAdd(-14, 0, 0)
                .key(6, S).mAdd(0, 0, 0);
        // The reload: the pistols low before him, twirled round the fingers twice, the head down to watch them.
        define(GestureId.DEADPOOL_RELOAD)
                .key(0, S).m(-40, -10, 10).o(-40, 10, -10).head(14, 0, 0)
                .key(5, E).m(-55, -35, 10).o(-55, 35, -10).head(16, 0, 0)
                .key(10, E).m(-40, 25, 10).o(-40, -25, -10).head(14, 0, 0)
                .key(15, E).m(-55, -35, 10).o(-55, 35, -10).head(16, 0, 0)
                .key(20, S).m(-40, -10, 10).o(-40, 10, -10).head(0, 0, 0);
        // The Trick Shot: one arm out along the look, the other hand on the hip, the head tilted for the bounce.
        define(GestureId.DEADPOOL_TRICK_SHOT)
                .key(0, S).m(REST).o(REST).head(0, 0, 0)
                .key(3, O).mAim(Gestures.Target.LOOK, 0, 0, 0).o(15, 0, -35).head(0, 0, -14)
                .key(12, S).mAim(Gestures.Target.LOOK, 0, 0, 0).o(15, 0, -35).head(0, 0, -14)
                .key(16, S).m(REST).o(REST).head(0, 0, 0);
        // The Teleport Belt: a slap of the buckle, the head down to it.
        define(GestureId.DEADPOOL_BLINK)
                .key(0, S).m(REST).head(0, 0, 0)
                .key(2, O).m(-20, -35, -20).head(25, 0, 0)
                .key(8, S).m(REST).head(0, 0, 0);
        // The Drop-In: both hands high on the grip, the point down, the knees tucked; then the plunge.
        define(GestureId.DEADPOOL_DROP_IN)
                .key(0, S).m(REST).o(REST).legs(0, 0).lean(0).drop(0)
                .key(3, O).m(-175, -8, 0).o(-175, 8, 0).legs(-30, -30).lean(0).drop(0)
                .key(9, S).m(-170, -8, 0).o(-170, 8, 0).legs(-30, -30).lean(0).drop(0)
                .key(12, I).m(-40, -15, 0).o(-40, 15, 0).legs(-10, -10).lean(15).drop(0.2F)
                .key(16, S).m(REST).o(REST).legs(0, 0).lean(0).drop(0);
        // The Taunt's wave: the right hand high, waving; the left on the hip; the head tilted.
        define(GestureId.DEADPOOL_TAUNT_WAVE)
                .key(0, S).m(REST).o(REST).head(0, 0, 0)
                .key(3, O).m(-165, 0, 30).o(10, 0, -40).head(0, 0, 8)
                .key(7, E).m(-165, 0, 8)
                .key(11, E).m(-165, 0, 30)
                .key(15, E).m(-165, 0, 8)
                .key(19, E).m(-165, 0, 30)
                .key(24, S).m(-160, 0, 20).o(10, 0, -40).head(0, 0, 8)
                .key(30, S).m(REST).o(REST).head(0, 0, 0);
        // The finger guns: both hands forward; a pretend shot from the right, then from the left.
        define(GestureId.DEADPOOL_TAUNT_GUNS)
                .key(0, S).m(REST).o(REST).head(0, 0, 0).lean(0)
                .key(4, O).m(-90, -12, 0).o(-90, 12, 0).head(0, 0, 8).lean(-3)
                .key(8, O).m(-112, -12, 0)
                .key(11, S).m(-90, -12, 0)
                .key(14, O).o(-112, 12, 0)
                .key(17, S).o(-90, 12, 0)
                .key(21, S).m(-90, -12, 0).o(-90, 12, 0).head(0, 0, 8).lean(-3)
                .key(26, S).m(REST).o(REST).head(0, 0, 0).lean(0);
        // The heart: both hands together before the chest in a heart, the head tilted, a little sway.
        define(GestureId.DEADPOOL_TAUNT_HEART)
                .key(0, S).m(REST).o(REST).head(0, 0, 0).lean(0)
                .key(4, O).m(-80, -45, -10).o(-80, 45, 10).head(0, 0, 12).lean(-3)
                .key(14, E).m(-82, -45, -10).o(-82, 45, 10).head(0, 0, -10).lean(-3)
                .key(24, E).m(-80, -45, -10).o(-80, 45, 10).head(0, 0, 12).lean(-3)
                .key(30, S).m(REST).o(REST).head(0, 0, 0).lean(0);
        // Dogpool's whistle: two fingers to the mask's mouth, the head up a little.
        define(GestureId.DEADPOOL_WHISTLE)
                .key(0, S).m(REST).head(0, 0, 0)
                .key(3, O).m(-125, -40, 5).head(-8, 0, 0)
                .key(12, S).m(-125, -40, 5).head(-8, 0, 0)
                .key(16, S).m(REST).head(0, 0, 0);
        // The Smoke Vanish: the bomb raised, then thrown down at his feet.
        define(GestureId.DEADPOOL_SMOKE)
                .key(0, S).m(REST).lean(0)
                .key(3, O).m(-130, 0, 15).lean(-4)
                .key(6, I).m(15, 0, 10).lean(10)
                .key(12, S).m(REST).lean(0);
        // The Chimichanga Break: the snack to the mouth, three happy bites.
        define(GestureId.DEADPOOL_EAT)
                .key(0, S).m(REST).head(0, 0, 0)
                .key(3, O).m(-115, -35, 0).head(5, 0, 0)
                .key(8, E).m(-104, -35, 0).head(8, 0, 0)
                .key(12, E).m(-117, -35, 0).head(4, 0, 0)
                .key(16, E).m(-104, -35, 0).head(8, 0, 0)
                .key(20, E).m(-115, -35, 0).head(5, 0, 0)
                .key(24, S).m(REST).head(0, 0, 0);
    }

    /** The gesture an effect of his starts, or null. */
    @Nullable
    public static GestureId gestureOf(int kind, int arg) {
        return switch (kind) {
            case DeadpoolFxPayload.SLASH -> switch (arg) {
                case 1 -> GestureId.DEADPOOL_SLASH_1;
                case 2 -> GestureId.DEADPOOL_SLASH_2;
                case 3 -> GestureId.DEADPOOL_SLASH_3;
                default -> null;
            };
            case DeadpoolFxPayload.PARRY_ON -> GestureId.DEADPOOL_PARRY;
            case DeadpoolFxPayload.SHOT -> switch (arg) {
                case 0 -> GestureId.DEADPOOL_SHOOT_L;
                case 1 -> GestureId.DEADPOOL_SHOOT_R;
                default -> GestureId.DEADPOOL_TRICK_SHOT;
            };
            case DeadpoolFxPayload.RELOAD -> GestureId.DEADPOOL_RELOAD;
            case DeadpoolFxPayload.BLINK_OUT -> GestureId.DEADPOOL_BLINK;
            case DeadpoolFxPayload.DROP_IN -> GestureId.DEADPOOL_DROP_IN;
            case DeadpoolFxPayload.TAUNT -> tauntPose(arg);
            case DeadpoolFxPayload.SMOKE -> GestureId.DEADPOOL_SMOKE;
            case DeadpoolFxPayload.DOGPOOL_IN -> GestureId.DEADPOOL_WHISTLE;
            case DeadpoolFxPayload.EAT -> arg == 1 ? GestureId.DEADPOOL_EAT : null;
            default -> null;
        };
    }

    /** The Taunt's pose of a line's effect; none for his other lines. */
    @Nullable
    static GestureId tauntPose(int arg) {
        int group = (arg % DeadpoolLines.POSE) / DeadpoolLines.GROUP;
        if (group != DeadpoolLines.Group.TAUNT.ordinal() && group != DeadpoolLines.Group.values().length) {
            return null;
        }
        return switch (DeadpoolLines.poseOf(arg)) {
            case 1 -> GestureId.DEADPOOL_TAUNT_GUNS;
            case 2 -> GestureId.DEADPOOL_TAUNT_HEART;
            default -> GestureId.DEADPOOL_TAUNT_WAVE;
        };
    }

    private DeadpoolGestures() {
    }
}
