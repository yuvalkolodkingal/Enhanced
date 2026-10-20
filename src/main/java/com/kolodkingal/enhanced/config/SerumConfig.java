package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Super Soldier Serum (3.8, lane L1): the item that opens the choice of Captain America, Red Hulk or Falcon. Every
 * option of the plan is defined here by the 3.8 skeleton (the lanes read them); {@code EnhancedConfig} calls {@link
 * #define} while it builds the server config. They are read from the server and read when used (never cached), so an
 * edit applies at once. TOML section {@code [super_soldier_serum]}.
 */
public final class SerumConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "super_soldier_serum";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue PENDING_TICKS;
    public static ModConfigSpec.BooleanValue CRAFTABLE;
    public static ModConfigSpec.IntValue CASE_PER_PLAYER;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("super_soldier_serum");
        ENABLED = builder.comment("Whether the serum works (off: using it does nothing).")
                .define("enabled", true);
        PENDING_TICKS = builder.comment("Ticks the serum's open screen stays valid for a choice.")
                .defineInRange("pendingTicks", 600, 20, 12000);
        CRAFTABLE = builder.comment("Whether the serum can be crafted (for servers without Avengers Towers).")
                .define("craftable", false);
        CASE_PER_PLAYER = builder.comment("Serums each player may take from the tower lab's case (0: unlimited).")
                .defineInRange("casePerPlayer", 1, 0, 64);
        builder.pop();
    }

    private SerumConfig() {
    }
}
