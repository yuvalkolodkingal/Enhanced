package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of Thanos, the hero (the five stones the boss held, made playable). Its options are defined in
 * {@link #define}, which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code [thanos]}).
 * The boss's own numbers are in {@link ThanosBossConfig}. Ticks are 1/20 s; exhaustion 4.0 is one food point.
 */
public final class ThanosConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "thanos";

    // ---- the body ----
    public static ModConfigSpec.DoubleValue HEALTH_BONUS;
    public static ModConfigSpec.DoubleValue ARMOR_BONUS;
    public static ModConfigSpec.DoubleValue TOUGHNESS_BONUS;
    public static ModConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.DoubleValue ATTACK_BONUS;
    public static ModConfigSpec.DoubleValue SCALE;
    public static ModConfigSpec.DoubleValue REACH_BONUS;
    public static ModConfigSpec.DoubleValue STEP_BONUS;
    public static ModConfigSpec.DoubleValue REGEN_PER_SECOND;
    public static ModConfigSpec.IntValue REGEN_DELAY_TICKS;

    // ---- the Power Stone ----
    public static ModConfigSpec.DoubleValue POWER_BLAST_DAMAGE;
    public static ModConfigSpec.DoubleValue POWER_BLAST_SPLASH;
    public static ModConfigSpec.DoubleValue POWER_BLAST_RANGE;
    public static ModConfigSpec.IntValue POWER_BLAST_COOLDOWN;
    public static ModConfigSpec.DoubleValue POWER_BLAST_EXHAUSTION;
    public static ModConfigSpec.DoubleValue SLAM_DAMAGE;
    public static ModConfigSpec.DoubleValue SLAM_RADIUS;
    public static ModConfigSpec.IntValue SLAM_COOLDOWN;
    public static ModConfigSpec.DoubleValue SLAM_EXHAUSTION;

    // ---- the Space Stone ----
    public static ModConfigSpec.DoubleValue SPACE_STEP_RANGE;
    public static ModConfigSpec.IntValue SPACE_STEP_COOLDOWN;
    public static ModConfigSpec.DoubleValue STEP_EXHAUSTION;
    public static ModConfigSpec.DoubleValue PULL_RANGE;
    public static ModConfigSpec.IntValue PULL_COOLDOWN;
    public static ModConfigSpec.DoubleValue PULL_EXHAUSTION;

    // ---- the Reality Stone ----
    public static ModConfigSpec.DoubleValue TRANSMUTE_RANGE;
    public static ModConfigSpec.IntValue TRANSMUTE_TICKS;
    public static ModConfigSpec.IntValue TRANSMUTE_COOLDOWN;
    public static ModConfigSpec.DoubleValue TRANSMUTE_EXHAUSTION;
    public static ModConfigSpec.DoubleValue PETRIFY_DAMAGE_FACTOR;
    public static ModConfigSpec.DoubleValue VEIL_RADIUS;
    public static ModConfigSpec.DoubleValue VEIL_EXHAUSTION;

    // ---- the Soul Stone ----
    public static ModConfigSpec.DoubleValue DRAIN_RANGE;
    public static ModConfigSpec.DoubleValue DRAIN_DAMAGE_PER_SECOND;
    public static ModConfigSpec.DoubleValue DRAIN_HEAL_FRACTION;
    public static ModConfigSpec.DoubleValue DRAIN_EXHAUSTION;
    public static ModConfigSpec.IntValue DRAIN_COOLDOWN;
    public static ModConfigSpec.DoubleValue SIGHT_RANGE;
    public static ModConfigSpec.IntValue SIGHT_TICKS;
    public static ModConfigSpec.IntValue SIGHT_COOLDOWN;
    public static ModConfigSpec.BooleanValue SIGHT_BREAKS_ILLUSIONS;
    public static ModConfigSpec.DoubleValue SIGHT_EXHAUSTION;

    // ---- the Time Stone ----
    public static ModConfigSpec.IntValue REWIND_SECONDS;
    public static ModConfigSpec.DoubleValue REWIND_MAX_HEAL;
    public static ModConfigSpec.IntValue REWIND_COOLDOWN;
    public static ModConfigSpec.DoubleValue REWIND_EXHAUSTION;
    public static ModConfigSpec.DoubleValue TIME_LOCK_RADIUS;
    public static ModConfigSpec.IntValue TIME_LOCK_TICKS;
    public static ModConfigSpec.IntValue TIME_LOCK_COOLDOWN;
    public static ModConfigSpec.DoubleValue TIME_LOCK_PROJECTILE_DRAG;
    public static ModConfigSpec.DoubleValue TIME_LOCK_EXHAUSTION;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);

        HEALTH_BONUS = builder.comment("Extra health Thanos has (20 = 10 hearts).").defineInRange("healthBonus", 20.0, 0.0, 100.0);
        ARMOR_BONUS = builder.comment("Extra armour points.").defineInRange("armorBonus", 6.0, 0.0, 30.0);
        TOUGHNESS_BONUS = builder.comment("Extra armour toughness.").defineInRange("toughnessBonus", 2.0, 0.0, 20.0);
        KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance (1 = none at all).").defineInRange("knockbackResistance", 0.5, 0.0, 1.0);
        ATTACK_BONUS = builder.comment("Extra attack damage, with a bare fist or any weapon (Titan strength).").defineInRange("attackBonus", 3.0, 0.0, 20.0);
        SCALE = builder.comment("Extra size (0.1 = 10 percent taller and wider).").defineInRange("scale", 0.1, 0.0, 0.5);
        REACH_BONUS = builder.comment("Extra reach, in blocks (long arms).").defineInRange("reachBonus", 1.0, 0.0, 4.0);
        STEP_BONUS = builder.comment("Extra step height: a Titan's stride walks up whole blocks.").defineInRange("stepBonus", 0.4, 0.0, 1.0);
        REGEN_PER_SECOND = builder.comment("The Titan healing factor: health regained each second while unhurt.").defineInRange("regenPerSecond", 0.5, 0.0, 10.0);
        REGEN_DELAY_TICKS = builder.comment("Ticks without being hurt before the healing factor starts.").defineInRange("regenDelayTicks", 100, 0, 1200);

        POWER_BLAST_DAMAGE = builder.comment("Damage of the Power Blast.").defineInRange("powerBlastDamage", 12.0, 0.0, 100.0);
        POWER_BLAST_SPLASH = builder.comment("Damage of the explosion at the end of the Power Blast.").defineInRange("powerBlastSplash", 6.0, 0.0, 100.0);
        POWER_BLAST_RANGE = builder.comment("Range of the Power Blast, in blocks.").defineInRange("powerBlastRange", 28.0, 4.0, 64.0);
        POWER_BLAST_COOLDOWN = builder.comment("Ticks before the Power Blast can be used again.").defineInRange("powerBlastCooldown", 70, 0, 6000);
        POWER_BLAST_EXHAUSTION = builder.comment("Exhaustion of the Power Blast (4.0 = one food point).").defineInRange("powerBlastExhaustion", 2.0, 0.0, 20.0);
        SLAM_DAMAGE = builder.comment("Damage of the Titan Slam.").defineInRange("slamDamage", 10.0, 0.0, 100.0);
        SLAM_RADIUS = builder.comment("Radius of the Titan Slam's ring.").defineInRange("slamRadius", 7.0, 2.0, 24.0);
        SLAM_COOLDOWN = builder.comment("Ticks before the Titan Slam can be used again.").defineInRange("slamCooldown", 160, 0, 6000);
        SLAM_EXHAUSTION = builder.comment("Exhaustion of the Titan Slam.").defineInRange("slamExhaustion", 4.0, 0.0, 20.0);

        SPACE_STEP_RANGE = builder.comment("Range of the Portal Step, in blocks.").defineInRange("spaceStepRange", 40.0, 4.0, 128.0);
        SPACE_STEP_COOLDOWN = builder.comment("Ticks before the Portal Step can be used again.").defineInRange("spaceStepCooldown", 100, 0, 6000);
        STEP_EXHAUSTION = builder.comment("Exhaustion of the Portal Step.").defineInRange("stepExhaustion", 1.0, 0.0, 20.0);
        PULL_RANGE = builder.comment("Range of the Space Pull, in blocks.").defineInRange("pullRange", 24.0, 4.0, 64.0);
        PULL_COOLDOWN = builder.comment("Ticks before the Space Pull can be used again.").defineInRange("pullCooldown", 220, 0, 6000);
        PULL_EXHAUSTION = builder.comment("Exhaustion of the Space Pull.").defineInRange("pullExhaustion", 2.0, 0.0, 20.0);

        TRANSMUTE_RANGE = builder.comment("Range of the Transmute, in blocks.").defineInRange("transmuteRange", 20.0, 4.0, 64.0);
        TRANSMUTE_TICKS = builder.comment("How long a creature stays stone.").defineInRange("transmuteTicks", 60, 10, 600);
        TRANSMUTE_COOLDOWN = builder.comment("Ticks before the Transmute can be used again.").defineInRange("transmuteCooldown", 240, 0, 6000);
        TRANSMUTE_EXHAUSTION = builder.comment("Exhaustion of the Transmute.").defineInRange("transmuteExhaustion", 2.0, 0.0, 20.0);
        PETRIFY_DAMAGE_FACTOR = builder.comment("The damage a petrified creature takes, times (it is stone). The boss's Petrify Orb does this too.")
                .defineInRange("petrifyDamageFactor", 0.5, 0.1, 1.0);
        VEIL_RADIUS = builder.comment("How close a projectile must come to be turned into a bubble by the Reality Veil.").defineInRange("veilRadius", 8.0, 2.0, 32.0);
        VEIL_EXHAUSTION = builder.comment("Exhaustion of the Reality Veil each tick it is on.").defineInRange("veilExhaustion", 0.01, 0.0, 1.0);

        DRAIN_RANGE = builder.comment("Range of the Soul Drain, in blocks.").defineInRange("drainRange", 16.0, 4.0, 64.0);
        DRAIN_DAMAGE_PER_SECOND = builder.comment("Damage of the Soul Drain each second (it ignores armour and enchantments).")
                .defineInRange("drainDamagePerSecond", 6.0, 0.0, 100.0);
        DRAIN_HEAL_FRACTION = builder.comment("The share of the Soul Drain's damage that heals Thanos.").defineInRange("drainHealFraction", 0.5, 0.0, 2.0);
        DRAIN_EXHAUSTION = builder.comment("Exhaustion of the Soul Drain each tick.").defineInRange("drainExhaustion", 0.02, 0.0, 1.0);
        DRAIN_COOLDOWN = builder.comment("Ticks before the Soul Drain can be used again after it ends.").defineInRange("drainCooldown", 40, 0, 6000);
        SIGHT_RANGE = builder.comment("Range of the Soul Sight, in blocks.").defineInRange("sightRange", 32.0, 4.0, 64.0);
        SIGHT_TICKS = builder.comment("How long the Soul Sight lasts.").defineInRange("sightTicks", 160, 20, 1200);
        SIGHT_COOLDOWN = builder.comment("Ticks before the Soul Sight can be used again.").defineInRange("sightCooldown", 400, 0, 6000);
        SIGHT_BREAKS_ILLUSIONS = builder.comment("Whether the Soul Sight dissolves Loki's duplicates and ends his disguises.").define("sightBreaksIllusions", true);
        SIGHT_EXHAUSTION = builder.comment("Exhaustion of the Soul Sight.").defineInRange("sightExhaustion", 1.0, 0.0, 20.0);

        REWIND_SECONDS = builder.comment("How far back the Rewind goes, in seconds.").defineInRange("rewindSeconds", 5, 1, 15);
        REWIND_MAX_HEAL = builder.comment("The most health the Rewind returns.").defineInRange("rewindMaxHeal", 20.0, 0.0, 100.0);
        REWIND_COOLDOWN = builder.comment("Ticks before the Rewind can be used again.").defineInRange("rewindCooldown", 900, 0, 72000);
        REWIND_EXHAUSTION = builder.comment("Exhaustion of the Rewind.").defineInRange("rewindExhaustion", 8.0, 0.0, 20.0);
        TIME_LOCK_RADIUS = builder.comment("Radius of the Time Lock's dome.").defineInRange("timeLockRadius", 6.0, 2.0, 24.0);
        TIME_LOCK_TICKS = builder.comment("How long the Time Lock stands.").defineInRange("timeLockTicks", 100, 10, 600);
        TIME_LOCK_COOLDOWN = builder.comment("Ticks before the Time Lock can be used again.").defineInRange("timeLockCooldown", 600, 0, 72000);
        TIME_LOCK_PROJECTILE_DRAG = builder.comment("The share of its speed a projectile loses each tick inside the Time Lock.")
                .defineInRange("timeLockProjectileDrag", 0.65, 0.0, 1.0);
        TIME_LOCK_EXHAUSTION = builder.comment("Exhaustion of the Time Lock.").defineInRange("timeLockExhaustion", 3.0, 0.0, 20.0);

        builder.pop();
    }

    private ThanosConfig() {
    }
}
