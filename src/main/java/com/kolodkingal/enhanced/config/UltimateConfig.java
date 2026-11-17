package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Ultimate Mode (3.17): one server setting for every hero's ultimate version, in place of the separate switches of
 * before (3.16's {@code vision.solar_jewel.enabled}). TOML section {@code [ultimate]}; read when used. Operators change
 * it for a world with {@code /enhanced ultimate on|off} (saved in {@code serverconfig/enhanced-ultimate.json}, which
 * then wins over {@code enabled} here).
 */
public final class UltimateConfig {
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.IntValue AWAKEN_TICKS;
    public static ConfigSpec.IntValue DURATION_SECONDS;
    public static ConfigSpec.IntValue COOLDOWN_SECONDS;
    public static ConfigSpec.IntValue SIGNATURE_COOLDOWN_SECONDS;
    public static ConfigSpec.DoubleValue SIGNATURE_DAMAGE;
    public static ConfigSpec.IntValue SIGNATURE_RADIUS;
    public static ConfigSpec.DoubleValue DAMAGE_BONUS;
    public static ConfigSpec.BooleanValue SIGNATURE_HURTS_PLAYERS;
    public static ConfigSpec.IntValue NEXUS_FLASH_RANGE;
    public static ConfigSpec.DoubleValue NEXUS_FLASH_DAMAGE;

    public static void define(ConfigSpec.Builder builder) {
        builder.comment("Ultimate Mode (3.17): every hero gets an ultimate version. Off by default, and while it is off every hero is",
                "exactly the hero of before 3.17. On: the Ultimate Form key (the apostrophe key unless changed) awakens your hero's",
                "ultimate form for a while (the key again: its signature move); Source Code Vision runs on the Solar Jewel and burns;",
                "Billy is the Demiurge (every spell, the other magic users' abilities on the Magic Types key, and he can grant magic",
                "to others); the Scarlet Witch becomes the Nexus Being; Spider-Man becomes Cosmic Spider-Man, Iron Man the Godkiller.",
                "Operators: /enhanced ultimate on|off|info (saved with the world, it then wins over 'enabled' here).")
                .push("ultimate");
        ENABLED = builder.comment("On: Ultimate Mode for every player (unless the world's own setting, /enhanced ultimate, says otherwise).")
                .define("enabled", false);
        AWAKEN_TICKS = builder.comment("Ticks the awakening takes (the hero rises in their power before the form is theirs).")
                .defineInRange("awakenTicks", 30, 5, 200);
        DURATION_SECONDS = builder.comment("Seconds an ultimate form lasts.")
                .defineInRange("durationSeconds", 45, 5, 3600);
        COOLDOWN_SECONDS = builder.comment("Seconds after a form ends before it can be awakened again.")
                .defineInRange("cooldownSeconds", 180, 0, 36000);
        SIGNATURE_COOLDOWN_SECONDS = builder.comment("Seconds between two signature moves (O while the form lasts).")
                .defineInRange("signatureCooldownSeconds", 10, 1, 600);
        SIGNATURE_DAMAGE = builder.comment("Damage of a signature move to each creature it strikes (some moves strike several times).")
                .defineInRange("signatureDamage", 14.0, 0.0, 200.0);
        SIGNATURE_RADIUS = builder.comment("How far (blocks) a signature move reaches round the hero.")
                .defineInRange("signatureRadius", 10, 2, 64);
        DAMAGE_BONUS = builder.comment("How much more damage everything a hero does while in their ultimate form (0.25: a quarter more).")
                .defineInRange("damageBonus", 0.25, 0.0, 5.0);
        SIGNATURE_HURTS_PLAYERS = builder.comment("Whether signature moves strike other players too (PvP must be on).")
                .define("signatureHurtsPlayers", true);
        NEXUS_FLASH_RANGE = builder.comment("The Nexus Being (the Scarlet Witch's ultimate form): how far (blocks) from her the Chaos",
                "Burst reaches: all the ground and the sky that near throw red energy every two seconds, and it strikes everyone",
                "there but her (her signature move is the same burst at once, twice as hard).")
                .defineInRange("nexusFlashRange", 50, 4, 128);
        NEXUS_FLASH_DAMAGE = builder.comment("The Nexus Being: the damage of each Chaos Burst to each one it strikes.")
                .defineInRange("nexusFlashDamage", 15.0, 0.0, 200.0);
        builder.pop();
    }

    private UltimateConfig() {
    }
}
