package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gungnir, the Allfather's spear (3.6.1): who may take it from its stand in Odin's vault. {@code EnhancedConfig} calls
 * {@link #define} while it builds the server config (TOML section {@code [gungnir]}). Read when used (never cached).
 */
public final class GungnirConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "gungnir";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue ONE_PER_WORLD;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ONE_PER_WORLD = builder.comment("Only one player in the world ever takes Gungnir from its stand: after him the stand"
                        + " is empty for everyone else, even other victors over Hela (he may take a new one there if he"
                        + " loses his). Players in Creative mode are not counted. False: every victor over Hela takes his own.")
                .define("onePerWorld", true);
        builder.pop();
    }

    private GungnirConfig() {
    }
}
