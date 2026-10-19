package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Stone Locator (3.7): its recipe and how far and how fast it searches for the Infinity Stones' sites. Every
 * option of the plan is defined here by the 3.7 skeleton (lane L5 reads them); {@code EnhancedConfig} calls {@link
 * #define} while it builds the server config (TOML section {@code [stone_locator]}). They are read from the server and
 * read when used (never cached).
 */
public final class LocatorConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "stone_locator";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue CRAFTABLE;
    public static ModConfigSpec.BooleanValue RETURNS_CORE;
    public static ModConfigSpec.IntValue SEARCH_RADIUS;
    public static ModConfigSpec.IntValue NETHER_SEARCH_RADIUS;
    public static ModConfigSpec.IntValue START_CHECKS_PER_TICK;
    public static ModConfigSpec.IntValue RECHECK_TICKS;
    public static ModConfigSpec.IntValue MOVE_RECHECK;
    public static ModConfigSpec.IntValue PULSE_COOLDOWN;
    public static ModConfigSpec.BooleanValue POINTS_TO_GATEWAYS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("stone_locator");
        CRAFTABLE = builder.comment("Whether the Stone Locator can be crafted.")
                .define("craftable", true);
        RETURNS_CORE = builder.comment("Whether the stone (or vessel) in the recipe's centre is given back.")
                .define("returnsCore", true);
        SEARCH_RADIUS = builder.comment("The farthest (blocks) it searches in the Overworld and the End.")
                .defineInRange("searchRadius", 4096, 256, 30000);
        NETHER_SEARCH_RADIUS = builder.comment("The farthest (blocks) it searches in the Nether.")
                .defineInRange("netherSearchRadius", 1024, 128, 30000);
        START_CHECKS_PER_TICK = builder.comment("Structure starts loaded per tick for the search, on the whole",
                "server.")
                .defineInRange("startChecksPerTick", 2, 1, 32);
        RECHECK_TICKS = builder.comment("Ticks a search's answer is kept.")
                .defineInRange("recheckTicks", 600, 20, 72000);
        MOVE_RECHECK = builder.comment("Blocks the holder may move before the search runs again.")
                .defineInRange("moveRecheck", 64, 8, 1024);
        PULSE_COOLDOWN = builder.comment("Cooldown (ticks) of the spark trail.")
                .defineInRange("pulseCooldown", 100, 0, 72000);
        POINTS_TO_GATEWAYS = builder.comment("Whether, in the wrong realm, it points to the way there (a stronghold",
                "for the End, a Bifrost site for Asgard).")
                .define("pointsToGateways", true);
        builder.pop();
    }

    private LocatorConfig() {
    }
}
