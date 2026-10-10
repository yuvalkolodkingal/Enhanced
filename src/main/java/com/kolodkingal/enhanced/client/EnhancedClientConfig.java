package com.kolodkingal.enhanced.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side switches for the heroes' animation. Lives in config/enhanced-client.toml; nothing here is synced.
 */
public final class EnhancedClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ANIMATIONS;
    public static final ModConfigSpec.BooleanValue REDUCED_MOTION;
    public static final ModConfigSpec.BooleanValue FLIGHT_EFFECTS;

    static {
        BUILDER.push("animation");
        ANIMATIONS = BUILDER.comment("Animate the heroes: blended poses, flight, running, Thor's weapon. Off: vanilla poses only.")
                .translation("config.enhanced.animations")
                .define("animations", true);
        REDUCED_MOTION = BUILDER.comment("Calmer animation: no hover bob, drift, banking jitter or camera shake, and fainter rings.")
                .translation("config.enhanced.reduced_motion")
                .define("reducedMotion", false);
        FLIGHT_EFFECTS = BUILDER.comment("Flight effects: ribbons, rings, wind streaks and shockwaves (trail particles are thinned out when off).")
                .translation("config.enhanced.flight_effects")
                .define("flightEffects", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Whether the hero animation runs at all (true until the config has loaded). */
    public static boolean animations() {
        return !SPEC.isLoaded() || ANIMATIONS.get();
    }

    public static boolean reducedMotion() {
        return SPEC.isLoaded() && REDUCED_MOTION.get();
    }

    public static boolean flightEffects() {
        return !SPEC.isLoaded() || FLIGHT_EFFECTS.get();
    }

    private EnhancedClientConfig() {
    }
}
