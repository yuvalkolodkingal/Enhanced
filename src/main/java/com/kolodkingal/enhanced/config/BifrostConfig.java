package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The Bifrost of 3.5 (Thor's, Hofund's, Gungnir's, Heimdall's and the rune stones'): who it takes with its traveller.
 * Every option of the plan is defined here by the 3.5 skeleton (the skeleton of 3.5; lane L2 owns the Bifrost after
 * it); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code [bifrost]}).
 * They are read from the server and synced to clients, and read when used (never cached), so an edit applies at once.
 */
public final class BifrostConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "bifrost";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.DoubleValue GROUP_RADIUS;
    public static ConfigSpec.BooleanValue SNEAK_TO_STAY;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        GROUP_RADIUS = builder.comment("The radius (blocks) of the Bifrost's circle: every player and creature whose feet are "
                        + "inside it (and within a few blocks up or down) travels with the one who opened it. The "
                        + "default is the circle of runes it burns into the ground. 0: only the traveller goes.")
                .defineInRange("groupRadius", 3.2, 0.0, 16.0);
        SNEAK_TO_STAY = builder.comment("Whether a player inside the circle who is sneaking stays behind (false: everyone inside "
                        + "goes).")
                .define("sneakToStay", false);
        builder.pop();
    }

    private BifrostConfig() {
    }
}
