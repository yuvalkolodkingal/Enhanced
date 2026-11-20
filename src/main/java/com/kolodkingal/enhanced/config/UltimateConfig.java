package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The Ultimate switch (3.18), TOML section {@code [ultimate]}: the ultimate versions of the heroes (now Cosmic Spider-Man)
 * work only while it is on, or for the players an operator granted them to ({@code /enhanced ultimate grant}). Off by
 * default. {@code /enhanced ultimate true|false} turns it on and off for everyone.
 */
public final class UltimateConfig {
    public static ConfigSpec.BooleanValue ENABLED;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("ultimate");
        ENABLED = builder.comment("Whether the ultimate versions of the heroes (Cosmic Spider-Man) work for everyone. Off: only for the",
                        "players granted them with /enhanced ultimate grant <players>.")
                .define("enabled", false);
        builder.pop();
    }

    private UltimateConfig() {
    }
}
