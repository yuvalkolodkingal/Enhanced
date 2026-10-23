package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The Hulk and the Gamma Emitter (3.6). Every option of the plan is defined here by the 3.6 skeleton (lane L7 reads
 * them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML sections {@code [hulk]}
 * and {@code [gamma]}). Every number is the Ragnarok Hulk's; the Brand New Day Hulk's damage is it times {@code
 * bndDamageFactor}. They are read from the server and synced to clients, and read when used (never cached).
 */
public final class HulkConfig {
    /** What the Hulk breaks (always only with the gamerule mobGriefing and where the protections allow). */
    public enum BlockBreaking {
        /** Nothing. */
        NONE,
        /** Soft blocks only: glass, leaves, snow, ice, flowers, grass. */
        SOFT,
        /** Small craters in dirt, stone and wood too. */
        ALL
    }

    /** The TOML sections of these options. */
    public static final String SECTION_HULK = "hulk";
    public static final String SECTION_GAMMA = "gamma";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.EnumValue<BlockBreaking> BLOCK_BREAKING;
    public static ConfigSpec.BooleanValue NEEDS_MOB_GRIEFING;
    public static ConfigSpec.DoubleValue HEALTH_BONUS;
    public static ConfigSpec.DoubleValue ARMOR_BONUS;
    public static ConfigSpec.DoubleValue TOUGHNESS_BONUS;
    public static ConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ConfigSpec.DoubleValue ATTACK_BONUS;
    public static ConfigSpec.DoubleValue ATTACK_KNOCKBACK;
    public static ConfigSpec.DoubleValue SCALE_RAGNAROK;
    public static ConfigSpec.DoubleValue SCALE_BRAND_NEW_DAY;
    public static ConfigSpec.DoubleValue BND_DAMAGE_FACTOR;
    public static ConfigSpec.DoubleValue REACH_RAGNAROK;
    public static ConfigSpec.DoubleValue REACH_BRAND_NEW_DAY;
    public static ConfigSpec.DoubleValue JUMP_BONUS;
    public static ConfigSpec.DoubleValue SAFE_FALL;
    public static ConfigSpec.DoubleValue STEP_BONUS;
    public static ConfigSpec.DoubleValue SPEED_BONUS;
    public static ConfigSpec.DoubleValue BREAK_SPEED_FACTOR;
    public static ConfigSpec.DoubleValue REGEN_PER_SECOND;
    public static ConfigSpec.DoubleValue EXHAUSTION_FACTOR;
    public static ConfigSpec.DoubleValue SMASH_DAMAGE;
    public static ConfigSpec.IntValue SMASH_RADIUS;
    public static ConfigSpec.IntValue SMASH_COOLDOWN;
    public static ConfigSpec.DoubleValue GRAB_DAMAGE1;
    public static ConfigSpec.DoubleValue GRAB_DAMAGE2;
    public static ConfigSpec.DoubleValue GRAB_DAMAGE3;
    public static ConfigSpec.DoubleValue GRAB_BOSS_PUNCH;
    public static ConfigSpec.IntValue GRAB_COOLDOWN;
    public static ConfigSpec.DoubleValue CLAP_DAMAGE;
    public static ConfigSpec.IntValue CLAP_LENGTH;
    public static ConfigSpec.IntValue CLAP_ANGLE;
    public static ConfigSpec.DoubleValue CLAP_KNOCKBACK;
    public static ConfigSpec.BooleanValue CLAP_EXTINGUISHES;
    public static ConfigSpec.IntValue CLAP_COOLDOWN;
    public static ConfigSpec.IntValue ROAR_RADIUS;
    public static ConfigSpec.IntValue ROAR_FRIGHT_TICKS;
    public static ConfigSpec.IntValue ROAR_WEAKNESS_TICKS;
    public static ConfigSpec.IntValue ROAR_COOLDOWN;
    public static ConfigSpec.DoubleValue LEAP_DAMAGE;
    public static ConfigSpec.IntValue LEAP_RADIUS;
    public static ConfigSpec.IntValue LEAP_RANGE;
    public static ConfigSpec.IntValue LEAP_COOLDOWN;
    public static ConfigSpec.IntValue BOUND_DISTANCE;
    public static ConfigSpec.IntValue BOUND_COOLDOWN;
    public static ConfigSpec.DoubleValue BOULDER_DAMAGE;
    public static ConfigSpec.DoubleValue BOULDER_SPLASH;
    public static ConfigSpec.IntValue BOULDER_COOLDOWN;
    public static ConfigSpec.DoubleValue CHARGE_DAMAGE;
    public static ConfigSpec.IntValue CHARGE_DISTANCE;
    public static ConfigSpec.IntValue CHARGE_COOLDOWN;
    public static ConfigSpec.IntValue TRANSFORM_TICKS;
    public static ConfigSpec.IntValue TRANSFORM_COOLDOWN;
    public static ConfigSpec.IntValue SWITCH_COOLDOWN;
    public static ConfigSpec.BooleanValue RAGE;
    public static ConfigSpec.DoubleValue RAGE_DAMAGE_BONUS;
    public static ConfigSpec.BooleanValue BANNER_ANGER_TRANSFORM;
    public static ConfigSpec.IntValue BANNER_ANGER_PER_HEART;
    public static ConfigSpec.BooleanValue BANNER_LOW_HEALTH_TRANSFORM;
    public static ConfigSpec.BooleanValue WIELDS_GAUNTLET;
    public static ConfigSpec.IntValue ARM_BURNED_SECONDS;
    public static ConfigSpec.BooleanValue EMITTER_CRAFTABLE;
    public static ConfigSpec.IntValue WARMUP_TICKS;
    public static ConfigSpec.IntValue EXPOSURE_TICKS;
    public static ConfigSpec.IntValue BEAM_TICKS;
    public static ConfigSpec.IntValue BEAM_LENGTH;
    public static ConfigSpec.IntValue EMITTER_COOLDOWN;
    public static ConfigSpec.BooleanValue HEALS_HULK;
    public static ConfigSpec.DoubleValue HEAL_PER_SECOND;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("hulk");
        BLOCK_BREAKING = builder.comment("What the Hulk breaks: NONE, SOFT (glass, leaves, snow, ice, flowers, grass;",
                "never doors, crops or beds) or ALL.")
                .defineEnum("blockBreaking", BlockBreaking.SOFT);
        NEEDS_MOB_GRIEFING = builder.comment("Whether every block the Hulk breaks also needs the gamerule",
                "mobGriefing.")
                .define("needsMobGriefing", true);
        HEALTH_BONUS = builder.comment("Health the Hulk's body adds.")
                .defineInRange("healthBonus", 20.0, 0.0, 200.0);
        ARMOR_BONUS = builder.comment("Armour the Hulk's body adds.")
                .defineInRange("armorBonus", 8.0, 0.0, 30.0);
        TOUGHNESS_BONUS = builder.comment("Armour toughness the Hulk's body adds.")
                .defineInRange("toughnessBonus", 4.0, 0.0, 20.0);
        KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the Hulk.")
                .defineInRange("knockbackResistance", 0.9, 0.0, 1.0);
        ATTACK_BONUS = builder.comment("Attack damage the Hulk's fists add.")
                .defineInRange("attackBonus", 7.0, 0.0, 100.0);
        ATTACK_KNOCKBACK = builder.comment("Attack knockback the Hulk's fists add.")
                .defineInRange("attackKnockback", 1.5, 0.0, 10.0);
        SCALE_RAGNAROK = builder.comment("How much bigger the Ragnarok Hulk is (0.5: x1.5).")
                .defineInRange("scaleRagnarok", 0.5, 0.0, 1.5);
        SCALE_BRAND_NEW_DAY = builder.comment("How much bigger the Brand New Day Hulk is (0.75: x1.75).")
                .defineInRange("scaleBrandNewDay", 0.75, 0.0, 1.5);
        BND_DAMAGE_FACTOR = builder.comment("Every damage of the Brand New Day Hulk is multiplied by this.")
                .defineInRange("bndDamageFactor", 1.15, 1.0, 2.0);
        REACH_RAGNAROK = builder.comment("Reach the Ragnarok Hulk's long arms add.")
                .defineInRange("reachRagnarok", 1.5, 0.0, 6.0);
        REACH_BRAND_NEW_DAY = builder.comment("Reach the Brand New Day Hulk's long arms add.")
                .defineInRange("reachBrandNewDay", 2.0, 0.0, 6.0);
        JUMP_BONUS = builder.comment("Jump strength the Hulk adds (his giant jumps).")
                .defineInRange("jumpBonus", 0.5, 0.0, 2.0);
        SAFE_FALL = builder.comment("Safe fall distance the Hulk adds.")
                .defineInRange("safeFall", 24.0, 0.0, 256.0);
        STEP_BONUS = builder.comment("Step height the Hulk adds.")
                .defineInRange("stepBonus", 0.6, 0.0, 2.0);
        SPEED_BONUS = builder.comment("Movement speed the Hulk adds (0.15: +15%).")
                .defineInRange("speedBonus", 0.15, 0.0, 1.0);
        BREAK_SPEED_FACTOR = builder.comment("How many times faster the Hulk breaks blocks by hand.")
                .defineInRange("breakSpeedFactor", 4.0, 1.0, 20.0);
        REGEN_PER_SECOND = builder.comment("Health the Hulk heals per second when not hurt for 5 seconds.")
                .defineInRange("regenPerSecond", 0.35, 0.0, 10.0);
        EXHAUSTION_FACTOR = builder.comment("How many times hungrier the Hulk gets.")
                .defineInRange("exhaustionFactor", 1.5, 1.0, 5.0);
        SMASH_DAMAGE = builder.comment("Damage of Hulk Smash to everyone on the ground near.")
                .defineInRange("smashDamage", 7.0, 0.0, 100.0);
        SMASH_RADIUS = builder.comment("Radius (blocks) of Hulk Smash.")
                .defineInRange("smashRadius", 6, 1, 32);
        SMASH_COOLDOWN = builder.comment("Cooldown (ticks) of Hulk Smash.")
                .defineInRange("smashCooldown", 60, 0, 72000);
        GRAB_DAMAGE1 = builder.comment("Damage of the first slam of the Grab Smash.")
                .defineInRange("grabDamage1", 4.0, 0.0, 100.0);
        GRAB_DAMAGE2 = builder.comment("Damage of the second slam.")
                .defineInRange("grabDamage2", 4.0, 0.0, 100.0);
        GRAB_DAMAGE3 = builder.comment("Damage of the third slam.")
                .defineInRange("grabDamage3", 6.0, 0.0, 100.0);
        GRAB_BOSS_PUNCH = builder.comment("Damage of the one heavy punch a boss or a grab-immune creature gets",
                "instead.")
                .defineInRange("grabBossPunch", 8.0, 0.0, 100.0);
        GRAB_COOLDOWN = builder.comment("Cooldown (ticks) of the Grab Smash.")
                .defineInRange("grabCooldown", 160, 0, 72000);
        CLAP_DAMAGE = builder.comment("Damage of the Thunder Clap.")
                .defineInRange("clapDamage", 6.0, 0.0, 100.0);
        CLAP_LENGTH = builder.comment("Length (blocks) of the Thunder Clap's shockwave.")
                .defineInRange("clapLength", 14, 2, 64);
        CLAP_ANGLE = builder.comment("Width (degrees) of the Thunder Clap's cone.")
                .defineInRange("clapAngle", 50, 10, 180);
        CLAP_KNOCKBACK = builder.comment("How far the Thunder Clap throws.")
                .defineInRange("clapKnockback", 3.0, 0.0, 10.0);
        CLAP_EXTINGUISHES = builder.comment("Whether the Thunder Clap puts out fire.")
                .define("clapExtinguishes", true);
        CLAP_COOLDOWN = builder.comment("Cooldown (ticks) of the Thunder Clap.")
                .defineInRange("clapCooldown", 100, 0, 72000);
        ROAR_RADIUS = builder.comment("Radius (blocks) of the Roar.")
                .defineInRange("roarRadius", 12, 2, 64);
        ROAR_FRIGHT_TICKS = builder.comment("Ticks hostile creatures stay frightened by the Roar.")
                .defineInRange("roarFrightTicks", 80, 0, 1200);
        ROAR_WEAKNESS_TICKS = builder.comment("Ticks of Weakness the Roar gives.")
                .defineInRange("roarWeaknessTicks", 160, 0, 1200);
        ROAR_COOLDOWN = builder.comment("Cooldown (ticks) of the Roar.")
                .defineInRange("roarCooldown", 300, 0, 72000);
        LEAP_DAMAGE = builder.comment("Damage where the Super Leap lands.")
                .defineInRange("leapDamage", 6.0, 0.0, 100.0);
        LEAP_RADIUS = builder.comment("Radius (blocks) of the Super Leap's landing.")
                .defineInRange("leapRadius", 4, 1, 16);
        LEAP_RANGE = builder.comment("Range (blocks) of the Super Leap.")
                .defineInRange("leapRange", 48, 8, 128);
        LEAP_COOLDOWN = builder.comment("Cooldown (ticks) of the Super Leap.")
                .defineInRange("leapCooldown", 120, 0, 72000);
        BOUND_DISTANCE = builder.comment("Distance (blocks) of the Bound.")
                .defineInRange("boundDistance", 24, 4, 64);
        BOUND_COOLDOWN = builder.comment("Cooldown (ticks) of the Bound.")
                .defineInRange("boundCooldown", 30, 0, 72000);
        BOULDER_DAMAGE = builder.comment("Damage of the boulder to the creature it hits.")
                .defineInRange("boulderDamage", 8.0, 0.0, 100.0);
        BOULDER_SPLASH = builder.comment("Splash damage of the boulder.")
                .defineInRange("boulderSplash", 4.0, 0.0, 100.0);
        BOULDER_COOLDOWN = builder.comment("Cooldown (ticks) of the Boulder Throw.")
                .defineInRange("boulderCooldown", 80, 0, 72000);
        CHARGE_DAMAGE = builder.comment("Damage of the Gamma Charge.")
                .defineInRange("chargeDamage", 6.0, 0.0, 100.0);
        CHARGE_DISTANCE = builder.comment("Distance (blocks) of the Gamma Charge.")
                .defineInRange("chargeDistance", 12, 2, 48);
        CHARGE_COOLDOWN = builder.comment("Cooldown (ticks) of the Gamma Charge.")
                .defineInRange("chargeCooldown", 140, 0, 72000);
        TRANSFORM_TICKS = builder.comment("Ticks the change between Bruce Banner and the Hulk takes.")
                .defineInRange("transformTicks", 40, 1, 200);
        TRANSFORM_COOLDOWN = builder.comment("Cooldown (ticks) of the Transform.")
                .defineInRange("transformCooldown", 100, 0, 72000);
        SWITCH_COOLDOWN = builder.comment("Cooldown (ticks) of Switch Hulk.")
                .defineInRange("switchCooldown", 40, 0, 72000);
        RAGE = builder.comment("Whether the Hulk has his Rage meter.")
                .define("rage", true);
        RAGE_DAMAGE_BONUS = builder.comment("The most the Rage adds to his damage (0.3: +30%).")
                .defineInRange("rageDamageBonus", 0.3, 0.0, 2.0);
        BANNER_ANGER_TRANSFORM = builder.comment("Whether Bruce Banner Hulks out by himself when angry.")
                .define("bannerAngerTransform", true);
        BANNER_ANGER_PER_HEART = builder.comment("Anger Banner gains per heart of damage.")
                .defineInRange("bannerAngerPerHeart", 10, 0, 100);
        BANNER_LOW_HEALTH_TRANSFORM = builder.comment("Whether Banner gets much angrier below 4 health (the Hulk",
                "protects Banner).")
                .define("bannerLowHealthTransform", true);
        WIELDS_GAUNTLET = builder.comment("Whether the Hulk may wield a filled Infinity Gauntlet and Snap (Endgame).")
                .define("wieldsGauntlet", true);
        ARM_BURNED_SECONDS = builder.comment("Seconds the Hulk's arm stays burned after his Snap.")
                .defineInRange("armBurnedSeconds", 600, 0, 7200);
        builder.pop();
        builder.push("gamma");
        EMITTER_CRAFTABLE = builder.comment("Whether the Gamma Emitter can be crafted.")
                .define("emitterCraftable", true);
        WARMUP_TICKS = builder.comment("Ticks the Gamma Emitter warms up before its beam.")
                .defineInRange("warmupTicks", 60, 0, 400);
        EXPOSURE_TICKS = builder.comment("Ticks a powerless player stands in the beam to become the Hulk.")
                .defineInRange("exposureTicks", 60, 1, 400);
        BEAM_TICKS = builder.comment("Ticks the beam lasts.")
                .defineInRange("beamTicks", 60, 1, 400);
        BEAM_LENGTH = builder.comment("Length (blocks) of the beam.")
                .defineInRange("beamLength", 6, 1, 16);
        EMITTER_COOLDOWN = builder.comment("Cooldown (ticks) of the Gamma Emitter.")
                .defineInRange("emitterCooldown", 200, 0, 72000);
        HEALS_HULK = builder.comment("Whether the beam heals a Hulk in it.")
                .define("healsHulk", true);
        HEAL_PER_SECOND = builder.comment("Health per second the beam gives a Hulk.")
                .defineInRange("healPerSecond", 2.0, 0.0, 20.0);
        builder.pop();
    }

    private HulkConfig() {
    }
}
