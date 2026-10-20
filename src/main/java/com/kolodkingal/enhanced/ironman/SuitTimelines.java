package com.kolodkingal.enhanced.ironman;

import net.minecraft.util.Mth;

/**
 * The suit-up's timelines (3.8, PLAN 7.2): pure functions of the phase's progress, so every client draws the same
 * moment and the GameTests check them (monotonic, 1 at the end, the helmet last, DOWN the reverse). The 3.8 skeleton's
 * curves are the plainest (every part on together, linearly); lane L7 writes each suit's choreography here and keeps
 * these signatures.
 */
public final class SuitTimelines {
    /**
     * The progress (0-1) of a phase of {@code ticks} that began at game time {@code start}, at {@code gameTime} plus
     * the render's {@code partialTick}.
     */
    public static float progress(long gameTime, long start, int ticks, float partialTick) {
        if (ticks <= 0) {
            return 1.0F;
        }
        return Mth.clamp((gameTime - start + partialTick) / ticks, 0.0F, 1.0F);
    }

    /**
     * How far the part {@code part} of the suit {@code kind} is on (0: not there, 1: in place) at the phase's {@code
     * progress}; a take-off ({@code up} false) plays the suit-up backwards. The skeleton: every part as the phase.
     */
    public static float reveal(SuitKind kind, SuitParts part, float progress, boolean up) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        return up ? t : 1.0F - t;
    }

    private SuitTimelines() {
    }
}
