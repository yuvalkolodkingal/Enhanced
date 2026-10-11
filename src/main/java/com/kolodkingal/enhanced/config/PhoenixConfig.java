package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of the Phoenix (Jean Grey). Its options are defined in {@link #define}, which {@code
 * EnhancedConfig} calls while it builds the server config (TOML section {@code [phoenix]}); the lane that builds the
 * hero adds the rest here, so no other file changes.
 */
public final class PhoenixConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "phoenix";

    /**
     * Assigned by {@link #define} when the config is built, which is at mod construction: before anything reads it (it
     * cannot be final because the options are defined in a method, not in a static block).
     */
    public static ModConfigSpec.DoubleValue FLIGHT_SPEED;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        FLIGHT_SPEED = builder.comment("The Phoenix's flying speed (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.09, 0.01, 0.5);
        builder.pop();
    }

    private PhoenixConfig() {
    }
}
