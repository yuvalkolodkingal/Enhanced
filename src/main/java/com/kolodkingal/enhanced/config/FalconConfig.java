package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Falcon and the Exo-Wings (3.8, lane L5). Every option of the plan is defined here by the 3.8 skeleton (the lanes
 * read them); {@code EnhancedConfig} calls {@link #define} while it builds the server config. They are read from the
 * server and read when used (never cached), so an edit applies at once. TOML section {@code [falcon]}.
 */
public final class FalconConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "falcon";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.DoubleValue HEALTH_BONUS;
    public static ModConfigSpec.DoubleValue SPEED_BONUS;
    public static ModConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue GLIDE_BOOST;
    public static ModConfigSpec.IntValue GLIDE_BOOST_TICKS;
    public static ModConfigSpec.DoubleValue DART_DAMAGE;
    public static ModConfigSpec.DoubleValue DART_SPEED;
    public static ModConfigSpec.IntValue DART_RANGE;
    public static ModConfigSpec.IntValue DART_GAP;
    public static ModConfigSpec.DoubleValue ROCKET_DAMAGE;
    public static ModConfigSpec.DoubleValue ROCKET_POWER;
    public static ModConfigSpec.IntValue ROCKET_COOLDOWN;
    public static ModConfigSpec.IntValue DIVE_RANGE;
    public static ModConfigSpec.DoubleValue DIVE_SPEED;
    public static ModConfigSpec.DoubleValue DIVE_DAMAGE;
    public static ModConfigSpec.DoubleValue DIVE_SPLASH;
    public static ModConfigSpec.DoubleValue DIVE_MIN_HEIGHT;
    public static ModConfigSpec.IntValue DIVE_COOLDOWN;
    public static ModConfigSpec.DoubleValue CLIMB_HEIGHT;
    public static ModConfigSpec.IntValue CLIMB_COOLDOWN;
    public static ModConfigSpec.IntValue SHIELD_MAX_TICKS;
    public static ModConfigSpec.DoubleValue SHIELD_MELEE_REDUCTION;
    public static ModConfigSpec.DoubleValue SHIELD_EXPLOSION_REDUCTION;
    public static ModConfigSpec.IntValue SHIELD_COOLDOWN;
    public static ModConfigSpec.DoubleValue SLASH_DAMAGE;
    public static ModConfigSpec.DoubleValue SLASH_RADIUS;
    public static ModConfigSpec.IntValue SLASH_COOLDOWN;
    public static ModConfigSpec.DoubleValue REDWING_HEALTH;
    public static ModConfigSpec.IntValue REDWING_TICKS;
    public static ModConfigSpec.DoubleValue REDWING_MARK_RADIUS;
    public static ModConfigSpec.DoubleValue REDWING_ZAP_DAMAGE;
    public static ModConfigSpec.DoubleValue REDWING_ZAP_RANGE;
    public static ModConfigSpec.IntValue REDWING_ZAP_GAP;
    public static ModConfigSpec.IntValue REDWING_REPAIR_TICKS;
    public static ModConfigSpec.DoubleValue STRIKE_DAMAGE;
    public static ModConfigSpec.IntValue STRIKE_RANGE;
    public static ModConfigSpec.IntValue STRIKE_COOLDOWN;
    public static ModConfigSpec.IntValue WINGS_DURABILITY;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("falcon");
        HEALTH_BONUS = builder.comment("Max health added to his 20.")
                .defineInRange("healthBonus", 4.0, 0.0, 100.0);
        SPEED_BONUS = builder.comment("Movement speed added, as a fraction.")
                .defineInRange("speedBonus", 0.1, 0.0, 2.0);
        FLIGHT_SPEED = builder.comment("Flight speed with the wings.")
                .defineInRange("flightSpeed", 0.1, 0.01, 0.5);
        GLIDE_BOOST = builder.comment("Speed the nozzles add while gliding upward.")
                .defineInRange("glideBoost", 0.6, 0.0, 4.0);
        GLIDE_BOOST_TICKS = builder.comment("Ticks between two glide boosts.")
                .defineInRange("glideBoostTicks", 40, 1, 1200);
        DART_DAMAGE = builder.comment("Damage of each energy dart.")
                .defineInRange("dartDamage", 4.0, 0.0, 200.0);
        DART_SPEED = builder.comment("Speed of the energy darts.")
                .defineInRange("dartSpeed", 3.0, 0.2, 8.0);
        DART_RANGE = builder.comment("Range (blocks) of the energy darts.")
                .defineInRange("dartRange", 40, 4, 128);
        DART_GAP = builder.comment("Ticks between two pairs of darts.")
                .defineInRange("dartGap", 6, 1, 200);
        ROCKET_DAMAGE = builder.comment("Damage of the Wrist Rocket.")
                .defineInRange("rocketDamage", 8.0, 0.0, 200.0);
        ROCKET_POWER = builder.comment("Power of the Wrist Rocket's small boom (it never breaks blocks).")
                .defineInRange("rocketPower", 1.0, 0.0, 8.0);
        ROCKET_COOLDOWN = builder.comment("Cooldown (ticks) of the Wrist Rocket.")
                .defineInRange("rocketCooldown", 80, 0, 72000);
        DIVE_RANGE = builder.comment("Range (blocks) of the Dive.")
                .defineInRange("diveRange", 32, 4, 128);
        DIVE_SPEED = builder.comment("Speed (blocks per tick) of the Dive.")
                .defineInRange("diveSpeed", 1.8, 0.2, 6.0);
        DIVE_DAMAGE = builder.comment("Damage of the Dive.")
                .defineInRange("diveDamage", 10.0, 0.0, 200.0);
        DIVE_SPLASH = builder.comment("Damage of the Dive to creatures near the one it hits.")
                .defineInRange("diveSplash", 3.0, 0.0, 200.0);
        DIVE_MIN_HEIGHT = builder.comment("The least height (blocks) above the ground for a Dive.")
                .defineInRange("diveMinHeight", 4.0, 0.0, 64.0);
        DIVE_COOLDOWN = builder.comment("Cooldown (ticks) of the Dive.")
                .defineInRange("diveCooldown", 100, 0, 72000);
        CLIMB_HEIGHT = builder.comment("Height (blocks) of the Boost Climb.")
                .defineInRange("climbHeight", 24.0, 1.0, 128.0);
        CLIMB_COOLDOWN = builder.comment("Cooldown (ticks) of the Boost Climb.")
                .defineInRange("climbCooldown", 60, 0, 72000);
        SHIELD_MAX_TICKS = builder.comment("The longest (ticks) the Wing Shield can be held.")
                .defineInRange("shieldMaxTicks", 100, 1, 1200);
        SHIELD_MELEE_REDUCTION = builder.comment("Fraction of a blow from the front the Wing Shield takes away.")
                .defineInRange("shieldMeleeReduction", 0.7, 0.0, 1.0);
        SHIELD_EXPLOSION_REDUCTION = builder.comment("Fraction of an explosion from the front the Wing Shield takes",
                "away.")
                .defineInRange("shieldExplosionReduction", 0.5, 0.0, 1.0);
        SHIELD_COOLDOWN = builder.comment("Cooldown (ticks) of the Wing Shield.")
                .defineInRange("shieldCooldown", 60, 0, 72000);
        SLASH_DAMAGE = builder.comment("Damage of the Wing Slash.")
                .defineInRange("slashDamage", 7.0, 0.0, 200.0);
        SLASH_RADIUS = builder.comment("Radius (blocks) of the Wing Slash.")
                .defineInRange("slashRadius", 3.0, 1.0, 16.0);
        SLASH_COOLDOWN = builder.comment("Cooldown (ticks) of the Wing Slash.")
                .defineInRange("slashCooldown", 80, 0, 72000);
        REDWING_HEALTH = builder.comment("Redwing's health.")
                .defineInRange("redwingHealth", 12.0, 1.0, 200.0);
        REDWING_TICKS = builder.comment("Ticks Redwing stays out.")
                .defineInRange("redwingTicks", 1200, 20, 72000);
        REDWING_MARK_RADIUS = builder.comment("Radius (blocks) in which Redwing marks monsters.")
                .defineInRange("redwingMarkRadius", 32.0, 4.0, 128.0);
        REDWING_ZAP_DAMAGE = builder.comment("Damage of Redwing's zap.")
                .defineInRange("redwingZapDamage", 2.0, 0.0, 200.0);
        REDWING_ZAP_RANGE = builder.comment("Range (blocks) of Redwing's zap.")
                .defineInRange("redwingZapRange", 16.0, 1.0, 64.0);
        REDWING_ZAP_GAP = builder.comment("Ticks between two zaps of Redwing.")
                .defineInRange("redwingZapGap", 20, 1, 400);
        REDWING_REPAIR_TICKS = builder.comment("Ticks before a shot-down Redwing can fly again.")
                .defineInRange("redwingRepairTicks", 600, 0, 72000);
        STRIKE_DAMAGE = builder.comment("Damage of Redwing Strike.")
                .defineInRange("strikeDamage", 8.0, 0.0, 200.0);
        STRIKE_RANGE = builder.comment("Range (blocks) of Redwing Strike.")
                .defineInRange("strikeRange", 32, 4, 128);
        STRIKE_COOLDOWN = builder.comment("Cooldown (ticks) of Redwing Strike.")
                .defineInRange("strikeCooldown", 200, 0, 72000);
        WINGS_DURABILITY = builder.comment("Durability of the Exo-Wings.")
                .defineInRange("wingsDurability", 900, 1, 100000);
        builder.pop();
    }

    private FalconConfig() {
    }
}
