package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Eternity and Love (3.8, lane L8): the Shrine of the Cosmic Entities, Eternity's realm and the wish, and Love, Thor's
 * daughter. Every option of the plan is defined here by the 3.8 skeleton (the lanes read them); {@code EnhancedConfig}
 * calls {@link #define} while it builds the server config. They are read from the server and read when used (never
 * cached), so an edit applies at once. TOML sections {@code [eternity]} and {@code [love]} (the latter's options
 * prefixed {@code LOVE_}).
 */
public final class EternityConfig {
    /** The TOML sections of these options. */
    public static final String SECTION_ETERNITY = "eternity";
    public static final String SECTION_LOVE = "love";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.IntValue ALTAR_CHARGE_TICKS;
    public static ConfigSpec.IntValue WISHES_PER_PLAYER;
    public static ConfigSpec.BooleanValue WISH_FULL_STACK;
    public static ConfigSpec.BooleanValue LOVE_WISH;
    public static ConfigSpec.IntValue RETURN_DELAY_TICKS;
    public static ConfigSpec.IntValue SCREEN_DELAY_TICKS;
    public static ConfigSpec.DoubleValue LOVE_HEALTH_BONUS;
    public static ConfigSpec.DoubleValue LOVE_ARMOR_BONUS;
    public static ConfigSpec.DoubleValue LOVE_ATTACK_BONUS;
    public static ConfigSpec.DoubleValue LOVE_KNOCKBACK_RESISTANCE;
    public static ConfigSpec.DoubleValue LOVE_SAFE_FALL;
    public static ConfigSpec.DoubleValue LOVE_FLIGHT_SPEED;
    public static ConfigSpec.BooleanValue LOVE_FLIGHT_NEEDS_AXE;
    public static ConfigSpec.BooleanValue LOVE_WORTHY;
    public static ConfigSpec.DoubleValue LOVE_STRIKE_DAMAGE;
    public static ConfigSpec.DoubleValue LOVE_STRIKE_RADIUS;
    public static ConfigSpec.IntValue LOVE_STRIKE_RANGE;
    public static ConfigSpec.IntValue LOVE_STRIKE_COOLDOWN;
    public static ConfigSpec.BooleanValue LOVE_STRIKE_SETS_FIRE;
    public static ConfigSpec.DoubleValue LOVE_EYE_LASER_DAMAGE;
    public static ConfigSpec.IntValue LOVE_EYE_LASER_RANGE;
    public static ConfigSpec.IntValue LOVE_EYE_LASER_HIT_GAP;
    public static ConfigSpec.IntValue LOVE_EYE_LASER_FIRE_SECONDS;
    public static ConfigSpec.IntValue LOVE_EYE_LASER_MAX_TICKS;
    public static ConfigSpec.IntValue LOVE_EYE_LASER_COOLDOWN;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("eternity");
        ENABLED = builder.comment("Whether the Shrine of the Cosmic Entities generates in new End chunks (existing",
                "shrines stay).")
                .define("enabled", true);
        ALTAR_CHARGE_TICKS = builder.comment("Ticks the Bifrost key floats over the altar before the journey.")
                .defineInRange("altarChargeTicks", 60, 1, 1200);
        WISHES_PER_PLAYER = builder.comment("Wishes each player may make (0: unlimited).")
                .defineInRange("wishesPerPlayer", 1, 0, 64);
        WISH_FULL_STACK = builder.comment("Whether a wished item comes as a full stack (false: one).")
                .define("wishFullStack", true);
        LOVE_WISH = builder.comment("Whether one may wish to become Love.")
                .define("loveWish", true);
        RETURN_DELAY_TICKS = builder.comment("Ticks after a wish before the traveller is sent home.")
                .defineInRange("returnDelayTicks", 60, 0, 1200);
        SCREEN_DELAY_TICKS = builder.comment("Ticks after arriving before Eternity speaks (the screen opens).")
                .defineInRange("screenDelayTicks", 40, 0, 1200);
        builder.pop();
        builder.push("love");
        LOVE_HEALTH_BONUS = builder.comment("Max health added to her 20 (32: sixteen hearts).")
                .defineInRange("healthBonus", 12.0, 0.0, 100.0);
        LOVE_ARMOR_BONUS = builder.comment("Armour added.")
                .defineInRange("armorBonus", 4.0, 0.0, 30.0);
        LOVE_ATTACK_BONUS = builder.comment("Attack damage added.")
                .defineInRange("attackBonus", 3.0, 0.0, 40.0);
        LOVE_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance.")
                .defineInRange("knockbackResistance", 0.3, 0.0, 1.0);
        LOVE_SAFE_FALL = builder.comment("Safe fall distance added (blocks).")
                .defineInRange("safeFall", 6.0, 0.0, 64.0);
        LOVE_FLIGHT_SPEED = builder.comment("Flight speed.")
                .defineInRange("flightSpeed", 0.1, 0.01, 0.5);
        LOVE_FLIGHT_NEEDS_AXE = builder.comment("Whether Love flies only while she holds Stormbreaker (as Thor).")
                .define("flightNeedsAxe", true);
        LOVE_WORTHY = builder.comment("Whether Love is worthy of Mjolnir.")
                .define("worthy", true);
        LOVE_STRIKE_DAMAGE = builder.comment("Damage of the Thunder Strike.")
                .defineInRange("strikeDamage", 8.0, 0.0, 200.0);
        LOVE_STRIKE_RADIUS = builder.comment("Radius (blocks) of the Thunder Strike.")
                .defineInRange("strikeRadius", 2.0, 0.0, 16.0);
        LOVE_STRIKE_RANGE = builder.comment("Range (blocks) of the Thunder Strike.")
                .defineInRange("strikeRange", 48, 4, 128);
        LOVE_STRIKE_COOLDOWN = builder.comment("Cooldown (ticks) of the Thunder Strike.")
                .defineInRange("strikeCooldown", 100, 0, 72000);
        LOVE_STRIKE_SETS_FIRE = builder.comment("Whether the Thunder Strike sets blocks on fire (only with",
                "mobGriefing too).")
                .define("strikeSetsFire", false);
        LOVE_EYE_LASER_DAMAGE = builder.comment("Damage of the Eye Laser to each creature it touches.")
                .defineInRange("eyeLaserDamage", 6.0, 0.0, 200.0);
        LOVE_EYE_LASER_RANGE = builder.comment("Range (blocks) of the Eye Laser.")
                .defineInRange("eyeLaserRange", 24, 4, 128);
        LOVE_EYE_LASER_HIT_GAP = builder.comment("Ticks before the Eye Laser hurts the same creature again.")
                .defineInRange("eyeLaserHitGap", 20, 1, 400);
        LOVE_EYE_LASER_FIRE_SECONDS = builder.comment("Seconds the Eye Laser sets creatures burning.")
                .defineInRange("eyeLaserFireSeconds", 4, 0, 60);
        LOVE_EYE_LASER_MAX_TICKS = builder.comment("The longest (ticks) the Eye Laser can be held.")
                .defineInRange("eyeLaserMaxTicks", 100, 1, 1200);
        LOVE_EYE_LASER_COOLDOWN = builder.comment("Cooldown (ticks) of the Eye Laser.")
                .defineInRange("eyeLaserCooldown", 60, 0, 72000);
        builder.pop();
    }

    private EternityConfig() {
    }
}
