package com.kolodkingal.enhanced.client.render;

import net.minecraft.util.Mth;

/**
 * How long each part of Spider-Man's web effects lasts, and how it grows and fades (DESIGN section 14, "Client
 * effects"): the strand of a Web Shot, the strands of a Web Burst, the splat where a web hits, and the line of a zip or
 * a swing as it lets go. Pure arithmetic on ticks (no client classes), so the game tests can check the timelines; the
 * drawing is {@link SpiderEffects}.
 */
public final class WebTiming {
    /** A Web Shot's strand grows from his hand to the impact over this many ticks, holds one, and fades over one. */
    public static final int SHOT_GROW = 2;
    public static final int SHOT_TICKS = 4;
    /** A Web Burst's strands go out in two ticks, hold two, and fade over three. */
    public static final int BURST_OUT = 2;
    public static final int BURST_HOLD = 2;
    public static final int BURST_TICKS = 7;
    /**
     * How long a splat of a Web Shot and of a strand of a burst lasts, and over how many of its last ticks it fades.
     */
    public static final int SPLAT_TICKS = 20;
    public static final int BURST_SPLAT_TICKS = 14;
    public static final int SPLAT_FADE = 8;
    /**
     * A zip's or swing's line lets go: the hand end flies to the anchor in this many ticks, the line fades in this
     * many.
     */
    public static final int RELEASE_FLY = 4;
    public static final int RELEASE_FADE = 5;

    /** How much of the way from the hand to the impact a Web Shot's strand has reached at {@code age} ticks. */
    public static float shotReach(float age) {
        return Mth.clamp(age / SHOT_GROW, 0.0F, 1.0F);
    }

    /** The opacity of a Web Shot's strand at {@code age} ticks (0 once it is over). */
    public static float shotAlpha(float age) {
        return age < 0.0F ? 0.0F : age < SHOT_TICKS - 1 ? 1.0F : Mth.clamp(SHOT_TICKS - age, 0.0F, 1.0F);
    }

    /** How much of its length a burst's strand has reached at {@code age} ticks. */
    public static float burstReach(float age) {
        return Mth.clamp(age / BURST_OUT, 0.0F, 1.0F);
    }

    /** The opacity of a burst's strand at {@code age} ticks. */
    public static float burstAlpha(float age) {
        float fade = BURST_TICKS - BURST_OUT - BURST_HOLD;
        return age < 0.0F ? 0.0F : age < BURST_OUT + BURST_HOLD ? 1.0F : Mth.clamp(1.0F - (age - BURST_OUT - BURST_HOLD) / fade, 0.0F, 1.0F);
    }

    /**
     * The opacity of a splat that lasts {@code life} ticks, at {@code age} ticks: full, then fading over the last
     * eight.
     */
    public static float splatAlpha(float age, int life) {
        return age < 0.0F || age >= life ? 0.0F : Mth.clamp((life - age) / SPLAT_FADE, 0.0F, 1.0F);
    }

    /**
     * How far the hand end of a line that has let go has flown towards the anchor, at {@code age} ticks after it did.
     */
    public static float releaseFly(float age) {
        return Mth.clamp(age / RELEASE_FLY, 0.0F, 1.0F);
    }

    /** The opacity of a line that has let go, {@code age} ticks after it did. */
    public static float releaseAlpha(float age) {
        return Mth.clamp(1.0F - age / RELEASE_FADE, 0.0F, 1.0F);
    }

    /**
     * The sideways shimmer of a line (blocks) at distance {@code along} from its hand, {@code time} ticks into the
     * game, {@code age} ticks after it was thrown: {@code sin(time 0.6 + along 3) 0.02}, damped to nothing as the line
     * goes taut over its first ten ticks, and nil at its two ends.
     */
    public static float ripple(float time, float along, float age, float fraction) {
        float slack = Mth.clamp(1.0F - age / 10.0F, 0.0F, 1.0F);
        return Mth.sin(time * 0.6F + along * 3.0F) * 0.02F * slack * Mth.sin(Mth.PI * Mth.clamp(fraction, 0.0F, 1.0F));
    }

    private WebTiming() {
    }
}
