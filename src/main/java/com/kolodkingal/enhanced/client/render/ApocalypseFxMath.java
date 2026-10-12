package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import net.minecraft.util.Mth;

/**
 * The small formulas of the boss's picture and sound that need no client class, so that the game tests can check them:
 * the colour of his eyes in each state, which theme of the music he asks for and how loud it is at a distance, how far the
 * wall of the arena has risen and how far its amber air reaches. The classes that draw and play ({@link ApocalypseEyesLayer},
 * {@code ApocalypseMusic}, {@link ArenaFx}) call these.
 */
public final class ApocalypseFxMath {
    /** The violet of the mind power and the gold of his shield and of the cracks of the cap, RGB. */
    public static final int VIOLET = SandPalette.VIOLET;
    public static final int GOLD = SandPalette.GOLD;

    /** The music is heard within this many blocks of him: full to {@link #MUSIC_FULL}, falling to nothing at the edge. */
    public static final double MUSIC_REACH = 96.0;
    public static final double MUSIC_FULL = 64.0;
    /** The amber air reaches this far beyond the wall; the wall rises from the ground in this many ticks. */
    public static final double TINT_REACH = 64.0;
    public static final float WALL_TICKS = 40.0F;

    /**
     * The colour (ARGB, alpha for the brightness) the glow of his eyes has now, or 0 for none.
     *
     * @param state    the state of his controller
     * @param time     the ticks since it began
     * @param age      his age in ticks (for the pulse)
     * @param armoured whether the armour is still on
     */
    public static int eyeTint(ApocalypseState state, float time, float age, boolean armoured) {
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.3F);
        return switch (state) {
            case AWAKENING -> time < 20.0F ? 0 : argb(0xFFFFFF, AnimMath.ss(20.0F, 40.0F, time));
            case TELL_MIND_CRUSH, ESCAPING -> argb(VIOLET, 0.75F + 0.25F * pulse);
            case STAGGERED -> argb(0xFFFFFF, 0.35F);
            case KNEELING -> argb(armoured ? 0xFFFFFF : GOLD, 1.0F);
            case DYING -> argb(0xFFFFFF, 1.0F - AnimMath.ss(0.0F, 60.0F, time));
            case ARMOUR_FALL -> argb(0xFFFFFF, 0.7F + 0.3F * pulse);
            default -> argb(0xFFFFFF, pulse);
        };
    }

    /** ARGB from an RGB and an alpha of 0 to 1. */
    public static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }

    /** Which theme a state of the fight asks for: the fuller one from the third phase, or in the true form. */
    public static boolean wantsFinale(int phase, boolean trueForm) {
        return phase >= 3 || trueForm;
    }

    /** How loud the music should be, 0 to 1, for a boss {@code distance} blocks from the camera. */
    public static float musicVolume(double distance) {
        return (float) Mth.clamp(1.0 - (distance - MUSIC_FULL) / (MUSIC_REACH - MUSIC_FULL), 0.0, 1.0);
    }

    /** The fraction of the wall that has risen, {@code ticks} after the fight began. */
    public static float wallRisen(float ticks) {
        return AnimMath.ss(0.0F, WALL_TICKS, ticks);
    }

    /**
     * How strongly the air is tinted for a camera {@code distance} blocks horizontally from the middle of an arena of a wall
     * {@code radius}: full inside the wall, falling off to nothing {@link #TINT_REACH} blocks beyond it.
     */
    public static float tintReach(double distance, double radius) {
        return (float) Mth.clamp(1.0 - (distance - radius) / TINT_REACH, 0.0, 1.0);
    }

    private ApocalypseFxMath() {
    }
}
