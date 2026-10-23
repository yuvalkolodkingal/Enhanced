package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Wolverine and the Adamantium Injection (3.7). Every option of the plan is defined here by the 3.7 skeleton (lane L2
 * reads them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML sections {@code
 * [wolverine]} and {@code [adamantium_injection]}). They are read from the server and read when used (never cached).
 */
public final class WolverineConfig {
    /** The TOML sections of these options. */
    public static final String SECTION_WOLVERINE = "wolverine";
    public static final String SECTION_ADAMANTIUM_INJECTION = "adamantium_injection";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.DoubleValue HEALTH_BONUS;
    public static ConfigSpec.DoubleValue ARMOR_BONUS;
    public static ConfigSpec.DoubleValue TOUGHNESS_BONUS;
    public static ConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ConfigSpec.DoubleValue SPEED_BONUS;
    public static ConfigSpec.DoubleValue ATTACK_BONUS;
    public static ConfigSpec.DoubleValue ATTACK_SPEED_BONUS;
    public static ConfigSpec.DoubleValue JUMP_BONUS;
    public static ConfigSpec.DoubleValue SAFE_FALL;
    public static ConfigSpec.DoubleValue CLAW_BONUS;
    public static ConfigSpec.BooleanValue HEAVY;
    public static ConfigSpec.DoubleValue SINK_SPEED;
    public static ConfigSpec.DoubleValue MAX_SWIM_UP;
    public static ConfigSpec.DoubleValue SLASH_DAMAGE_1;
    public static ConfigSpec.DoubleValue SLASH_DAMAGE_2;
    public static ConfigSpec.DoubleValue SLASH_DAMAGE_3;
    public static ConfigSpec.DoubleValue SLASH_REACH;
    public static ConfigSpec.IntValue SLASH_GAP;
    public static ConfigSpec.IntValue COMBO_RESET;
    public static ConfigSpec.DoubleValue SPIN_DAMAGE;
    public static ConfigSpec.DoubleValue SPIN_RADIUS;
    public static ConfigSpec.IntValue SPIN_COOLDOWN;
    public static ConfigSpec.IntValue POUNCE_RANGE;
    public static ConfigSpec.DoubleValue POUNCE_DAMAGE;
    public static ConfigSpec.IntValue POUNCE_PIN_TICKS;
    public static ConfigSpec.IntValue POUNCE_COOLDOWN;
    public static ConfigSpec.IntValue FERAL_LEAP_COOLDOWN;
    public static ConfigSpec.IntValue BERSERK_TICKS;
    public static ConfigSpec.DoubleValue BERSERK_DAMAGE;
    public static ConfigSpec.DoubleValue BERSERK_SPEED;
    public static ConfigSpec.DoubleValue BERSERK_REGEN_FACTOR;
    public static ConfigSpec.IntValue BERSERK_TIRED_TICKS;
    public static ConfigSpec.IntValue BERSERK_COOLDOWN;
    public static ConfigSpec.IntValue SNIFF_RADIUS;
    public static ConfigSpec.IntValue SNIFF_TICKS;
    public static ConfigSpec.IntValue SNIFF_COOLDOWN;
    public static ConfigSpec.IntValue TRAIL_SECONDS;
    public static ConfigSpec.IntValue TRACK_RANGE;
    public static ConfigSpec.IntValue TRACK_TICKS;
    public static ConfigSpec.IntValue TRACK_COOLDOWN;
    public static ConfigSpec.BooleanValue TRACK_TELLS_TARGET;
    public static ConfigSpec.DoubleValue CLIMB_SPEED;
    public static ConfigSpec.IntValue INJECTION_HOLD_TICKS;
    public static ConfigSpec.BooleanValue INJECTION_HEROES_REFUSED;
    public static ConfigSpec.BooleanValue INJECTION_CRAFTABLE;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("wolverine");
        HEALTH_BONUS = builder.comment("Max health added to his 20.")
                .defineInRange("healthBonus", 10.0, 0.0, 100.0);
        ARMOR_BONUS = builder.comment("Armour of his adamantium bones.")
                .defineInRange("armorBonus", 4.0, 0.0, 30.0);
        TOUGHNESS_BONUS = builder.comment("Armour toughness of his adamantium bones.")
                .defineInRange("toughnessBonus", 2.0, 0.0, 20.0);
        KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance (he is heavy).")
                .defineInRange("knockbackResistance", 0.4, 0.0, 1.0);
        SPEED_BONUS = builder.comment("Movement speed added, as a fraction (0.2 = 20 %).")
                .defineInRange("speedBonus", 0.2, 0.0, 2.0);
        ATTACK_BONUS = builder.comment("Attack damage added to every blow.")
                .defineInRange("attackBonus", 2.0, 0.0, 40.0);
        ATTACK_SPEED_BONUS = builder.comment("Attack speed added.")
                .defineInRange("attackSpeedBonus", 0.6, 0.0, 4.0);
        JUMP_BONUS = builder.comment("Jump strength added.")
                .defineInRange("jumpBonus", 0.15, 0.0, 2.0);
        SAFE_FALL = builder.comment("Safe fall distance added (blocks).")
                .defineInRange("safeFall", 4.0, 0.0, 64.0);
        CLAW_BONUS = builder.comment("Damage the claws add to a blow of his empty main hand.")
                .defineInRange("clawBonus", 4.0, 0.0, 40.0);
        HEAVY = builder.comment("Whether his adamantium makes him sink in water and lava.")
                .define("heavy", true);
        SINK_SPEED = builder.comment("How fast (blocks per tick, added each tick) he sinks in a fluid.")
                .defineInRange("sinkSpeed", 0.03, 0.0, 0.5);
        MAX_SWIM_UP = builder.comment("The fastest (blocks per tick) he can swim up.")
                .defineInRange("maxSwimUp", 0.02, 0.0, 1.0);
        SLASH_DAMAGE_1 = builder.comment("Damage of the first hit of the Claw Slash combo.")
                .defineInRange("slashDamage1", 6.0, 0.0, 100.0);
        SLASH_DAMAGE_2 = builder.comment("Damage of the second hit.")
                .defineInRange("slashDamage2", 6.0, 0.0, 100.0);
        SLASH_DAMAGE_3 = builder.comment("Damage of the third hit (the cross-slash).")
                .defineInRange("slashDamage3", 9.0, 0.0, 100.0);
        SLASH_REACH = builder.comment("Reach (blocks) of the Claw Slash.")
                .defineInRange("slashReach", 3.5, 1.0, 8.0);
        SLASH_GAP = builder.comment("Ticks between two hits of the combo.")
                .defineInRange("slashGap", 10, 1, 100);
        COMBO_RESET = builder.comment("Ticks after which the combo starts again from the first hit.")
                .defineInRange("comboReset", 30, 5, 200);
        SPIN_DAMAGE = builder.comment("Damage of the Spin Slash.")
                .defineInRange("spinDamage", 7.0, 0.0, 100.0);
        SPIN_RADIUS = builder.comment("Radius (blocks) of the Spin Slash.")
                .defineInRange("spinRadius", 3.0, 1.0, 8.0);
        SPIN_COOLDOWN = builder.comment("Cooldown (ticks) of the Spin Slash.")
                .defineInRange("spinCooldown", 80, 0, 72000);
        POUNCE_RANGE = builder.comment("Range (blocks) of the Pounce.")
                .defineInRange("pounceRange", 16, 4, 64);
        POUNCE_DAMAGE = builder.comment("Damage of the Pounce's stab.")
                .defineInRange("pounceDamage", 12.0, 0.0, 100.0);
        POUNCE_PIN_TICKS = builder.comment("Ticks the prey is pinned after the Pounce.")
                .defineInRange("pouncePinTicks", 20, 0, 200);
        POUNCE_COOLDOWN = builder.comment("Cooldown (ticks) of the Pounce.")
                .defineInRange("pounceCooldown", 100, 0, 72000);
        FERAL_LEAP_COOLDOWN = builder.comment("Cooldown (ticks) of the Feral Leap.")
                .defineInRange("feralLeapCooldown", 30, 0, 72000);
        BERSERK_TICKS = builder.comment("Ticks the Berserker Rage lasts.")
                .defineInRange("berserkTicks", 200, 20, 1200);
        BERSERK_DAMAGE = builder.comment("Damage added by the Rage, as a fraction (0.4 = 40 %).")
                .defineInRange("berserkDamage", 0.4, 0.0, 4.0);
        BERSERK_SPEED = builder.comment("Speed added by the Rage, as a fraction.")
                .defineInRange("berserkSpeed", 0.25, 0.0, 2.0);
        BERSERK_REGEN_FACTOR = builder.comment("How many times faster he heals in the Rage.")
                .defineInRange("berserkRegenFactor", 2.0, 1.0, 10.0);
        BERSERK_TIRED_TICKS = builder.comment("Ticks of Weakness I after the Rage.")
                .defineInRange("berserkTiredTicks", 100, 0, 1200);
        BERSERK_COOLDOWN = builder.comment("Cooldown (ticks) of the Berserker Rage.")
                .defineInRange("berserkCooldown", 900, 0, 72000);
        SNIFF_RADIUS = builder.comment("Radius (blocks) of the Sniff.")
                .defineInRange("sniffRadius", 48, 8, 128);
        SNIFF_TICKS = builder.comment("Ticks the creatures he sniffed glow for him.")
                .defineInRange("sniffTicks", 200, 20, 1200);
        SNIFF_COOLDOWN = builder.comment("Cooldown (ticks) of the Sniff.")
                .defineInRange("sniffCooldown", 300, 0, 72000);
        TRAIL_SECONDS = builder.comment("Seconds of scent trail kept for each player and monster.")
                .defineInRange("trailSeconds", 60, 5, 300);
        TRACK_RANGE = builder.comment("The farthest (blocks) Track follows a scent.")
                .defineInRange("trackRange", 256, 16, 4096);
        TRACK_TICKS = builder.comment("Ticks Track lasts.")
                .defineInRange("trackTicks", 1200, 100, 12000);
        TRACK_COOLDOWN = builder.comment("Cooldown (ticks) of Track, after it ends.")
                .defineInRange("trackCooldown", 300, 0, 72000);
        TRACK_TELLS_TARGET = builder.comment("Whether a tracked player is told that Wolverine has his scent.")
                .define("trackTellsTarget", true);
        CLIMB_SPEED = builder.comment("How fast (blocks per tick) Claw Climb takes him up a wall.")
                .defineInRange("climbSpeed", 0.12, 0.01, 1.0);
        builder.pop();
        builder.push("adamantium_injection");
        INJECTION_HOLD_TICKS = builder.comment("Ticks the Adamantium Injection is held to use it.")
                .defineInRange("holdTicks", 60, 10, 400);
        INJECTION_HEROES_REFUSED = builder.comment("Whether a hero is refused the injection (false: it replaces his",
                "power with Wolverine's).")
                .define("heroesRefused", true);
        INJECTION_CRAFTABLE = builder.comment("Whether the Adamantium Injection can be crafted (for servers without",
                "labs).")
                .define("craftable", false);
        builder.pop();
    }

    private WolverineConfig() {
    }
}
