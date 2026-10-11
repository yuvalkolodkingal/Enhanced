package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The numbers of Apocalypse's shield, the boss's and the hero's. Its options are defined in {@link #define}, which
 * {@code EnhancedConfig} calls while it builds the server config (TOML section {@code [apocalypse_shield]}); the lane
 * that builds the shield adds them here, so no other file changes.
 */
public final class ApocalypseShieldConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "apocalypse_shield";

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        builder.pop();
    }

    private ApocalypseShieldConfig() {
    }
}
