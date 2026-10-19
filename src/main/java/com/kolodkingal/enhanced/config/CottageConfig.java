package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Agatha's Cottage (3.7): the structure, its three residents (Agatha, Nicky and Rio), Senor Scratchy and the cellar's
 * Road door. Every option of the plan is defined here by the 3.7 skeleton (lane L6 reads them); {@code EnhancedConfig}
 * calls {@link #define} while it builds the server config (TOML section {@code [agatha_cottage]}). They are read from
 * the server and read when used (never cached).
 */
public final class CottageConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "agatha_cottage";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue RESIDENTS;
    public static ModConfigSpec.BooleanValue AGATHA_YIELDS;
    public static ModConfigSpec.DoubleValue SCRATCHY_RUNS_OFF;
    public static ModConfigSpec.IntValue GIFTS_PER_DAY;
    public static ModConfigSpec.IntValue BLESSING_TICKS;
    public static ModConfigSpec.BooleanValue TRADES;
    public static ModConfigSpec.BooleanValue ROAD_DOOR;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("agatha_cottage");
        ENABLED = builder.comment("Whether Agatha's Cottage generates in new chunks (existing cottages stay).")
                .define("enabled", true);
        RESIDENTS = builder.comment("Whether Agatha, Nicky and Rio live in the cottages.")
                .define("residents", true);
        AGATHA_YIELDS = builder.comment("Whether the cottage's Agatha is away while a player is Agatha (one of each).")
                .define("agathaYields", true);
        SCRATCHY_RUNS_OFF = builder.comment("Chance each morning that Senor Scratchy runs off.")
                .defineInRange("scratchyRunsOff", 0.3, 0.0, 1.0);
        GIFTS_PER_DAY = builder.comment("Gifts Nicky gives each player per day.")
                .defineInRange("giftsPerDay", 1, 0, 16);
        BLESSING_TICKS = builder.comment("Ticks of Regeneration I in Rio's blessing.")
                .defineInRange("blessingTicks", 600, 0, 12000);
        TRADES = builder.comment("Whether Agatha and Rio trade.")
                .define("trades", true);
        ROAD_DOOR = builder.comment("Whether the cellar's door opens onto the Witches' Road (false: decor only).")
                .define("roadDoor", true);
        builder.pop();
    }

    private CottageConfig() {
    }
}
