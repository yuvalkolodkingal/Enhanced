package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Hela, the Goddess of Death (3.5), the hero. Every option of the plan is defined here by the 3.5 skeleton (lane L4
 * reads them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code
 * [hela]}). They are read from the server and synced to clients, and read when used (never cached), so an edit applies
 * at once.
 */
public final class HelaConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "hela";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.DoubleValue VOLLEY_DAMAGE;
    public static ModConfigSpec.IntValue VOLLEY_COOLDOWN;
    public static ModConfigSpec.IntValue RAIN_COOLDOWN;
    public static ModConfigSpec.DoubleValue WALL_DAMAGE;
    public static ModConfigSpec.IntValue WALL_COOLDOWN;
    public static ModConfigSpec.IntValue BURST_COOLDOWN;
    public static ModConfigSpec.IntValue FORM_STRENGTH;
    public static ModConfigSpec.IntValue DASH_COOLDOWN;
    public static ModConfigSpec.IntValue RAISE_COUNT;
    public static ModConfigSpec.IntValue RAISE_COUNT_ASGARD;
    public static ModConfigSpec.IntValue RAISE_COOLDOWN;
    public static ModConfigSpec.IntValue FENRIS_COOLDOWN;
    public static ModConfigSpec.IntValue GRASP_SECONDS;
    public static ModConfigSpec.IntValue GRASP_COOLDOWN;
    public static ModConfigSpec.DoubleValue ASGARD_FACTOR;
    public static ModConfigSpec.BooleanValue SHATTER_MJOLNIR;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        VOLLEY_DAMAGE = builder.comment("Damage of each necrosword of the Necrosword Volley.")
                .defineInRange("volleyDamage", 7.0, 0.0, 100.0);
        VOLLEY_COOLDOWN = builder.comment("Cooldown (ticks) of the Necrosword Volley.")
                .defineInRange("volleyCooldown", 30, 0, 72000);
        RAIN_COOLDOWN = builder.comment("Cooldown (ticks) of the Blade Rain.")
                .defineInRange("rainCooldown", 160, 0, 72000);
        WALL_DAMAGE = builder.comment("Damage of each spike of the Blade Wall.")
                .defineInRange("wallDamage", 8.0, 0.0, 100.0);
        WALL_COOLDOWN = builder.comment("Cooldown (ticks) of the Blade Wall.")
                .defineInRange("wallCooldown", 140, 0, 72000);
        BURST_COOLDOWN = builder.comment("Cooldown (ticks) of the Blade Burst.")
                .defineInRange("burstCooldown", 120, 0, 72000);
        FORM_STRENGTH = builder.comment("The level of Strength the Goddess of Death form gives.")
                .defineInRange("formStrength", 2, 1, 5);
        DASH_COOLDOWN = builder.comment("Cooldown (ticks) of the Death Dash.")
                .defineInRange("dashCooldown", 100, 0, 72000);
        RAISE_COUNT = builder.comment("How many berserkers Raise the Dead calls.")
                .defineInRange("raiseCount", 3, 1, 10);
        RAISE_COUNT_ASGARD = builder.comment("How many berserkers Raise the Dead calls in Asgard.")
                .defineInRange("raiseCountAsgard", 5, 1, 10);
        RAISE_COOLDOWN = builder.comment("Cooldown (ticks) of Raise the Dead.")
                .defineInRange("raiseCooldown", 1200, 0, 72000);
        FENRIS_COOLDOWN = builder.comment("Cooldown (ticks) of calling Fenris.")
                .defineInRange("fenrisCooldown", 1800, 0, 72000);
        GRASP_SECONDS = builder.comment("How long (seconds) Death's Grasp can be held.")
                .defineInRange("graspSeconds", 6, 1, 60);
        GRASP_COOLDOWN = builder.comment("Cooldown (ticks) of Death's Grasp.")
                .defineInRange("graspCooldown", 100, 0, 72000);
        ASGARD_FACTOR = builder.comment("Hela's strength from Asgard: her damage is multiplied by this in Asgard.")
                .defineInRange("asgardFactor", 1.25, 1.0, 5.0);
        SHATTER_MJOLNIR = builder.comment("Whether Death's Grasp catches and shatters a thrown Mjolnir.")
                .define("shatterMjolnir", true);
        builder.pop();
    }

    private HelaConfig() {
    }
}
