package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The Soul Stone and its sacrifice at the Vormir Cliff (3.6). Every option of the plan is defined here by the 3.6
 * skeleton (lanes L2 and L5 read them); {@code EnhancedConfig} calls {@link #define} while it builds the server config
 * (TOML section {@code [soul_stone]}). They are read from the server and synced to clients, and read when used (never
 * cached).
 */
public final class SoulStoneConfig {
    /** Who counts as a friend for the sacrifice. */
    public enum FriendRule {
        /** Any other player. */
        ANY,
        /** Only a player on the seeker's scoreboard team. */
        TEAM
    }

    /** How many Soul Stones the sacrifice gives. */
    public enum OnePer {
        /** Each player once. */
        PLAYER,
        /** One in the whole world. */
        WORLD,
        /** Every sacrifice. */
        UNLIMITED
    }

    /** The TOML section of these options. */
    public static final String SECTION = "soul_stone";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.BooleanValue PLAYER_SACRIFICE;
    public static ConfigSpec.BooleanValue PET_SACRIFICE;
    public static ConfigSpec.BooleanValue DESTROY_ITEMS;
    public static ConfigSpec.BooleanValue REMOVE_POWER;
    public static ConfigSpec.EnumValue<FriendRule> FRIEND_RULE;
    public static ConfigSpec.EnumValue<OnePer> ONE_PER;
    public static ConfigSpec.IntValue PUSH_WINDOW_TICKS;
    public static ConfigSpec.IntValue MIN_FALL;
    public static ConfigSpec.IntValue DEATH_WINDOW_TICKS;
    public static ConfigSpec.DoubleValue DRAIN_DAMAGE_PER_SECOND;
    public static ConfigSpec.IntValue DRAIN_RANGE;
    public static ConfigSpec.DoubleValue DRAIN_HEAL_FRACTION;
    public static ConfigSpec.IntValue DRAIN_COOLDOWN;
    public static ConfigSpec.IntValue SIGHT_RANGE;
    public static ConfigSpec.IntValue SIGHT_TICKS;
    public static ConfigSpec.IntValue SIGHT_COOLDOWN;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("soul_stone");
        ENABLED = builder.comment("Whether the Vormir Cliff gives the Soul Stone for a sacrifice.")
                .define("enabled", true);
        PLAYER_SACRIFICE = builder.comment("Whether a player can be the sacrifice (works only where PvP is on).")
                .define("playerSacrifice", true);
        PET_SACRIFICE = builder.comment("Whether the seeker's own pet can be the sacrifice.")
                .define("petSacrifice", true);
        DESTROY_ITEMS = builder.comment("Whether a sacrificed player loses all his items (his ender chest is never",
                "touched).")
                .define("destroyItems", true);
        REMOVE_POWER = builder.comment("Whether a sacrificed player loses his power.")
                .define("removePower", true);
        FRIEND_RULE = builder.comment("Who is a friend: ANY other player (the default: PvP and the cliff's edge make",
                "it deliberate) or only a TEAM mate.")
                .defineEnum("friendRule", FriendRule.ANY);
        ONE_PER = builder.comment("How many Soul Stones: PLAYER (each player once), WORLD (one in the world) or",
                "UNLIMITED.")
                .defineEnum("onePer", OnePer.PLAYER);
        PUSH_WINDOW_TICKS = builder.comment("Ticks before the victim left the edge in which the seeker's push counts.")
                .defineInRange("pushWindowTicks", 100, 20, 400);
        MIN_FALL = builder.comment("The shortest fall (blocks) that counts.")
                .defineInRange("minFall", 30, 10, 100);
        DEATH_WINDOW_TICKS = builder.comment("Ticks after leaving the edge in which the death must come.")
                .defineInRange("deathWindowTicks", 200, 20, 1200);
        DRAIN_DAMAGE_PER_SECOND = builder.comment("Damage per second of the Soul Drain.")
                .defineInRange("drainDamagePerSecond", 6.0, 0.0, 100.0);
        DRAIN_RANGE = builder.comment("Range (blocks) of the Soul Drain.")
                .defineInRange("drainRange", 16, 4, 64);
        DRAIN_HEAL_FRACTION = builder.comment("Fraction of the drained health the holder gets back.")
                .defineInRange("drainHealFraction", 0.5, 0.0, 1.0);
        DRAIN_COOLDOWN = builder.comment("Cooldown (ticks) after the Soul Drain ends.")
                .defineInRange("drainCooldown", 40, 0, 72000);
        SIGHT_RANGE = builder.comment("Range (blocks) of the Soul Sight.")
                .defineInRange("sightRange", 32, 4, 128);
        SIGHT_TICKS = builder.comment("Ticks the Soul Sight lasts.")
                .defineInRange("sightTicks", 160, 20, 1200);
        SIGHT_COOLDOWN = builder.comment("Cooldown (ticks) of the Soul Sight.")
                .defineInRange("sightCooldown", 400, 0, 72000);
        builder.pop();
    }

    private SoulStoneConfig() {
    }
}
