package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of Thanos, the hero. Its options are defined in {@link #define}, which {@code EnhancedConfig}
 * calls while it builds the server config (TOML section {@code [thanos]}); the lane that builds the hero adds them
 * here, so no other file changes.
 */
public final class ThanosConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "thanos";

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        builder.pop();
    }

    private ThanosConfig() {
    }
}
