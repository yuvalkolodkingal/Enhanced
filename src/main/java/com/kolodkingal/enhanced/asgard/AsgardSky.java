package com.kolodkingal.enhanced.asgard;

import net.minecraft.util.Mth;

/**
 * The colours of Asgard's sky by the time of day (3.5): pure functions (the client's sky draws with them; the tests check
 * them). {@code dayTime} is the fraction of the day the game uses ({@code Level.getTimeOfDay}: 0 at noon, 0.5 at
 * midnight). By day a blue-gold sky with the nebula faint; at night deep space, the nebula and the stars full. No
 * sunrise colours.
 */
public final class AsgardSky {
    /** The sky at noon (the biome's), and in the deep of the night. */
    public static final int DAY = 0x9EC4F0;
    public static final int NIGHT = 0x060914;
    /** The gold of the horizon by day. */
    public static final int HORIZON = 0xE9D49A;

    /** How much of the night it is: 0 by day, 1 at midnight, smooth between (the sun's height). */
    public static float nightness(float dayTime) {
        float sun = Mth.cos(dayTime * Mth.TWO_PI);
        float day = Mth.clamp(sun * 2.0F + 0.5F, 0.0F, 1.0F);
        return 1.0F - day;
    }

    /** The colour of the sky overhead at this time (RGB). */
    public static int dayColour(float dayTime) {
        return lerp(DAY, NIGHT, nightness(dayTime));
    }

    /** The colour of the sky at the horizon at this time (RGB): gold by day, deep blue at night. */
    public static int horizonColour(float dayTime) {
        return lerp(HORIZON, 0x101a3a, nightness(dayTime));
    }

    /** How strongly the nebula shows: 0.35 by day, 1 at night. */
    public static float nebulaAlpha(float dayTime) {
        return 0.35F + 0.65F * nightness(dayTime);
    }

    /** How strongly the stars show: 0.15 by day, 1 at night. */
    public static float starAlpha(float dayTime) {
        return 0.15F + 0.85F * nightness(dayTime);
    }

    /** Blends two RGB colours. */
    public static int lerp(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return r << 16 | g << 8 | bl;
    }

    private AsgardSky() {
    }
}
