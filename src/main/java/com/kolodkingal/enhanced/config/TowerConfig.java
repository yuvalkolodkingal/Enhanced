package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Avengers Tower's armoury (3.8, lane L6): the suit displays and tech cases, and the run-time refit of old towers.
 * Every option of the plan is defined here by the 3.8 skeleton (the lanes read them); {@code EnhancedConfig} calls
 * {@link #define} while it builds the server config. They are read from the server and read when used (never cached),
 * so an edit applies at once. TOML section {@code [avengers_tower]}.
 */
public final class TowerConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "avengers_tower";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.IntValue DISPLAYS_PER_PLAYER;
    public static ModConfigSpec.BooleanValue REFIT;
    public static ModConfigSpec.IntValue REFIT_RADIUS;
    public static ModConfigSpec.IntValue REFIT_INTERVAL_TICKS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("avengers_tower");
        DISPLAYS_PER_PLAYER = builder.comment("Items each player may take from one display or case (0: unlimited, for",
                "creative servers).")
                .defineInRange("displaysPerPlayer", 1, 0, 64);
        REFIT = builder.comment("Whether towers generated before 3.8 get the new armoury at run time.")
                .define("refit", true);
        REFIT_RADIUS = builder.comment("Radius (blocks) round a player in which old towers are refitted.")
                .defineInRange("refitRadius", 96, 16, 512);
        REFIT_INTERVAL_TICKS = builder.comment("Ticks between two looks for old towers near a player.")
                .defineInRange("refitIntervalTicks", 100, 20, 12000);
        builder.pop();
    }

    private TowerConfig() {
    }
}
