package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Odin, the Allfather (3.5). Every option of the plan is defined here by the 3.5 skeleton (lane L3 reads them); {@code
 * EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code [odin]}). They are read
 * from the server and synced to clients, and read when used (never cached), so an edit applies at once.
 */
public final class OdinConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "odin";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.DoubleValue BLAST_DAMAGE;
    public static ModConfigSpec.IntValue BLAST_COOLDOWN;
    public static ModConfigSpec.DoubleValue THROW_DAMAGE;
    public static ModConfigSpec.IntValue THROW_COOLDOWN;
    public static ModConfigSpec.IntValue SHIELD_SECONDS;
    public static ModConfigSpec.IntValue SHIELD_COOLDOWN;
    public static ModConfigSpec.DoubleValue HEAL;
    public static ModConfigSpec.IntValue HEAL_COOLDOWN;
    public static ModConfigSpec.IntValue BANISH_COOLDOWN;
    public static ModConfigSpec.BooleanValue BANISH_PLAYERS;
    public static ModConfigSpec.DoubleValue WAVE_DAMAGE;
    public static ModConfigSpec.IntValue WAVE_COOLDOWN;
    public static ModConfigSpec.IntValue RAVENS_SECONDS;
    public static ModConfigSpec.IntValue RAVENS_COOLDOWN;
    public static ModConfigSpec.IntValue COMMAND_COOLDOWN;
    public static ModConfigSpec.IntValue ODINSLEEP_MAX_SECONDS;
    public static ModConfigSpec.IntValue ODINSLEEP_COOLDOWN;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        BLAST_DAMAGE = builder.comment("Damage of the Gungnir Blast.")
                .defineInRange("blastDamage", 12.0, 0.0, 100.0);
        BLAST_COOLDOWN = builder.comment("Cooldown (ticks) of the Gungnir Blast.")
                .defineInRange("blastCooldown", 25, 0, 72000);
        THROW_DAMAGE = builder.comment("Damage of the thrown Gungnir.")
                .defineInRange("throwDamage", 16.0, 0.0, 100.0);
        THROW_COOLDOWN = builder.comment("Cooldown (ticks) of the Gungnir Throw.")
                .defineInRange("throwCooldown", 60, 0, 72000);
        SHIELD_SECONDS = builder.comment("How long (seconds) the Odinforce Shield can be held.")
                .defineInRange("shieldSeconds", 12, 1, 120);
        SHIELD_COOLDOWN = builder.comment("Cooldown (ticks) of the Odinforce Shield.")
                .defineInRange("shieldCooldown", 200, 0, 72000);
        HEAL = builder.comment("Health the Odinforce Heal gives Odin and each ally.")
                .defineInRange("heal", 8.0, 0.0, 100.0);
        HEAL_COOLDOWN = builder.comment("Cooldown (ticks) of the Odinforce Heal.")
                .defineInRange("healCooldown", 600, 0, 72000);
        BANISH_COOLDOWN = builder.comment("Cooldown (ticks) of Banish.")
                .defineInRange("banishCooldown", 400, 0, 72000);
        BANISH_PLAYERS = builder.comment("Whether Banish sends a player (with PvP on only) home by the Bifrost.")
                .define("banishPlayers", true);
        WAVE_DAMAGE = builder.comment("Damage of the Odinforce Wave.")
                .defineInRange("waveDamage", 10.0, 0.0, 100.0);
        WAVE_COOLDOWN = builder.comment("Cooldown (ticks) of the Odinforce Wave.")
                .defineInRange("waveCooldown", 220, 0, 72000);
        RAVENS_SECONDS = builder.comment("How long (seconds) Huginn and Muninn fly.")
                .defineInRange("ravensSeconds", 20, 1, 600);
        RAVENS_COOLDOWN = builder.comment("Cooldown (ticks) of Huginn and Muninn.")
                .defineInRange("ravensCooldown", 600, 0, 72000);
        COMMAND_COOLDOWN = builder.comment("Cooldown (ticks) of the Allfather's Command.")
                .defineInRange("commandCooldown", 1200, 0, 72000);
        ODINSLEEP_MAX_SECONDS = builder.comment("The longest (seconds) the Odinsleep lasts.")
                .defineInRange("odinsleepMaxSeconds", 30, 1, 600);
        ODINSLEEP_COOLDOWN = builder.comment("Cooldown (ticks) of the Odinsleep, after it ends.")
                .defineInRange("odinsleepCooldown", 1800, 0, 72000);
        builder.pop();
    }

    private OdinConfig() {
    }
}
