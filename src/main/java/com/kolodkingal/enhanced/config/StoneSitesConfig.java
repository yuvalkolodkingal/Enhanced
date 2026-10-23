package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The new structures of the stones (3.6): the Sanctum Sanctorum, Avengers Tower, the End tunnel and the Vormir Cliff.
 * Each option only stops <em>new</em> generation (existing sites stay). Defined by the 3.6 skeleton (lane L4 reads
 * them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code
 * [stone_sites]}).
 */
public final class StoneSitesConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "stone_sites";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.BooleanValue SANCTUMS;
    public static ConfigSpec.BooleanValue AVENGERS_TOWERS;
    public static ConfigSpec.BooleanValue END_TUNNELS;
    public static ConfigSpec.BooleanValue VORMIR_CLIFFS;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("stone_sites");
        SANCTUMS = builder.comment("Whether new Sanctums Sanctorum generate (the Time Stone's site).")
                .define("sanctums", true);
        AVENGERS_TOWERS = builder.comment("Whether new Avengers Towers generate (the Tesseract's site).")
                .define("avengersTowers", true);
        END_TUNNELS = builder.comment("Whether new End tunnels generate (the Power Stone's site).")
                .define("endTunnels", true);
        VORMIR_CLIFFS = builder.comment("Whether new Vormir Cliffs generate in the Nether (the Soul Stone's site).")
                .define("vormirCliffs", true);
        builder.pop();
    }

    private StoneSitesConfig() {
    }
}
