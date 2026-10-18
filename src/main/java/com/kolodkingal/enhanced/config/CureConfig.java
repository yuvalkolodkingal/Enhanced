package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Mutant Cure (3.3.1). Its options are defined in {@link #define}, which {@code EnhancedConfig} calls while it builds
 * the server config (TOML section {@code [mutant_cure]}); they are read from the server and synced to clients, and read
 * when used (never cached), so an edit applies at once. The recipe is data ({@code data/enhanced/recipe/mutant_cure.json}):
 * a data pack can change or remove it.
 */
public final class CureConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "mutant_cure";

    /**
     * Assigned by {@link #define} when the config is built, which is at mod construction: before anything reads it (it
     * cannot be final because the options are defined in a method, not in a static block).
     */
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue HOLD_TICKS;
    public static ModConfigSpec.BooleanValue ON_OTHERS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether the Mutant Cure works (false: using it does nothing and nothing is used up).")
                .define("enabled", true);
        HOLD_TICKS = builder.comment("How long (ticks) right-click is held to inject the Mutant Cure.")
                .defineInRange("holdTicks", 40, 1, 600);
        ON_OTHERS = builder.comment("Whether a player may inject another player (right-click on them and hold): an operator always, "
                        + "anyone else only while PvP is on. False: only on yourself.")
                .define("onOthers", true);
        builder.pop();
    }

    private CureConfig() {
    }
}
