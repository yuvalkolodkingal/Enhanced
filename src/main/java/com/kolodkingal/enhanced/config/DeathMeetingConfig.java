package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The meeting with Death (3.7, Eilon's request): a player who dies meets Rio Vidal, the Green Witch, who lets him start
 * again at home or in the Nether. {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML
 * section {@code [death_meeting]}). Read from the server when used (never cached). The client's own switch back to the
 * vanilla screen is {@code showDeathMeeting} in enhanced-client.toml.
 */
public final class DeathMeetingConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "death_meeting";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue ALLOW_NETHER;
    public static ModConfigSpec.IntValue FIRE_RESISTANCE_SECONDS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether a player who dies meets Death (Rio, the Green Witch) instead of the plain",
                "death screen. Off: the vanilla screen. Hardcore worlds and doImmediateRespawn always keep vanilla's",
                "way.")
                .define("enabled", true);
        ALLOW_NETHER = builder.comment("Whether Death offers \"Start again in the Nether\" (a safe spot near the",
                "player's spawn point divided by 8, or a small platform built for him).")
                .define("allowNether", true);
        FIRE_RESISTANCE_SECONDS = builder.comment("Seconds of Fire Resistance a player who starts again in the Nether",
                "gets (0: none).")
                .defineInRange("fireResistanceSeconds", 10, 0, 600);
        builder.pop();
    }

    private DeathMeetingConfig() {
    }
}
