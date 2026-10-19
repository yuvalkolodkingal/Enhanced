package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The X-Men lower lab under every Mutant School (3.7): built at run time, its secret entrance, the lift, the Weapon X
 * rack and the Cure Station. Every option of the plan is defined here by the 3.7 skeleton (lane L4 reads them); {@code
 * EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code [school_lab]}). They
 * are read from the server and read when used (never cached).
 */
public final class SchoolLabConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "school_lab";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue AUTO_BUILD;
    public static ModConfigSpec.IntValue BUILD_RADIUS;
    public static ModConfigSpec.IntValue LAYERS_PER_TICK;
    public static ModConfigSpec.BooleanValue OPEN_TO_ALL;
    public static ModConfigSpec.IntValue LIFT_SNEAK_TICKS;
    public static ModConfigSpec.BooleanValue LECTERN_HINT;
    public static ModConfigSpec.IntValue INJECTIONS_PER_PLAYER;
    public static ModConfigSpec.BooleanValue CURE_STATION;
    public static ModConfigSpec.IntValue CURE_STATION_COOLDOWN;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("school_lab");
        ENABLED = builder.comment("Whether the schools get their lower lab (off: no labs are built and no entrance",
                "appears; built ones stay).")
                .define("enabled", true);
        AUTO_BUILD = builder.comment("Whether a school's lab is built by itself when a player comes near (off: only",
                "the command builds one).")
                .define("autoBuild", true);
        BUILD_RADIUS = builder.comment("Radius (blocks) round a player in which schools get their lab.")
                .defineInRange("buildRadius", 96, 16, 256);
        LAYERS_PER_TICK = builder.comment("Layers of the lab built per tick.")
                .defineInRange("layersPerTick", 2, 1, 28);
        OPEN_TO_ALL = builder.comment("Whether anyone may open the secret bookshelves (false: only the X-Men heroes",
                "and operators).")
                .define("openToAll", true);
        LIFT_SNEAK_TICKS = builder.comment("Ticks of Sneak on an elevator pad before the lift rides.")
                .defineInRange("liftSneakTicks", 20, 1, 200);
        LECTERN_HINT = builder.comment("Whether the Professor's notes are put on the study's empty lectern.")
                .define("lecternHint", true);
        INJECTIONS_PER_PLAYER = builder.comment("Adamantium Injections each player may take from one rack.")
                .defineInRange("injectionsPerPlayer", 1, 0, 64);
        CURE_STATION = builder.comment("Whether the Cure Station gives Mutant Cures.")
                .define("cureStation", true);
        CURE_STATION_COOLDOWN = builder.comment("Ticks before the same player gets another cure from a station.")
                .defineInRange("cureStationCooldown", 1200, 0, 72000);
        builder.pop();
    }

    private SchoolLabConfig() {
    }
}
