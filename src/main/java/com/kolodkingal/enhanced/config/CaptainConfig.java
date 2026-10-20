package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Captain America and the Vibranium Shield (3.8, lane L1). Every option of the plan is defined here by the 3.8
 * skeleton (the lanes read them); {@code EnhancedConfig} calls {@link #define} while it builds the server config. They
 * are read from the server and read when used (never cached), so an edit applies at once. TOML sections {@code
 * [captain_america]} and {@code [vibranium_shield]}.
 */
public final class CaptainConfig {
    /** The TOML sections of these options. */
    public static final String SECTION_CAPTAIN_AMERICA = "captain_america";
    public static final String SECTION_VIBRANIUM_SHIELD = "vibranium_shield";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.DoubleValue HEALTH_BONUS;
    public static ModConfigSpec.DoubleValue SPEED_BONUS;
    public static ModConfigSpec.DoubleValue ATTACK_BONUS;
    public static ModConfigSpec.DoubleValue RESISTANCE;
    public static ModConfigSpec.DoubleValue JUMP_BONUS;
    public static ModConfigSpec.DoubleValue SAFE_FALL;
    public static ModConfigSpec.DoubleValue ATTACK_SPEED_BONUS;
    public static ModConfigSpec.DoubleValue ATTACK_KNOCKBACK;
    public static ModConfigSpec.BooleanValue WORTHY;
    public static ModConfigSpec.DoubleValue THROW_DAMAGE;
    public static ModConfigSpec.DoubleValue THROW_SPEED;
    public static ModConfigSpec.IntValue THROW_RANGE;
    public static ModConfigSpec.IntValue THROW_COOLDOWN;
    public static ModConfigSpec.IntValue RICOCHETS;
    public static ModConfigSpec.DoubleValue RICOCHET_RADIUS;
    public static ModConfigSpec.DoubleValue RICOCHET_FALLOFF;
    public static ModConfigSpec.IntValue RICOCHET_COOLDOWN;
    public static ModConfigSpec.DoubleValue SLAM_DAMAGE;
    public static ModConfigSpec.DoubleValue SLAM_RADIUS;
    public static ModConfigSpec.IntValue SLAM_COOLDOWN;
    public static ModConfigSpec.IntValue LEAP_COOLDOWN;
    public static ModConfigSpec.DoubleValue CHARGE_DISTANCE;
    public static ModConfigSpec.DoubleValue CHARGE_DAMAGE;
    public static ModConfigSpec.IntValue CHARGE_COOLDOWN;
    public static ModConfigSpec.DoubleValue BASH_DAMAGE;
    public static ModConfigSpec.IntValue BASH_STUN_TICKS;
    public static ModConfigSpec.IntValue BASH_COOLDOWN;
    public static ModConfigSpec.DoubleValue RALLY_RADIUS;
    public static ModConfigSpec.IntValue RALLY_TICKS;
    public static ModConfigSpec.IntValue RALLY_COOLDOWN;
    public static ModConfigSpec.DoubleValue INSPIRE_HEAL;
    public static ModConfigSpec.IntValue INSPIRE_COOLDOWN;
    public static ModConfigSpec.DoubleValue COMBO_DAMAGE_1;
    public static ModConfigSpec.DoubleValue COMBO_DAMAGE_2;
    public static ModConfigSpec.DoubleValue COMBO_DAMAGE_3;
    public static ModConfigSpec.IntValue COMBO_GAP;
    public static ModConfigSpec.IntValue COMBO_RESET;
    public static ModConfigSpec.DoubleValue FLIP_DAMAGE;
    public static ModConfigSpec.IntValue FLIP_COOLDOWN;
    public static ModConfigSpec.BooleanValue CAP_BLOCKS_EVERYTHING;
    public static ModConfigSpec.DoubleValue CAP_BLOCK_ANGLE;
    public static ModConfigSpec.BooleanValue REFLECT_PROJECTILES;
    public static ModConfigSpec.DoubleValue REFLECT_SPEED;
    public static ModConfigSpec.BooleanValue AXE_DISABLES;
    public static ModConfigSpec.DoubleValue MELEE_PUSH;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("captain_america");
        HEALTH_BONUS = builder.comment("Max health added to his 20.")
                .defineInRange("healthBonus", 6.0, 0.0, 100.0);
        SPEED_BONUS = builder.comment("Movement speed added, as a fraction (0.4 = 40 %: Speed II).")
                .defineInRange("speedBonus", 0.4, 0.0, 2.0);
        ATTACK_BONUS = builder.comment("Attack damage added to every blow (Strength II).")
                .defineInRange("attackBonus", 6.0, 0.0, 40.0);
        RESISTANCE = builder.comment("Fraction of incoming damage he shrugs off (Resistance I), unless it bypasses",
                "effects.")
                .defineInRange("resistance", 0.2, 0.0, 0.9);
        JUMP_BONUS = builder.comment("Jump strength added.")
                .defineInRange("jumpBonus", 0.15, 0.0, 2.0);
        SAFE_FALL = builder.comment("Safe fall distance added (blocks).")
                .defineInRange("safeFall", 4.0, 0.0, 64.0);
        ATTACK_SPEED_BONUS = builder.comment("Attack speed added.")
                .defineInRange("attackSpeedBonus", 0.5, 0.0, 4.0);
        ATTACK_KNOCKBACK = builder.comment("Attack knockback added.")
                .defineInRange("attackKnockback", 0.5, 0.0, 4.0);
        WORTHY = builder.comment("Whether Captain America is worthy of Mjolnir (Endgame).")
                .define("worthy", true);
        THROW_DAMAGE = builder.comment("Damage of the Shield Throw.")
                .defineInRange("throwDamage", 8.0, 0.0, 200.0);
        THROW_SPEED = builder.comment("Speed (blocks per tick) of the thrown shield.")
                .defineInRange("throwSpeed", 1.8, 0.2, 6.0);
        THROW_RANGE = builder.comment("Range (blocks) of the throw before the shield turns home.")
                .defineInRange("throwRange", 24, 4, 96);
        THROW_COOLDOWN = builder.comment("Cooldown (ticks) of the Shield Throw, after the catch.")
                .defineInRange("throwCooldown", 20, 0, 72000);
        RICOCHETS = builder.comment("Bounces of the Ricochet Throw.")
                .defineInRange("ricochets", 3, 0, 16);
        RICOCHET_RADIUS = builder.comment("Radius (blocks) in which the Ricochet Throw finds its next target.")
                .defineInRange("ricochetRadius", 8.0, 1.0, 32.0);
        RICOCHET_FALLOFF = builder.comment("Damage lost on each bounce of the ricochet.")
                .defineInRange("ricochetFalloff", 1.0, 0.0, 10.0);
        RICOCHET_COOLDOWN = builder.comment("Cooldown (ticks) of the Ricochet Throw, after the catch.")
                .defineInRange("ricochetCooldown", 60, 0, 72000);
        SLAM_DAMAGE = builder.comment("Damage of the Shield Slam.")
                .defineInRange("slamDamage", 6.0, 0.0, 200.0);
        SLAM_RADIUS = builder.comment("Radius (blocks) of the Shield Slam.")
                .defineInRange("slamRadius", 4.0, 1.0, 16.0);
        SLAM_COOLDOWN = builder.comment("Cooldown (ticks) of the Shield Slam.")
                .defineInRange("slamCooldown", 100, 0, 72000);
        LEAP_COOLDOWN = builder.comment("Cooldown (ticks) of the Super-Soldier Leap.")
                .defineInRange("leapCooldown", 40, 0, 72000);
        CHARGE_DISTANCE = builder.comment("Distance (blocks) of the Shield Charge.")
                .defineInRange("chargeDistance", 8.0, 1.0, 32.0);
        CHARGE_DAMAGE = builder.comment("Damage of the Shield Charge.")
                .defineInRange("chargeDamage", 6.0, 0.0, 200.0);
        CHARGE_COOLDOWN = builder.comment("Cooldown (ticks) of the Shield Charge.")
                .defineInRange("chargeCooldown", 100, 0, 72000);
        BASH_DAMAGE = builder.comment("Damage of the Shield Bash.")
                .defineInRange("bashDamage", 5.0, 0.0, 200.0);
        BASH_STUN_TICKS = builder.comment("Ticks the Shield Bash stuns.")
                .defineInRange("bashStunTicks", 30, 0, 400);
        BASH_COOLDOWN = builder.comment("Cooldown (ticks) of the Shield Bash.")
                .defineInRange("bashCooldown", 60, 0, 72000);
        RALLY_RADIUS = builder.comment("Radius (blocks) of Assemble and Inspire.")
                .defineInRange("rallyRadius", 16.0, 1.0, 64.0);
        RALLY_TICKS = builder.comment("Ticks of Strength I and Speed I that Assemble gives.")
                .defineInRange("rallyTicks", 400, 20, 12000);
        RALLY_COOLDOWN = builder.comment("Cooldown (ticks) of Assemble.")
                .defineInRange("rallyCooldown", 1200, 0, 72000);
        INSPIRE_HEAL = builder.comment("Health Inspire heals.")
                .defineInRange("inspireHeal", 6.0, 0.0, 40.0);
        INSPIRE_COOLDOWN = builder.comment("Cooldown (ticks) of Inspire.")
                .defineInRange("inspireCooldown", 600, 0, 72000);
        COMBO_DAMAGE_1 = builder.comment("Damage of the first blow of the Combat Combo.")
                .defineInRange("comboDamage1", 4.0, 0.0, 200.0);
        COMBO_DAMAGE_2 = builder.comment("Damage of the second blow.")
                .defineInRange("comboDamage2", 4.0, 0.0, 200.0);
        COMBO_DAMAGE_3 = builder.comment("Damage of the third blow (the spinning kick).")
                .defineInRange("comboDamage3", 7.0, 0.0, 200.0);
        COMBO_GAP = builder.comment("Ticks between two blows of the combo.")
                .defineInRange("comboGap", 8, 1, 100);
        COMBO_RESET = builder.comment("Ticks after which the combo starts again from the first blow.")
                .defineInRange("comboReset", 30, 5, 200);
        FLIP_DAMAGE = builder.comment("Damage of the Flip Kick.")
                .defineInRange("flipDamage", 5.0, 0.0, 200.0);
        FLIP_COOLDOWN = builder.comment("Cooldown (ticks) of the Flip Kick.")
                .defineInRange("flipCooldown", 40, 0, 72000);
        builder.pop();
        builder.push("vibranium_shield");
        CAP_BLOCKS_EVERYTHING = builder.comment("Whether the shield blocks all damage from the front for Captain",
                "America (explosions and beams too).")
                .define("capBlocksEverything", true);
        CAP_BLOCK_ANGLE = builder.comment("The angle (degrees) in front of him that the shield covers for Captain",
                "America.")
                .defineInRange("capBlockAngle", 180.0, 10.0, 360.0);
        REFLECT_PROJECTILES = builder.comment("Whether projectiles from the front bounce back toward their shooter",
                "for Captain America.")
                .define("reflectProjectiles", true);
        REFLECT_SPEED = builder.comment("Fraction of its speed a reflected projectile keeps.")
                .defineInRange("reflectSpeed", 0.8, 0.0, 2.0);
        AXE_DISABLES = builder.comment("Whether an axe can disable the shield of Captain America (vanilla's 5",
                "seconds).")
                .define("axeDisables", false);
        MELEE_PUSH = builder.comment("How far a melee attacker in front is pushed back by a block.")
                .defineInRange("meleePush", 0.6, 0.0, 4.0);
        builder.pop();
    }

    private CaptainConfig() {
    }
}
