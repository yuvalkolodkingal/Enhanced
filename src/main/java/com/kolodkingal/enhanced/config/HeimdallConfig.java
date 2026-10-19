package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Heimdall, the Guardian of the Bifrost (3.5). Every option of the plan is defined here by the 3.5 skeleton (lane L2
 * reads them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code
 * [heimdall]}). They are read from the server and synced to clients, and read when used (never cached), so an edit
 * applies at once.
 */
public final class HeimdallConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "heimdall";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.IntValue SIGHT_RANGE;
    public static ModConfigSpec.BooleanValue SEE_OTHER_REALMS;
    public static ModConfigSpec.DoubleValue SLASH_DAMAGE1;
    public static ModConfigSpec.DoubleValue SLASH_DAMAGE2;
    public static ModConfigSpec.DoubleValue SLASH_DAMAGE3;
    public static ModConfigSpec.DoubleValue LUNGE_DAMAGE;
    public static ModConfigSpec.IntValue LUNGE_COOLDOWN;
    public static ModConfigSpec.IntValue WARD_SECONDS;
    public static ModConfigSpec.IntValue WARD_COOLDOWN;
    public static ModConfigSpec.IntValue CALL_COOLDOWN;
    public static ModConfigSpec.IntValue CALL_SECONDS;
    public static ModConfigSpec.DoubleValue STRIKE_DAMAGE;
    public static ModConfigSpec.IntValue STRIKE_COOLDOWN;
    public static ModConfigSpec.IntValue SEND_RANGE;
    public static ModConfigSpec.IntValue WATCH_SECONDS;
    public static ModConfigSpec.IntValue WATCH_RETRY;
    public static ModConfigSpec.DoubleValue HOFUND_BONUS;
    /** 3.5.1: how long the Heimdall of the city leaves his post to a player who is Heimdall. */
    public static ModConfigSpec.IntValue POST_SECONDS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        SIGHT_RANGE = builder.comment("How far (blocks) the All-Seeing Sight shows creatures through walls.")
                .defineInRange("sightRange", 96, 16, 256);
        SEE_OTHER_REALMS = builder.comment("Whether the Sight lists the players of other dimensions on Heimdall's HUD.")
                .define("seeOtherRealms", true);
        SLASH_DAMAGE1 = builder.comment("Damage of the first blow of the Hofund combo.")
                .defineInRange("slashDamage1", 9.0, 0.0, 100.0);
        SLASH_DAMAGE2 = builder.comment("Damage of the second blow of the Hofund combo.")
                .defineInRange("slashDamage2", 11.0, 0.0, 100.0);
        SLASH_DAMAGE3 = builder.comment("Damage of the third blow of the Hofund combo (it knocks back).")
                .defineInRange("slashDamage3", 14.0, 0.0, 100.0);
        LUNGE_DAMAGE = builder.comment("Damage of the Hofund Lunge.")
                .defineInRange("lungeDamage", 12.0, 0.0, 100.0);
        LUNGE_COOLDOWN = builder.comment("Cooldown (ticks) of the Hofund Lunge.")
                .defineInRange("lungeCooldown", 80, 0, 72000);
        WARD_SECONDS = builder.comment("How long (seconds) the Guardian's Ward can be held.")
                .defineInRange("wardSeconds", 10, 1, 120);
        WARD_COOLDOWN = builder.comment("Cooldown (ticks) of the Guardian's Ward.")
                .defineInRange("wardCooldown", 160, 0, 72000);
        CALL_COOLDOWN = builder.comment("Cooldown (ticks) of the Gatekeeper's Call.")
                .defineInRange("callCooldown", 900, 0, 72000);
        CALL_SECONDS = builder.comment("How long (seconds) the Einherjar of the Gatekeeper's Call stay.")
                .defineInRange("callSeconds", 40, 1, 600);
        STRIKE_DAMAGE = builder.comment("Damage of the Bifrost Strike.")
                .defineInRange("strikeDamage", 14.0, 0.0, 100.0);
        STRIKE_COOLDOWN = builder.comment("Cooldown (ticks) of the Bifrost Strike.")
                .defineInRange("strikeCooldown", 140, 0, 72000);
        SEND_RANGE = builder.comment("How far (blocks) the player Heimdall sends with the Bifrost may be.")
                .defineInRange("sendRange", 24, 1, 128);
        WATCH_SECONDS = builder.comment("How long (seconds) the Guardian's Watch lasts.")
                .defineInRange("watchSeconds", 180, 30, 1200);
        WATCH_RETRY = builder.comment("Ticks before a player who lost the Guardian's Watch may take it again.")
                .defineInRange("watchRetry", 1200, 0, 72000);
        HOFUND_BONUS = builder.comment("How much more damage Heimdall deals with Hofund (0.3: 30% more).")
                .defineInRange("hofundBonus", 0.3, 0.0, 5.0);
        POST_SECONDS = builder.comment("How long (seconds) the Heimdall of the city hands his post (the Observatory and the",
                        "Bifrost) to a player who is Heimdall, while he rests nearby.")
                .defineInRange("postSeconds", 600, 1, 7200);
        builder.pop();
    }

    private HeimdallConfig() {
    }
}
