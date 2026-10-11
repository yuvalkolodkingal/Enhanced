package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of the Ultron boss fight (the rules all bosses share are in {@link BossConfig}). Its options are
 * defined in {@link #define}, which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code
 * [ultron_boss]}); the lane that builds the boss adds them here, so no other file changes.
 */
public final class UltronBossConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "ultron_boss";

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        builder.pop();
    }

    private UltronBossConfig() {
    }
}
