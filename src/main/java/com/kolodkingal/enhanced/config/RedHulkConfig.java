package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Red Hulk (3.8, lane L2): his body, his Anger and its burst, his keys and Ross's form. Every option of the plan is
 * defined here by the 3.8 skeleton (the lanes read them); {@code EnhancedConfig} calls {@link #define} while it builds
 * the server config. They are read from the server and read when used (never cached), so an edit applies at once. TOML
 * section {@code [red_hulk]}.
 */
public final class RedHulkConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "red_hulk";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.DoubleValue SCALE_BONUS;
    public static ModConfigSpec.DoubleValue HEALTH_BONUS;
    public static ModConfigSpec.DoubleValue ATTACK_BONUS;
    public static ModConfigSpec.DoubleValue SPEED_PENALTY;
    public static ModConfigSpec.DoubleValue ARMOR_BONUS;
    public static ModConfigSpec.DoubleValue TOUGHNESS_BONUS;
    public static ModConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.DoubleValue REACH_BONUS;
    public static ModConfigSpec.DoubleValue STEP_BONUS;
    public static ModConfigSpec.DoubleValue JUMP_BONUS;
    public static ModConfigSpec.DoubleValue SAFE_FALL;
    public static ModConfigSpec.BooleanValue FIRE_IMMUNE;
    public static ModConfigSpec.IntValue TRANSFORM_TICKS;
    public static ModConfigSpec.DoubleValue ANGER_PER_DAMAGE_TAKEN;
    public static ModConfigSpec.DoubleValue ANGER_PER_DAMAGE_DEALT;
    public static ModConfigSpec.DoubleValue ANGER_ON_ROAR;
    public static ModConfigSpec.IntValue COOL_DELAY_TICKS;
    public static ModConfigSpec.DoubleValue COOL_PER_SECOND;
    public static ModConfigSpec.DoubleValue ROSS_ANGER_FACTOR;
    public static ModConfigSpec.DoubleValue BURST_RADIUS;
    public static ModConfigSpec.DoubleValue BURST_DAMAGE;
    public static ModConfigSpec.IntValue BURST_FIRE_SECONDS;
    public static ModConfigSpec.DoubleValue BURST_PUSH;
    public static ModConfigSpec.BooleanValue BURST_SETS_FIRE;
    public static ModConfigSpec.DoubleValue EARLY_BURST_MIN;
    public static ModConfigSpec.IntValue EARLY_BURST_COOLDOWN;
    public static ModConfigSpec.DoubleValue SMASH_DAMAGE;
    public static ModConfigSpec.DoubleValue SMASH_RADIUS;
    public static ModConfigSpec.DoubleValue SMASH_LAUNCH;
    public static ModConfigSpec.IntValue SMASH_COOLDOWN;
    public static ModConfigSpec.BooleanValue SMASH_BREAKS_BLOCKS;
    public static ModConfigSpec.DoubleValue GRAB_DAMAGE_1;
    public static ModConfigSpec.DoubleValue GRAB_DAMAGE_2;
    public static ModConfigSpec.DoubleValue GRAB_DAMAGE_3;
    public static ModConfigSpec.IntValue GRAB_COOLDOWN;
    public static ModConfigSpec.DoubleValue HEAT_LENGTH;
    public static ModConfigSpec.DoubleValue HEAT_ANGLE;
    public static ModConfigSpec.DoubleValue HEAT_DAMAGE;
    public static ModConfigSpec.IntValue HEAT_FIRE_SECONDS;
    public static ModConfigSpec.BooleanValue HEAT_MELTS_SNOW;
    public static ModConfigSpec.IntValue HEAT_COOLDOWN;
    public static ModConfigSpec.DoubleValue ROAR_RADIUS;
    public static ModConfigSpec.IntValue ROAR_FRIGHT_TICKS;
    public static ModConfigSpec.IntValue ROAR_WEAKNESS_TICKS;
    public static ModConfigSpec.IntValue ROAR_COOLDOWN;
    public static ModConfigSpec.IntValue JUMP_CHARGE_TICKS;
    public static ModConfigSpec.DoubleValue JUMP_MIN_HEIGHT;
    public static ModConfigSpec.DoubleValue JUMP_MAX_HEIGHT;
    public static ModConfigSpec.DoubleValue JUMP_LAND_DAMAGE;
    public static ModConfigSpec.DoubleValue JUMP_LAND_RADIUS;
    public static ModConfigSpec.IntValue JUMP_COOLDOWN;
    public static ModConfigSpec.DoubleValue BOUND_DISTANCE;
    public static ModConfigSpec.IntValue BOUND_COOLDOWN;
    public static ModConfigSpec.IntValue ABSORB_MAX_TICKS;
    public static ModConfigSpec.DoubleValue ABSORB_ANGER_PER_HEALTH;
    public static ModConfigSpec.IntValue ABSORB_COOLDOWN;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("red_hulk");
        SCALE_BONUS = builder.comment("Scale added to his size (0.5: one and a half times).")
                .defineInRange("scaleBonus", 0.5, 0.0, 2.0);
        HEALTH_BONUS = builder.comment("Max health added to his 20.")
                .defineInRange("healthBonus", 24.0, 0.0, 200.0);
        ATTACK_BONUS = builder.comment("Attack damage added to every blow (Strength IV).")
                .defineInRange("attackBonus", 12.0, 0.0, 60.0);
        SPEED_PENALTY = builder.comment("Movement speed taken away, as a fraction (Slowness I).")
                .defineInRange("speedPenalty", 0.15, 0.0, 0.9);
        ARMOR_BONUS = builder.comment("Armour of his red hide.")
                .defineInRange("armorBonus", 6.0, 0.0, 30.0);
        TOUGHNESS_BONUS = builder.comment("Armour toughness of his red hide.")
                .defineInRange("toughnessBonus", 3.0, 0.0, 20.0);
        KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance.")
                .defineInRange("knockbackResistance", 0.8, 0.0, 1.0);
        REACH_BONUS = builder.comment("Reach (blocks) added.")
                .defineInRange("reachBonus", 1.5, 0.0, 8.0);
        STEP_BONUS = builder.comment("Step height added.")
                .defineInRange("stepBonus", 0.6, 0.0, 4.0);
        JUMP_BONUS = builder.comment("Jump strength added.")
                .defineInRange("jumpBonus", 0.4, 0.0, 4.0);
        SAFE_FALL = builder.comment("Safe fall distance added (blocks).")
                .defineInRange("safeFall", 20.0, 0.0, 256.0);
        FIRE_IMMUNE = builder.comment("Whether fire, lava and hot floors never hurt him.")
                .define("fireImmune", true);
        TRANSFORM_TICKS = builder.comment("Ticks the change between Ross and Red Hulk takes.")
                .defineInRange("transformTicks", 30, 1, 200);
        ANGER_PER_DAMAGE_TAKEN = builder.comment("Anger for each health he loses.")
                .defineInRange("angerPerDamageTaken", 3.0, 0.0, 100.0);
        ANGER_PER_DAMAGE_DEALT = builder.comment("Anger for each health he deals.")
                .defineInRange("angerPerDamageDealt", 1.0, 0.0, 100.0);
        ANGER_ON_ROAR = builder.comment("Anger the Roar adds.")
                .defineInRange("angerOnRoar", 10.0, 0.0, 100.0);
        COOL_DELAY_TICKS = builder.comment("Ticks without fighting before his anger cools.")
                .defineInRange("coolDelayTicks", 100, 0, 12000);
        COOL_PER_SECOND = builder.comment("Anger lost per second while he cools.")
                .defineInRange("coolPerSecond", 4.0, 0.0, 100.0);
        ROSS_ANGER_FACTOR = builder.comment("How fast Ross (his human form) gets angry, as a fraction of Red Hulk's.")
                .defineInRange("rossAngerFactor", 0.5, 0.0, 1.0);
        BURST_RADIUS = builder.comment("Radius (blocks) of the Anger Burst.")
                .defineInRange("burstRadius", 6.0, 1.0, 24.0);
        BURST_DAMAGE = builder.comment("Damage of the Anger Burst (at its centre).")
                .defineInRange("burstDamage", 10.0, 0.0, 200.0);
        BURST_FIRE_SECONDS = builder.comment("Seconds the Anger Burst sets creatures burning.")
                .defineInRange("burstFireSeconds", 5, 0, 60);
        BURST_PUSH = builder.comment("How far the Anger Burst pushes creatures.")
                .defineInRange("burstPush", 1.2, 0.0, 4.0);
        BURST_SETS_FIRE = builder.comment("Whether the Anger Burst sets blocks on fire (only with the mobGriefing",
                "game rule too).")
                .define("burstSetsFire", true);
        EARLY_BURST_MIN = builder.comment("The least anger for an Anger Burst on B.")
                .defineInRange("earlyBurstMin", 50.0, 0.0, 100.0);
        EARLY_BURST_COOLDOWN = builder.comment("Cooldown (ticks) of the Anger Burst on B.")
                .defineInRange("earlyBurstCooldown", 200, 0, 72000);
        SMASH_DAMAGE = builder.comment("Damage of the Ground Smash.")
                .defineInRange("smashDamage", 8.0, 0.0, 200.0);
        SMASH_RADIUS = builder.comment("Radius (blocks) of the Ground Smash.")
                .defineInRange("smashRadius", 6.0, 1.0, 24.0);
        SMASH_LAUNCH = builder.comment("How high the Ground Smash throws creatures.")
                .defineInRange("smashLaunch", 0.9, 0.0, 4.0);
        SMASH_COOLDOWN = builder.comment("Cooldown (ticks) of the Ground Smash.")
                .defineInRange("smashCooldown", 80, 0, 72000);
        SMASH_BREAKS_BLOCKS = builder.comment("Whether the Ground Smash breaks soft blocks (the Hulk's rules; only",
                "with mobGriefing too).")
                .define("smashBreaksBlocks", false);
        GRAB_DAMAGE_1 = builder.comment("Damage of the first slam of Grab & Slam.")
                .defineInRange("grabDamage1", 5.0, 0.0, 200.0);
        GRAB_DAMAGE_2 = builder.comment("Damage of the second slam.")
                .defineInRange("grabDamage2", 5.0, 0.0, 200.0);
        GRAB_DAMAGE_3 = builder.comment("Damage of the last slam and throw.")
                .defineInRange("grabDamage3", 7.0, 0.0, 200.0);
        GRAB_COOLDOWN = builder.comment("Cooldown (ticks) of Grab & Slam.")
                .defineInRange("grabCooldown", 160, 0, 72000);
        HEAT_LENGTH = builder.comment("Length (blocks) of the Heat Wave.")
                .defineInRange("heatLength", 10.0, 1.0, 32.0);
        HEAT_ANGLE = builder.comment("Angle (degrees) of the Heat Wave's cone.")
                .defineInRange("heatAngle", 50.0, 5.0, 180.0);
        HEAT_DAMAGE = builder.comment("Damage of the Heat Wave.")
                .defineInRange("heatDamage", 5.0, 0.0, 200.0);
        HEAT_FIRE_SECONDS = builder.comment("Seconds the Heat Wave sets creatures burning.")
                .defineInRange("heatFireSeconds", 4, 0, 60);
        HEAT_MELTS_SNOW = builder.comment("Whether the Heat Wave melts snow and ice (only with mobGriefing too).")
                .define("heatMeltsSnow", true);
        HEAT_COOLDOWN = builder.comment("Cooldown (ticks) of the Heat Wave.")
                .defineInRange("heatCooldown", 120, 0, 72000);
        ROAR_RADIUS = builder.comment("Radius (blocks) of the Roar.")
                .defineInRange("roarRadius", 12.0, 1.0, 64.0);
        ROAR_FRIGHT_TICKS = builder.comment("Ticks the Roar frightens monsters.")
                .defineInRange("roarFrightTicks", 80, 0, 1200);
        ROAR_WEAKNESS_TICKS = builder.comment("Ticks of Weakness I the Roar gives.")
                .defineInRange("roarWeaknessTicks", 160, 0, 1200);
        ROAR_COOLDOWN = builder.comment("Cooldown (ticks) of the Roar.")
                .defineInRange("roarCooldown", 300, 0, 72000);
        JUMP_CHARGE_TICKS = builder.comment("Ticks of holding V for a fully charged Super Jump.")
                .defineInRange("jumpChargeTicks", 30, 1, 200);
        JUMP_MIN_HEIGHT = builder.comment("Height (blocks) of an uncharged Super Jump.")
                .defineInRange("jumpMinHeight", 8.0, 1.0, 64.0);
        JUMP_MAX_HEIGHT = builder.comment("Height (blocks) of a fully charged Super Jump.")
                .defineInRange("jumpMaxHeight", 30.0, 1.0, 128.0);
        JUMP_LAND_DAMAGE = builder.comment("Damage of the Super Jump's landing.")
                .defineInRange("jumpLandDamage", 4.0, 0.0, 200.0);
        JUMP_LAND_RADIUS = builder.comment("Radius (blocks) of the Super Jump's landing.")
                .defineInRange("jumpLandRadius", 3.0, 0.0, 16.0);
        JUMP_COOLDOWN = builder.comment("Cooldown (ticks) of the Super Jump, after the landing.")
                .defineInRange("jumpCooldown", 60, 0, 72000);
        BOUND_DISTANCE = builder.comment("Distance (blocks) of the Bound.")
                .defineInRange("boundDistance", 20.0, 4.0, 64.0);
        BOUND_COOLDOWN = builder.comment("Cooldown (ticks) of the Bound.")
                .defineInRange("boundCooldown", 40, 0, 72000);
        ABSORB_MAX_TICKS = builder.comment("The longest (ticks) Energy Absorb can be held.")
                .defineInRange("absorbMaxTicks", 80, 1, 1200);
        ABSORB_ANGER_PER_HEALTH = builder.comment("Anger for each health of energy he absorbs.")
                .defineInRange("absorbAngerPerHealth", 2.0, 0.0, 100.0);
        ABSORB_COOLDOWN = builder.comment("Cooldown (ticks) of Energy Absorb.")
                .defineInRange("absorbCooldown", 200, 0, 72000);
        builder.pop();
    }

    private RedHulkConfig() {
    }
}
