package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Agatha Harkness's gestures (3.3). A witch of the old school: her spells are cast with the hands held close
 * and then flung out, the fingers clawed, the shoulders in it (WandaVision's finale; the Witches' Road). The take-off ends
 * on her hover pose ({@code FlightProfile.AGATHA}) and the shield's raise on its stance ({@link StancePoses#AGATHA_SHIELD_M}),
 * so nothing pops when they hand over. No Minecraft client imports: the tables, and {@link #predicted}, are checked by the
 * game tests.
 */
public final class AgathaGestures {
    /** The hover pose of her flight profile (the take-off ends on it). */
    public static final float[] HOVER_M = {-14, 0, 26};
    public static final float[] HOVER_O = {-14, 0, -26};
    /** The legs of the hover pose (right, left). */
    public static final float HOVER_LEG_R = 2;
    public static final float HOVER_LEG_L = 1;

    static void register() {
        // The take-off: the arms swept up and open as her magic lifts her, the head thrown back, then settled on the hover.
        define(GestureId.TAKEOFF_AGATHA)
                .key(0, S).m(-8, 0, 10).o(-8, 0, -10).legs(0, 0).head(0, 0, 0)
                .key(6, O).m(-62, 0, 74).o(-62, 0, -74).legs(6, -2).head(-10, 0, 0)
                .key(13, E).m(-26, 0, 44).o(-26, 0, -44).legs(4, 2).head(-4, 0, 0)
                .key(20, S).m(HOVER_M).o(HOVER_O).legs(HOVER_LEG_R, HOVER_LEG_L).head(0, 0, 0);
        // The Purple Beam: the hand drawn back beside the face, then thrust out along the look (the stance BEAM_HAND holds it).
        define(GestureId.AGATHA_BEAM)
                .key(0, S).m(-20, 0, 10).twist(0)
                .key(3, O).m(-120, -24, 26).twist(-6)
                .key(7, E).mAim(LOOK, 0, -3, 0).twist(4)
                .key(10, S).mAim(LOOK, 0, -3, 0).twist(0);
        // The hex mark or the glimpse: the hand raised at the target, the fingers flicked open, and lowered.
        define(GestureId.AGATHA_HEX)
                .key(0, S).m(-12, 0, 8).head(0, 0, 0)
                .key(4, O).mAim(LOOK, -6, -6, 0).head(0, 0, -4)
                .key(7, O).mAim(LOOK, -18, -6, 0)
                .key(14, S).m(-12, 0, 8).head(0, 0, 0);
        // The Power Drain: both hands thrust at the target, clawed and trembling as it is drawn out, then dragged back to the
        // chest, the body leaning back with it.
        define(GestureId.AGATHA_DRAIN)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10).lean(0).head(0, 0, 0)
                .key(4, O).mAim(LOOK, 0, -9, 0).oAim(LOOK, 0, 9, 0).lean(4)
                .key(10, S).mAim(LOOK, -4, -9, 0).mTremble(2.4F, 2.2F).oAim(LOOK, -4, 9, 0).oTremble(2.4F, 2.2F).lean(4)
                .key(16, E).m(-72, 30, -8).o(-72, -30, 8).lean(-4).head(-6, 0, 0)
                .key(22, S).m(-18, 0, 10).o(-18, 0, -10).lean(0).head(0, 0, 0);
        // The Witch's Shield: the hands drawn in to the chest, then pushed out into the stance.
        define(GestureId.AGATHA_SHIELD_RAISE)
                .key(0, S).m(-12, 0, 10).o(-12, 0, -10).head(0, 0, 0)
                .key(4, O).m(-44, 34, -6).o(-44, -34, 6)
                .key(10, S).m(StancePoses.AGATHA_SHIELD_M).o(StancePoses.AGATHA_SHIELD_O).head(StancePoses.AGATHA_SHIELD_HEAD, 0, 0);
        define(GestureId.AGATHA_SHIELD_DROP)
                .key(0, S).m(StancePoses.AGATHA_SHIELD_M).o(StancePoses.AGATHA_SHIELD_O)
                .key(8, S).m(-10, 0, 10).o(-10, 0, -10);
        // The Rune Trap: the hand draws a circle in the air before her, then both palms are driven down at the ground.
        define(GestureId.AGATHA_RUNES)
                .key(0, S).m(-12, 0, 8).o(-12, 0, -10).head(0, 0, 0).lean(0)
                .key(4, O).m(-96, -28, 4)
                .key(8, S).m(-112, 6, 30)
                .key(12, S).m(-80, 26, 14)
                .key(15, O).m(-46, 0, 16).o(-46, 0, -16).head(12, 0, 0).lean(5)
                .key(19, S).m(-46, 0, 16).o(-46, 0, -16).head(12, 0, 0).lean(5)
                .key(24, S).m(-12, 0, 8).o(-12, 0, -10).head(0, 0, 0).lean(0);
    }

    /**
     * The gesture the local Agatha's own key press starts at once, or null: B (the shield) when it has recharged, is not up
     * and she is fed; N (the runes) when they have recharged. The Purple Beam and the hex wait for the server (the beam's
     * state, the hex's raised hand); the Power Drain waits for its cooldown, because only the server judges its target.
     */
    @Nullable
    public static GestureId predicted(PowerData data, int ability, long now, boolean fed) {
        if (ability == 4 && data.isReady(4, now) && !data.chaosShieldActive() && fed) {
            return GestureId.AGATHA_SHIELD_RAISE;
        }
        if (ability == 5 && data.isReady(5, now)) {
            return GestureId.AGATHA_RUNES;
        }
        return null;
    }

    /** Her eyes still burn this long after the drain or the runes were cast (ticks). */
    public static final int GLOW_AFTER_CAST = 24;

    /**
     * Whether her eyes burn violet now ("It's been Agatha all along": they do in WandaVision): while the beam burns, the shield
     * is up and the hex is cast, and for {@link #GLOW_AFTER_CAST} ticks after the drain or the runes (whose cooldowns start
     * at {@code drainCooldown} and {@code runeCooldown}).
     */
    public static boolean eyesGlow(PowerData data, long now, long drainCooldown, long runeCooldown) {
        return data.is(Live.BEAMING) || data.chaosShieldActive() || data.is(Live.CASTING)
                || justCast(data, 3, now, drainCooldown) || justCast(data, 5, now, runeCooldown);
    }

    private static boolean justCast(PowerData data, int ability, long now, long full) {
        long left = data.cooldownUntil(ability) - now;
        return left > 0 && left > full - GLOW_AFTER_CAST;
    }

    private AgathaGestures() {
    }
}
