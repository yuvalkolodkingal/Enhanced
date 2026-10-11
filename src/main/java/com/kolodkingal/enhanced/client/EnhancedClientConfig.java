package com.kolodkingal.enhanced.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side switches for how the heroes move and look. Lives in config/enhanced-client.toml; nothing here is synced.
 */
public final class EnhancedClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ANIMATIONS;
    public static final ModConfigSpec.BooleanValue REDUCED_MOTION;
    public static final ModConfigSpec.BooleanValue FLIGHT_EFFECTS;
    public static final ModConfigSpec.BooleanValue HIDE_HERO_ARMOUR;

    static {
        BUILDER.push("animation");
        ANIMATIONS = BUILDER.comment("Animate the heroes: blended poses, flight, running, Thor's weapon. Off: vanilla poses only.")
                .translation("config.enhanced.animations")
                .define("animations", true);
        REDUCED_MOTION = BUILDER.comment("Calmer animation: hover bob and drift at a third, no trembling hands, jitter or camera shake, and fainter rings.")
                .translation("config.enhanced.reduced_motion")
                .define("reducedMotion", false);
        FLIGHT_EFFECTS = BUILDER.comment("Flight effects: ribbons, rings, wind streaks and shockwaves (trail particles are thinned out when off).")
                .translation("config.enhanced.flight_effects")
                .define("flightEffects", true);
        BUILDER.pop();
        BUILDER.push("appearance");
        HIDE_HERO_ARMOUR = BUILDER.comment("Don't draw the armour of heroes (yours in third person, and everyone else's): it only hides the characters.",
                        "It protects as usual. Elytra, mob heads, carved pumpkins and the hero gear of this mod are always drawn.")
                .translation("config.enhanced.hide_hero_armour")
                .define("hideHeroArmour", true);
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

    /** Whether heroes are drawn without their armour (true until the config has loaded). */
    public static boolean hideHeroArmour() {
        return !SPEC.isLoaded() || HIDE_HERO_ARMOUR.get();
    }

    private EnhancedClientConfig() {
    }
}
