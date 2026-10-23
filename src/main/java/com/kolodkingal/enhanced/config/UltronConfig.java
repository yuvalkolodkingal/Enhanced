package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The balance numbers of Ultron, the hero. Its options are defined in {@link #define}, which {@code EnhancedConfig}
 * calls while it builds the server config (TOML section {@code [ultron]}). The lightning and Vision numbers also apply
 * to the boss and its Sentries, so the boss fight reads them from here.
 */
public final class UltronConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "ultron";

    /**
     * Assigned by {@link #define} when the config is built, which is at mod construction: before anything reads it (it
     * cannot be final because the options are defined in a method, not in a static block).
     */
    public static ConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ConfigSpec.DoubleValue BODY_HEALTH;
    public static ConfigSpec.DoubleValue BODY_ARMOR;
    public static ConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ConfigSpec.DoubleValue LIGHTNING_FACTOR;
    public static ConfigSpec.IntValue VISION_CUT_RANGE;
    public static ConfigSpec.DoubleValue BOLT_DAMAGE;
    public static ConfigSpec.DoubleValue BOLT_SPLASH;
    public static ConfigSpec.IntValue BOLT_INTERVAL;
    public static ConfigSpec.IntValue BOLT_POOL;
    public static ConfigSpec.DoubleValue BOLT_REGEN_PER_SECOND;
    public static ConfigSpec.IntValue BOLT_RESTART;
    public static ConfigSpec.DoubleValue OVERCHARGE_DAMAGE;
    public static ConfigSpec.DoubleValue OVERCHARGE_SPLASH;
    public static ConfigSpec.DoubleValue OVERCHARGE_RADIUS;
    public static ConfigSpec.IntValue OVERCHARGE_COST;
    public static ConfigSpec.IntValue OVERCHARGE_CHARGE_TICKS;
    public static ConfigSpec.DoubleValue OVERCHARGE_SLOW;
    public static ConfigSpec.IntValue OVERCHARGE_COOLDOWN;
    public static ConfigSpec.IntValue PULL_RANGE;
    public static ConfigSpec.DoubleValue PULL_DAMAGE;
    public static ConfigSpec.IntValue PULL_COOLDOWN;
    public static ConfigSpec.DoubleValue SLAM_RADIUS;
    public static ConfigSpec.DoubleValue SLAM_DAMAGE;
    public static ConfigSpec.IntValue SLAM_COOLDOWN;
    public static ConfigSpec.IntValue UPLOAD_RANGE;
    public static ConfigSpec.DoubleValue UPLOAD_HEAL;
    public static ConfigSpec.IntValue UPLOAD_COOLDOWN;
    public static ConfigSpec.IntValue SECOND_LIFE_COOLDOWN;
    public static ConfigSpec.DoubleValue SECOND_LIFE_HEALTH;
    public static ConfigSpec.IntValue SECOND_LIFE_RANGE;
    public static ConfigSpec.IntValue HACK_RANGE;
    public static ConfigSpec.IntValue HACK_MAX;
    public static ConfigSpec.IntValue DRONE_COUNT;
    public static ConfigSpec.IntValue DRONE_MAX;
    public static ConfigSpec.IntValue DRONE_LIFETIME;
    public static ConfigSpec.IntValue DRONE_COOLDOWN;
    public static ConfigSpec.DoubleValue DRONE_HEALTH;
    public static ConfigSpec.DoubleValue DRONE_DAMAGE;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        FLIGHT_SPEED = builder.comment("Ultron's flying speed (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.10, 0.01, 0.5);
        BODY_HEALTH = builder.comment("Extra maximum health of the vibranium body.")
                .defineInRange("bodyHealth", 10.0, 0.0, 40.0);
        BODY_ARMOR = builder.comment("Extra armour of the vibranium body.")
                .defineInRange("bodyArmor", 8.0, 0.0, 30.0);
        KNOCKBACK_RESISTANCE = builder.comment("Extra knockback resistance of the vibranium body.")
                .defineInRange("knockbackResistance", 0.3, 0.0, 1.0);
        LIGHTNING_FACTOR = builder.comment("Lightning does this much to Ultron, his bodies and the Sentries (they are electronics).")
                .defineInRange("lightningFactor", 1.25, 1.0, 3.0);
        VISION_CUT_RANGE = builder.comment("A Vision this near cuts Ultron's uploads, the boss's and the hero's (0 turns it off).")
                .defineInRange("visionCutRange", 48, 0, 128);
        BOLT_DAMAGE = builder.comment("Damage of a plasma bolt.")
                .defineInRange("boltDamage", 4.0, 0.0, 100.0);
        BOLT_SPLASH = builder.comment("The radius in which others near the bolt's target take half.")
                .defineInRange("boltSplash", 1.2, 0.0, 4.0);
        BOLT_INTERVAL = builder.comment("Ticks between bolts while R is held.")
                .defineInRange("boltInterval", 4, 1, 20);
        BOLT_POOL = builder.comment("The plasma pool: how many bolts he can fire before he is empty.")
                .defineInRange("boltPool", 30, 5, 500);
        BOLT_REGEN_PER_SECOND = builder.comment("How fast the plasma pool refills, per second (after half a second without firing).")
                .defineInRange("boltRegenPerSecond", 4.0, 0.0, 100.0);
        BOLT_RESTART = builder.comment("How much plasma he needs to fire again once the pool is empty.")
                .defineInRange("boltRestart", 6, 1, 100);
        OVERCHARGE_DAMAGE = builder.comment("The Overcharge's hit.")
                .defineInRange("overchargeDamage", 14.0, 0.0, 200.0);
        OVERCHARGE_SPLASH = builder.comment("The Overcharge's splash damage.")
                .defineInRange("overchargeSplash", 8.0, 0.0, 200.0);
        OVERCHARGE_RADIUS = builder.comment("The Overcharge's splash radius.")
                .defineInRange("overchargeRadius", 3.5, 0.5, 10.0);
        OVERCHARGE_COST = builder.comment("How much plasma the Overcharge costs.")
                .defineInRange("overchargeCost", 10, 0, 100);
        OVERCHARGE_CHARGE_TICKS = builder.comment("How long the Overcharge charges.")
                .defineInRange("overchargeChargeTicks", 15, 1, 100);
        OVERCHARGE_SLOW = builder.comment("How much slower he moves while he charges (0.6 = 40% of his speed).")
                .defineInRange("overchargeSlow", 0.6, 0.0, 0.95);
        OVERCHARGE_COOLDOWN = builder.comment("The Overcharge's cooldown, in ticks.")
                .defineInRange("overchargeCooldown", 160, 0, 6000);
        PULL_RANGE = builder.comment("Gravity Pull: how far it reaches.")
                .defineInRange("pullRange", 24, 4, 64);
        PULL_DAMAGE = builder.comment("Gravity Pull: its damage.")
                .defineInRange("pullDamage", 3.0, 0.0, 100.0);
        PULL_COOLDOWN = builder.comment("Gravity Pull: its cooldown, in ticks.")
                .defineInRange("pullCooldown", 80, 0, 6000);
        SLAM_RADIUS = builder.comment("Gravity Slam: its radius.")
                .defineInRange("slamRadius", 7.0, 2.0, 24.0);
        SLAM_DAMAGE = builder.comment("Gravity Slam: its damage.")
                .defineInRange("slamDamage", 6.0, 0.0, 100.0);
        SLAM_COOLDOWN = builder.comment("Gravity Slam: its cooldown, in ticks.")
                .defineInRange("slamCooldown", 160, 0, 6000);
        UPLOAD_RANGE = builder.comment("Upload: how near a drone must be.")
                .defineInRange("uploadRange", 48, 8, 128);
        UPLOAD_HEAL = builder.comment("Upload: how much health it heals.")
                .defineInRange("uploadHeal", 8.0, 0.0, 40.0);
        UPLOAD_COOLDOWN = builder.comment("Upload: its cooldown, in ticks.")
                .defineInRange("uploadCooldown", 500, 0, 12000);
        SECOND_LIFE_COOLDOWN = builder.comment("Second Life: its recharge, in ticks.")
                .defineInRange("secondLifeCooldown", 6000, 0, 72000);
        SECOND_LIFE_HEALTH = builder.comment("Second Life: the health he comes back with, as a fraction of the maximum.")
                .defineInRange("secondLifeHealth", 0.25, 0.05, 1.0);
        SECOND_LIFE_RANGE = builder.comment("Second Life: how near a drone must be.")
                .defineInRange("secondLifeRange", 64, 8, 256);
        HACK_RANGE = builder.comment("Hack: how far it reaches.")
                .defineInRange("hackRange", 32, 4, 64);
        HACK_MAX = builder.comment("Hack: how many hacked Sentries he can have at a time.")
                .defineInRange("hackMax", 2, 0, 6);
        DRONE_COUNT = builder.comment("Sentry Army: how many Sentries one use calls.")
                .defineInRange("droneCount", 3, 1, 6);
        DRONE_MAX = builder.comment("Sentry Army: the most drones he can have at a time.")
                .defineInRange("droneMax", 6, 1, 12);
        DRONE_LIFETIME = builder.comment("Sentry Army: how long a drone lasts, in ticks.")
                .defineInRange("droneLifetime", 1800, 100, 72000);
        DRONE_COOLDOWN = builder.comment("Sentry Army: its cooldown, in ticks.")
                .defineInRange("droneCooldown", 400, 0, 12000);
        DRONE_HEALTH = builder.comment("A drone's health.")
                .defineInRange("droneHealth", 24.0, 4.0, 200.0);
        DRONE_DAMAGE = builder.comment("A drone's plasma bolt.")
                .defineInRange("droneDamage", 3.0, 0.0, 100.0);
        builder.pop();
    }

    private UltronConfig() {
    }
}
