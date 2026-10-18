package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.heimdall.HeimdallAbilities;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Heimdall's gestures and the poses of his stances (3.5). The sentinel moves little and with weight: the blows of
 * Hofund are a soldier's, the whole body turning behind a long two-handed blade (Thor: the Dark World's guard on the
 * Bifrost); the Bifrost is the sword turned in its socket; the Ward is the blade planted and both hands on its pommel. No
 * Minecraft client imports: the tables and {@link #step} are checked by the game tests.
 */
public final class HeimdallGestures {
    // ---- the stances' poses (main arm, off arm: x pitch, y yaw, z roll in degrees) ----
    /** The Ward: both hands on the pommel of the planted blade, before the chest. */
    public static final float[] WARD_M = {-40, 28, 4};
    public static final float[] WARD_O = {-40, -28, -4};
    public static final float WARD_HEAD = 6;
    /** Far Sight: the off hand shading the eyes, the sword hand low at his side. */
    public static final float[] FAR_M = {-14, 0, 6};
    public static final float[] FAR_O = {-128, -38, -6};
    public static final float FAR_HEAD = -5;
    /** The watch: both hands resting on the sword held upright before him, lower and easier than the Ward. */
    public static final float[] WATCH_M = {-26, 22, 3};
    public static final float[] WATCH_O = {-26, -22, -3};
    /** The rest the gestures come back to. */
    private static final float[] REST_M = {-8, 0, 8};
    private static final float[] REST_O = {-8, 0, -8};

    static void register() {
        blows();
        sight();
        bifrost();
    }

    private static void blows() {
        // The forehand: Hofund raised high on the right and behind, then cut down across the body to the left.
        define(GestureId.HEIMDALL_SLASH_1)
                .key(0, S).m(REST_M).o(REST_O).twist(0).lean(0)
                .key(3, E).m(-165, -34, 34).o(-24, 0, -18).twist(-16).lean(-2)
                .key(6, O).m(-52, 52, -6).o(-14, 0, -22).twist(16).lean(6)
                .key(8, S).m(-46, 56, -8).twist(14).lean(5)
                .key(12, S).m(REST_M).o(REST_O).twist(0).lean(0);
        // The backhand: drawn across to the left shoulder, then swept back out to the right.
        define(GestureId.HEIMDALL_SLASH_2)
                .key(0, S).m(-46, 56, -8).o(REST_O).twist(14).lean(4)
                .key(3, E).m(-118, 66, -14).o(-20, 0, -16).twist(18).lean(0)
                .key(6, O).m(-64, -58, 34).o(-12, 0, -24).twist(-18).lean(6)
                .key(8, S).m(-58, -62, 36).twist(-16).lean(5)
                .key(12, S).m(REST_M).o(REST_O).twist(0).lean(0);
        // The third: both hands on the hilt, the blade raised overhead, then brought straight down with the body behind it.
        define(GestureId.HEIMDALL_SLASH_3)
                .key(0, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0)
                .key(5, E).m(-176, 14, 6).o(-170, -22, -6).lean(-7).drop(0).head(-8, 0, 0)
                .key(8, I).m(-48, 18, 2).o(-52, -24, -2).lean(14).drop(0.16F).head(8, 0, 0).legs(-22, 14)
                .key(11, S).m(-44, 18, 2).o(-48, -24, -2).lean(12).drop(0.14F).legs(-22, 14)
                .key(16, S).m(REST_M).o(REST_O).lean(0).drop(0).head(0, 0, 0).legs(0, 0);
        // The lunge: the blade drawn back at the hip, then thrust along his look with a long step, the off arm thrown back.
        define(GestureId.HEIMDALL_LUNGE)
                .key(0, S).m(-20, 0, 12).o(REST_O).lean(0).legs(0, 0).twist(0)
                .key(2, E).m(-30, -20, 22).o(-30, 0, -24).lean(-2).twist(-14)
                .key(5, O).mAim(LOOK, 0, -2, 0).o(30, 0, -30).lean(16).legs(-34, 26).twist(12)
                .key(9, S).mAim(LOOK, 0, -2, 0).o(26, 0, -28).lean(14).legs(-30, 22).twist(10)
                .key(14, S).m(REST_M).o(REST_O).lean(0).legs(0, 0).twist(0);
        // Hofund returns: the hand thrown out to the side, then the sword slaps into it and is brought before him.
        define(GestureId.HEIMDALL_RETURN)
                .key(0, S).m(REST_M)
                .key(3, O).m(-96, -24, 26)
                .key(5, I).m(-84, -18, 24).mTremble(2.0F, 2.6F)
                .key(9, S).m(-60, 20, 6)
                .key(14, S).m(REST_M);
    }

    private static void sight() {
        // The Sight opens: the head lifts to look far and the off hand passes before the eyes, which take the amber light.
        define(GestureId.HEIMDALL_SIGHT_ON)
                .key(0, S).o(REST_O).head(0, 0, 0)
                .key(4, O).o(-150, -44, -4).head(-4, 0, 0)
                .key(8, S).o(-120, -10, -12).head(-8, 0, 0)
                .key(16, S).o(REST_O).head(0, 0, 0);
        // It closes: a slow nod.
        define(GestureId.HEIMDALL_SIGHT_OFF)
                .key(0, S).head(0, 0, 0)
                .key(4, O).head(10, 0, 0)
                .key(10, S).head(0, 0, 0);
    }

    private static void bifrost() {
        // The Bifrost: both hands on the hilt before him, the blade down as if in the socket, turned once to the right and back.
        define(GestureId.HEIMDALL_BIFROST)
                .key(0, S).m(REST_M).o(REST_O).twist(0).head(0, 0, 0)
                .key(4, O).m(-56, 30, 2).o(-56, -30, -2).twist(0).head(4, 0, 0)
                .key(12, S).m(-58, 24, 8).o(-54, -36, 2).twist(-14)
                .key(20, S).m(-58, 24, 8).o(-54, -36, 2).twist(-14).head(-4, 0, 0)
                .key(26, E).m(-56, 30, 2).o(-56, -30, -2).twist(0)
                .key(30, S).m(REST_M).o(REST_O).twist(0).head(0, 0, 0);
        // The Gatekeeper's Call: the sword raised to the sky, the head thrown back.
        define(GestureId.HEIMDALL_CALL)
                .key(0, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0)
                .key(5, O).m(-178, 0, 10).o(-30, 0, -30).head(-14, 0, 0).lean(-4)
                .key(15, S).m(-178, 0, 10).o(-30, 0, -30).head(-14, 0, 0).lean(-4)
                .key(22, S).m(REST_M).o(REST_O).head(0, 0, 0).lean(0);
        // The Bifrost Strike: the sword raised high, then brought down to point where the light falls.
        define(GestureId.HEIMDALL_STRIKE)
                .key(0, S).m(REST_M).head(0, 0, 0).lean(0)
                .key(5, E).m(-174, -6, 12).head(-10, 0, 0).lean(-4)
                .key(8, I).mAim(LOOK, 6, 0, 0).head(0, 0, 0).lean(8)
                .key(13, S).mAim(LOOK, 4, 0, 0).lean(6)
                .key(20, S).m(REST_M).lean(0);
        // The Bifrost for a friend: the open off hand pushed out towards him.
        define(GestureId.HEIMDALL_SEND)
                .key(0, S).o(REST_O).lean(0)
                .key(4, O).oAim(LOOK, -4, 8, 0).lean(5)
                .key(9, S).oAim(LOOK, -4, 8, 0).oTremble(1.2F, 1.4F).lean(5)
                .key(16, S).o(REST_O).lean(0);
    }

    /**
     * Which blow of the combo a rise of {@code Live.HEIMDALL_SLASHING} at {@code now} is, after the blow {@code last} (1-3, 0
     * for none) seen at {@code lastAt}: the server's own rule ({@link HeimdallAbilities#nextStep}) with two ticks of slack for
     * the time the data takes to arrive.
     */
    public static int step(int last, long lastAt, long now) {
        return HeimdallAbilities.nextStep(last, lastAt, Math.max(lastAt, now - 2));
    }

    /** The gesture of a blow (1-3). */
    @Nullable
    public static GestureId blow(int step) {
        return switch (step) {
            case 1 -> GestureId.HEIMDALL_SLASH_1;
            case 2 -> GestureId.HEIMDALL_SLASH_2;
            case 3 -> GestureId.HEIMDALL_SLASH_3;
            default -> null;
        };
    }

    /**
     * The gesture an R cooldown of {@code left} ticks stands for: the Lunge (a blow's is too short to be one: the blows are
     * told by their state, see {@link #step}).
     */
    @Nullable
    public static GestureId cooldown(int ability, long left) {
        return ability == 1 && left > HeimdallAbilities.SLASH_COOLDOWN + 8 ? GestureId.HEIMDALL_LUNGE : null;
    }

    private HeimdallGestures() {
    }
}
