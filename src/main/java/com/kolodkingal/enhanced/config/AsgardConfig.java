package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Asgard, the realm eternal (3.5): the dimension reached only by the Bifrost, its rules and its people. Every option of
 * the plan is defined here by the 3.5 skeleton (lanes L1, the realm, and L5, the Asgardians, read
 * them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code [asgard]}). They are
 * read from the server and synced to clients, and read when used (never cached), so an edit applies at once.
 */
public final class AsgardConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "asgard";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue PROTECT_CITY;
    public static ModConfigSpec.BooleanValue KEEP_INVENTORY;
    public static ModConfigSpec.BooleanValue HOFUND_OPENS_BIFROST;
    public static ModConfigSpec.IntValue RUNE_STONE_COOLDOWN;
    public static ModConfigSpec.IntValue VOID_CATCH_Y;
    public static ModConfigSpec.IntValue EDGE_RADIUS;
    public static ModConfigSpec.BooleanValue NPCS;
    public static ModConfigSpec.DoubleValue TALK_DISTANCE;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether Asgard can be reached (false: the Bifrost does not offer it, the rune stones "
                        + "fizzle and the gate refuses every way in; operators may still /tp).")
                .define("enabled", true);
        PROTECT_CITY = builder.comment("Whether the city of Asgard is protected from breaking, placing, explosions and fire (the "
                        + "wild land and the sea are not). Operators in Creative may edit it.")
                .define("protectCity", true);
        KEEP_INVENTORY = builder.comment("Whether a player who dies in Asgard keeps his inventory and experience (with it off, "
                        + "Mjolnir, Stormbreaker, Hofund, Gungnir and Hela's Headdress are still kept).")
                .define("keepInventory", true);
        HOFUND_OPENS_BIFROST = builder.comment("Whether anyone holding Hofund can open the Bifrost (as Mjolnir and Stormbreaker do).")
                .define("hofundOpensBifrost", true);
        RUNE_STONE_COOLDOWN = builder.comment("Ticks a Bifrost rune stone needs before it can call the Bifrost again.")
                .defineInRange("runeStoneCooldown", 1200, 20, 72000);
        VOID_CATCH_Y = builder.comment("A player who falls below this height in Asgard is caught by Heimdall (the Bifrost sets him "
                        + "down in the Observatory).")
                .defineInRange("voidCatchY", -56, -64, 0);
        EDGE_RADIUS = builder.comment("Beyond this distance from the centre of Asgard, a player is pushed back toward the city.")
                .defineInRange("edgeRadius", 640, 400, 2000);
        NPCS = builder.comment("Whether the Asgardians live in the city (false: none at all; the keeper removes its own).")
                .define("npcs", true);
        TALK_DISTANCE = builder.comment("How far (blocks) a player may be from an Asgardian to talk to him.")
                .defineInRange("talkDistance", 6.0, 2.0, 16.0);
        builder.pop();
    }

    private AsgardConfig() {
    }
}
