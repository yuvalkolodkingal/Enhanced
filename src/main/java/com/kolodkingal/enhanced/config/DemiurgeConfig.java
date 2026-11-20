package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The numbers of Billy the Demiurge (3.18.1): Billy's ultimate version, behind Ultimate ({@code [ultimate]}). TOML section
 * {@code [billy.demiurge]}, defined from inside {@code [billy]} ({@code EnhancedConfig}). Every option is read when it is used,
 * so an edit of the file changes it live.
 */
public final class DemiurgeConfig {
    /** The TOML sub-section of these options (inside {@code [billy]}). */
    public static final String SECTION = "demiurge";

    public static ConfigSpec.BooleanValue ENABLED;
    // ---- the body ----
    public static ConfigSpec.DoubleValue MAX_HEALTH;
    public static ConfigSpec.DoubleValue ARMOR;
    public static ConfigSpec.DoubleValue TOUGHNESS;
    public static ConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ConfigSpec.IntValue REGEN_INTERVAL_TICKS;
    public static ConfigSpec.DoubleValue REGEN_AMOUNT;
    public static ConfigSpec.DoubleValue FLIGHT_SPEED;
    // ---- his magic ----
    public static ConfigSpec.DoubleValue DAMAGE_FACTOR;
    public static ConfigSpec.DoubleValue BARRIER_HEALTH_FACTOR;
    public static ConfigSpec.BooleanValue MAGIC_TYPES;
    // ---- the gift of magic ----
    public static ConfigSpec.BooleanValue GIFTS;
    public static ConfigSpec.IntValue GIFT_STRENGTH;
    public static ConfigSpec.IntValue GIFT_MAX_POWERS;
    public static ConfigSpec.DoubleValue GIFT_RANGE;

    /** Defines the options (called inside the {@code [billy]} section). */
    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether Billy can become Billy the Demiurge (Sneak + N). He can only while Ultimate is on for him",
                        "([ultimate] enabled, or /enhanced ultimate grant <him>).")
                .define("enabled", true);

        MAX_HEALTH = builder.comment("His maximum health as the Demiurge (40 = twenty hearts).")
                .defineInRange("maxHealth", 40.0, 1.0, 1024.0);
        ARMOR = builder.comment("Armour as the Demiurge, on top of what he wears (20 = a full netherite set).")
                .defineInRange("armor", 20.0, 0.0, 30.0);
        TOUGHNESS = builder.comment("Armour toughness as the Demiurge (12 = a full netherite set).")
                .defineInRange("toughness", 12.0, 0.0, 20.0);
        KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance as the Demiurge (0.4 = a full netherite set).")
                .defineInRange("knockbackResistance", 0.4, 0.0, 1.0);
        REGEN_INTERVAL_TICKS = builder.comment("He heals regenAmount every this many ticks, on top of a player's own healing.")
                .defineInRange("regenIntervalTicks", 40, 1, 1200);
        REGEN_AMOUNT = builder.comment("Health he heals every regenIntervalTicks (2 = one heart).")
                .defineInRange("regenAmount", 2.0, 0.0, 100.0);
        FLIGHT_SPEED = builder.comment("His flight speed as the Demiurge (Billy's own is billy.flightSpeed).")
                .defineInRange("flightSpeed", 0.12, 0.01, 0.5);

        DAMAGE_FACTOR = builder.comment("Every power of his, his own and the magic types', hurts this many times as much as the Demiurge",
                        "(his bare fist is not changed).")
                .defineInRange("damageFactor", 2.0, 1.0, 20.0);
        BARRIER_HEALTH_FACTOR = builder.comment("His Hex Barrier, which wraps his whole body as the Demiurge, has this many times its health.")
                .defineInRange("barrierHealthFactor", 2.0, 1.0, 20.0);
        MAGIC_TYPES = builder.comment("Whether H, as the Demiurge, gives his keys the magic of another witch or sorcerer (Magic Types).")
                .define("magicTypes", true);

        GIFTS = builder.comment("Whether the Demiurge can give magic to a player who has no hero (Sneak + G opens the screen).")
                .define("gifts", true);
        GIFT_STRENGTH = builder.comment("The strength (1-10, as in the Hero Creator) of every power he gives.")
                .defineInRange("giftStrength", 8, 1, 10);
        GIFT_MAX_POWERS = builder.comment("At most this many powers in one gift.")
                .defineInRange("giftMaxPowers", 10, 1, 20);
        GIFT_RANGE = builder.comment("How near (blocks) the player he gives magic to must be (0: anywhere in the same world).")
                .defineInRange("giftRange", 0.0, 0.0, 10000.0);
        builder.pop();
    }

    private DemiurgeConfig() {
    }
}
