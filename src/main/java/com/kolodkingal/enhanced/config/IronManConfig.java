package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Iron Man and his six suits (3.8, lanes L3 and L4): the Arc Reactors, suit energy, the suit-up, and each suit's
 * armour, flight and moves. Every option of the plan is defined here by the 3.8 skeleton (the lanes read them); {@code
 * EnhancedConfig} calls {@link #define} while it builds the server config. They are read from the server and read when
 * used (never cached), so an edit applies at once. TOML sections {@code [iron_man]}, {@code [mark_1]}, {@code
 * [mark_3]}, {@code [mark_5]}, {@code [mark_50]}, {@code [mark_85]} and {@code [hulkbuster]}; the suits' options are
 * prefixed with the suit ({@code MARK3_ARMOR} is {@code [mark_3] armor}).
 */
public final class IronManConfig {
    /** The TOML sections of these options. */
    public static final String SECTION_IRON_MAN = "iron_man";
    public static final String SECTION_MARK_1 = "mark_1";
    public static final String SECTION_MARK_3 = "mark_3";
    public static final String SECTION_MARK_5 = "mark_5";
    public static final String SECTION_MARK_50 = "mark_50";
    public static final String SECTION_MARK_85 = "mark_85";
    public static final String SECTION_HULKBUSTER = "hulkbuster";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.IntValue REACTOR_HOLD_TICKS;
    public static ModConfigSpec.BooleanValue REACTOR_REPLACES_HERO;
    public static ModConfigSpec.BooleanValue REACTORS_CRAFTABLE;
    public static ModConfigSpec.BooleanValue ONLY_IRON_MAN_WEARS;
    public static ModConfigSpec.DoubleValue ENERGY_MAX;
    public static ModConfigSpec.DoubleValue RECHARGE_GROUND;
    public static ModConfigSpec.DoubleValue RECHARGE_AIR;
    public static ModConfigSpec.IntValue RECHARGE_DELAY_TICKS;
    public static ModConfigSpec.IntValue EMPTY_SLOW_FALL_TICKS;
    public static ModConfigSpec.DoubleValue SUIT_UP_DAMAGE_FACTOR;
    public static ModConfigSpec.IntValue SUIT_PHASE_COOLDOWN;
    public static ModConfigSpec.IntValue SUIT_UP_TICKS_MARK1;
    public static ModConfigSpec.IntValue SUIT_UP_TICKS_MARK3;
    public static ModConfigSpec.IntValue SUIT_UP_TICKS_MARK5;
    public static ModConfigSpec.IntValue SUIT_UP_TICKS_MARK50;
    public static ModConfigSpec.IntValue SUIT_UP_TICKS_MARK85;
    public static ModConfigSpec.IntValue SUIT_UP_TICKS_HULKBUSTER;
    public static ModConfigSpec.DoubleValue SUIT_DOWN_FACTOR;
    public static ModConfigSpec.DoubleValue GANTRY_RANGE;
    public static ModConfigSpec.IntValue NANO_REBUILD_TICKS;
    public static ModConfigSpec.IntValue HARD_LANDING_DURABILITY;
    public static ModConfigSpec.BooleanValue MAGNETIC_CLASSIC_SUITS;
    public static ModConfigSpec.BooleanValue BOOMS_BREAK_BLOCKS;
    public static ModConfigSpec.DoubleValue MARK1_ARMOR;
    public static ModConfigSpec.DoubleValue MARK1_TOUGHNESS;
    public static ModConfigSpec.DoubleValue MARK1_KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.IntValue MARK1_DURABILITY;
    public static ModConfigSpec.DoubleValue MARK1_SPEED_PENALTY;
    public static ModConfigSpec.DoubleValue MARK1_FLAME_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK1_FLAME_LENGTH;
    public static ModConfigSpec.DoubleValue MARK1_FLAME_ANGLE;
    public static ModConfigSpec.IntValue MARK1_FLAME_FIRE_SECONDS;
    public static ModConfigSpec.DoubleValue MARK1_FLAME_ENERGY_PER_SECOND;
    public static ModConfigSpec.IntValue MARK1_FLAME_MAX_TICKS;
    public static ModConfigSpec.IntValue MARK1_FLAME_COOLDOWN;
    public static ModConfigSpec.BooleanValue MARK1_FLAME_SETS_BLOCKS;
    public static ModConfigSpec.IntValue MARK1_BOOST_UP_TICKS;
    public static ModConfigSpec.IntValue MARK1_BOOST_FORWARD_TICKS;
    public static ModConfigSpec.DoubleValue MARK1_BOOST_ENERGY;
    public static ModConfigSpec.IntValue MARK1_BOOST_COOLDOWN;
    public static ModConfigSpec.IntValue MARK1_CRASH_DURABILITY;
    public static ModConfigSpec.DoubleValue MARK1_CRASH_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK1_ROCKET_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK1_ROCKET_POWER;
    public static ModConfigSpec.DoubleValue MARK1_ROCKET_ENERGY;
    public static ModConfigSpec.IntValue MARK1_ROCKET_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK1_BRACE_REDUCTION;
    public static ModConfigSpec.DoubleValue MARK1_BRACE_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue MARK1_PUNCH_DAMAGE;
    public static ModConfigSpec.IntValue MARK1_PUNCH_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK3_ARMOR;
    public static ModConfigSpec.DoubleValue MARK3_TOUGHNESS;
    public static ModConfigSpec.DoubleValue MARK3_KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.IntValue MARK3_DURABILITY;
    public static ModConfigSpec.DoubleValue MARK3_FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue MARK3_FLIGHT_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue MARK3_REPULSOR_DAMAGE;
    public static ModConfigSpec.IntValue MARK3_REPULSOR_RANGE;
    public static ModConfigSpec.DoubleValue MARK3_REPULSOR_POWER;
    public static ModConfigSpec.DoubleValue MARK3_REPULSOR_ENERGY;
    public static ModConfigSpec.IntValue MARK3_REPULSOR_GAP;
    public static ModConfigSpec.DoubleValue MARK3_DOUBLE_BLAST_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK3_DOUBLE_BLAST_POWER;
    public static ModConfigSpec.DoubleValue MARK3_DOUBLE_BLAST_ENERGY;
    public static ModConfigSpec.IntValue MARK3_DOUBLE_BLAST_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK3_UNIBEAM_DAMAGE;
    public static ModConfigSpec.IntValue MARK3_UNIBEAM_RANGE;
    public static ModConfigSpec.DoubleValue MARK3_UNIBEAM_ENERGY_PER_SECOND;
    public static ModConfigSpec.IntValue MARK3_UNIBEAM_MAX_TICKS;
    public static ModConfigSpec.DoubleValue MARK3_FLARE_RADIUS;
    public static ModConfigSpec.IntValue MARK3_FLARE_TICKS;
    public static ModConfigSpec.DoubleValue MARK3_FLARE_ENERGY;
    public static ModConfigSpec.IntValue MARK3_FLARE_COOLDOWN;
    public static ModConfigSpec.IntValue MARK3_MISSILE_COUNT;
    public static ModConfigSpec.DoubleValue MARK3_MISSILE_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK3_MISSILE_POWER;
    public static ModConfigSpec.DoubleValue MARK3_MISSILE_ENERGY;
    public static ModConfigSpec.IntValue MARK3_MISSILE_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK5_ARMOR;
    public static ModConfigSpec.DoubleValue MARK5_TOUGHNESS;
    public static ModConfigSpec.DoubleValue MARK5_KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.IntValue MARK5_DURABILITY;
    public static ModConfigSpec.DoubleValue MARK5_SPEED_BONUS;
    public static ModConfigSpec.DoubleValue MARK5_FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue MARK5_FLIGHT_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue MARK5_REPULSOR_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK5_REPULSOR_ENERGY;
    public static ModConfigSpec.DoubleValue MARK5_UNIBEAM_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK5_UNIBEAM_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue MARK5_DASH_DISTANCE;
    public static ModConfigSpec.DoubleValue MARK5_DASH_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK5_DASH_ENERGY;
    public static ModConfigSpec.IntValue MARK5_DASH_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK5_LASER_BURST_RADIUS;
    public static ModConfigSpec.DoubleValue MARK5_LASER_BURST_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK5_LASER_BURST_ENERGY;
    public static ModConfigSpec.IntValue MARK5_LASER_BURST_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK50_ARMOR;
    public static ModConfigSpec.DoubleValue MARK50_TOUGHNESS;
    public static ModConfigSpec.DoubleValue MARK50_KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.IntValue MARK50_DURABILITY;
    public static ModConfigSpec.DoubleValue MARK50_SPEED_BONUS;
    public static ModConfigSpec.DoubleValue MARK50_FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue MARK50_FLIGHT_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue MARK50_REPULSOR_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK50_BLADE_DAMAGE;
    public static ModConfigSpec.IntValue MARK50_BLADE_TICKS;
    public static ModConfigSpec.DoubleValue MARK50_BLADE_ENERGY;
    public static ModConfigSpec.IntValue MARK50_BLADE_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK50_UNIBEAM_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK50_SHIELD_REDUCTION;
    public static ModConfigSpec.DoubleValue MARK50_SHIELD_ENERGY_PER_SECOND;
    public static ModConfigSpec.IntValue MARK50_SHIELD_MAX_TICKS;
    public static ModConfigSpec.IntValue MARK50_CANNON_CHARGE_TICKS;
    public static ModConfigSpec.DoubleValue MARK50_CANNON_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK50_CANNON_POWER;
    public static ModConfigSpec.DoubleValue MARK50_CANNON_ENERGY;
    public static ModConfigSpec.IntValue MARK50_CANNON_COOLDOWN;
    public static ModConfigSpec.DoubleValue MARK85_ARMOR;
    public static ModConfigSpec.DoubleValue MARK85_TOUGHNESS;
    public static ModConfigSpec.DoubleValue MARK85_KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.IntValue MARK85_DURABILITY;
    public static ModConfigSpec.DoubleValue MARK85_SPEED_BONUS;
    public static ModConfigSpec.DoubleValue MARK85_FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue MARK85_FLIGHT_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue MARK85_REPULSOR_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK85_REPULSOR_POWER;
    public static ModConfigSpec.DoubleValue MARK85_REPULSOR_ENERGY;
    public static ModConfigSpec.IntValue MARK85_REPULSOR_GAP;
    public static ModConfigSpec.DoubleValue MARK85_BLADE_DAMAGE_1;
    public static ModConfigSpec.DoubleValue MARK85_BLADE_DAMAGE_2;
    public static ModConfigSpec.DoubleValue MARK85_BLADE_DAMAGE_3;
    public static ModConfigSpec.DoubleValue MARK85_BLADE_HAND_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK85_BLADE_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue MARK85_LASER_DAMAGE;
    public static ModConfigSpec.IntValue MARK85_LASER_RANGE;
    public static ModConfigSpec.DoubleValue MARK85_LASER_ENERGY_PER_SECOND;
    public static ModConfigSpec.IntValue MARK85_LASER_MAX_TICKS;
    public static ModConfigSpec.DoubleValue MARK85_SHIELD_ANGLE;
    public static ModConfigSpec.DoubleValue MARK85_SHIELD_ENERGY_PER_SECOND;
    public static ModConfigSpec.IntValue MARK85_SHIELD_MAX_TICKS;
    public static ModConfigSpec.DoubleValue MARK85_REFOCUSER_DAMAGE;
    public static ModConfigSpec.IntValue MARK85_REFOCUSER_JUMPS;
    public static ModConfigSpec.DoubleValue MARK85_REFOCUSER_JUMP_RADIUS;
    public static ModConfigSpec.DoubleValue MARK85_REFOCUSER_ENERGY;
    public static ModConfigSpec.IntValue MARK85_REFOCUSER_COOLDOWN;
    public static ModConfigSpec.IntValue MARK85_REFOCUSER_CHARGED_TICKS;
    public static ModConfigSpec.DoubleValue MARK85_REFOCUSER_CHARGED_FACTOR;
    public static ModConfigSpec.IntValue MARK85_MISSILE_COUNT;
    public static ModConfigSpec.DoubleValue MARK85_MISSILE_DAMAGE;
    public static ModConfigSpec.DoubleValue MARK85_MISSILE_POWER;
    public static ModConfigSpec.DoubleValue MARK85_MISSILE_LOCK_RANGE;
    public static ModConfigSpec.IntValue MARK85_MISSILE_HOMING_TICKS;
    public static ModConfigSpec.DoubleValue MARK85_MISSILE_TURN_DEGREES;
    public static ModConfigSpec.DoubleValue MARK85_MISSILE_ENERGY;
    public static ModConfigSpec.IntValue MARK85_MISSILE_COOLDOWN;
    public static ModConfigSpec.IntValue MARK85_DRONE_COUNT;
    public static ModConfigSpec.IntValue MARK85_DRONE_TICKS;
    public static ModConfigSpec.DoubleValue MARK85_DRONE_DAMAGE;
    public static ModConfigSpec.IntValue MARK85_DRONE_SHOT_GAP;
    public static ModConfigSpec.DoubleValue MARK85_DRONE_RANGE;
    public static ModConfigSpec.DoubleValue MARK85_DRONE_ENERGY;
    public static ModConfigSpec.IntValue MARK85_DRONE_COOLDOWN;
    public static ModConfigSpec.DoubleValue HULKBUSTER_ARMOR;
    public static ModConfigSpec.DoubleValue HULKBUSTER_TOUGHNESS;
    public static ModConfigSpec.DoubleValue HULKBUSTER_KNOCKBACK_RESISTANCE;
    public static ModConfigSpec.IntValue HULKBUSTER_DURABILITY;
    public static ModConfigSpec.DoubleValue HULKBUSTER_SCALE_BONUS;
    public static ModConfigSpec.DoubleValue HULKBUSTER_SPEED_PENALTY;
    public static ModConfigSpec.DoubleValue HULKBUSTER_FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue HULKBUSTER_FLIGHT_ENERGY_PER_SECOND;
    public static ModConfigSpec.BooleanValue HULKBUSTER_EXPLOSION_IMMUNE;
    public static ModConfigSpec.IntValue HULKBUSTER_DROP_HEIGHT;
    public static ModConfigSpec.DoubleValue HULKBUSTER_PUNCH_DAMAGE;
    public static ModConfigSpec.DoubleValue HULKBUSTER_PUNCH_FINAL_DAMAGE;
    public static ModConfigSpec.DoubleValue HULKBUSTER_PUNCH_FINAL_KNOCKBACK;
    public static ModConfigSpec.IntValue HULKBUSTER_PUNCH_COOLDOWN;
    public static ModConfigSpec.DoubleValue HULKBUSTER_UNIBEAM_DAMAGE;
    public static ModConfigSpec.DoubleValue HULKBUSTER_UNIBEAM_ENERGY_PER_SECOND;
    public static ModConfigSpec.DoubleValue HULKBUSTER_GRAB_RANGE;
    public static ModConfigSpec.IntValue HULKBUSTER_GRAB_MAX_TICKS;
    public static ModConfigSpec.DoubleValue HULKBUSTER_THROW_DISTANCE;
    public static ModConfigSpec.DoubleValue HULKBUSTER_THROW_DAMAGE;
    public static ModConfigSpec.IntValue HULKBUSTER_GRAB_COOLDOWN;
    public static ModConfigSpec.DoubleValue HULKBUSTER_VERONICA_REPAIR;
    public static ModConfigSpec.IntValue HULKBUSTER_VERONICA_COOLDOWN;
    public static ModConfigSpec.BooleanValue HULKBUSTER_NEEDS_SKY;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("iron_man");
        REACTOR_HOLD_TICKS = builder.comment("Ticks an Arc Reactor is held to the chest to become Iron Man.")
                .defineInRange("reactorHoldTicks", 40, 1, 400);
        REACTOR_REPLACES_HERO = builder.comment("Whether an Arc Reactor replaces a hero's power with Iron Man's",
                "(false: a hero is refused).")
                .define("reactorReplacesHero", true);
        REACTORS_CRAFTABLE = builder.comment("Whether the Arc Reactors can be crafted (for servers without Avengers",
                "Towers).")
                .define("reactorsCraftable", false);
        ONLY_IRON_MAN_WEARS = builder.comment("Whether only Iron Man may wear a suit (Creative players always may).")
                .define("onlyIronManWears", true);
        ENERGY_MAX = builder.comment("Suit energy when full.")
                .defineInRange("energyMax", 100.0, 1.0, 10000.0);
        RECHARGE_GROUND = builder.comment("Suit energy recharged per second on the ground.")
                .defineInRange("rechargeGround", 3.0, 0.0, 100.0);
        RECHARGE_AIR = builder.comment("Suit energy recharged per second in the air, not flying.")
                .defineInRange("rechargeAir", 1.0, 0.0, 100.0);
        RECHARGE_DELAY_TICKS = builder.comment("Ticks after a move before the suit recharges.")
                .defineInRange("rechargeDelayTicks", 20, 0, 1200);
        EMPTY_SLOW_FALL_TICKS = builder.comment("Ticks of Slow Falling when the suit's energy runs out in flight.")
                .defineInRange("emptySlowFallTicks", 100, 0, 1200);
        SUIT_UP_DAMAGE_FACTOR = builder.comment("Incoming damage is multiplied by this while a suit goes on (a brief",
                "protection, never immunity).")
                .defineInRange("suitUpDamageFactor", 0.5, 0.0, 1.0);
        SUIT_PHASE_COOLDOWN = builder.comment("Ticks after a suit-up or take-off before the next one may start.")
                .defineInRange("suitPhaseCooldown", 40, 0, 1200);
        SUIT_UP_TICKS_MARK1 = builder.comment("Ticks the Mark 1 takes to go on.")
                .defineInRange("suitUpTicksMark1", 60, 1, 400);
        SUIT_UP_TICKS_MARK3 = builder.comment("Ticks the Mark 3 takes to go on.")
                .defineInRange("suitUpTicksMark3", 50, 1, 400);
        SUIT_UP_TICKS_MARK5 = builder.comment("Ticks the Mark 5 takes to unfold.")
                .defineInRange("suitUpTicksMark5", 40, 1, 400);
        SUIT_UP_TICKS_MARK50 = builder.comment("Ticks the Mark 50's nanites take to cover him.")
                .defineInRange("suitUpTicksMark50", 30, 1, 400);
        SUIT_UP_TICKS_MARK85 = builder.comment("Ticks the Mark 85's nanites take to cover him.")
                .defineInRange("suitUpTicksMark85", 24, 1, 400);
        SUIT_UP_TICKS_HULKBUSTER = builder.comment("Ticks of the Hulkbuster's drop, opening, step-in, closing and",
                "growth.")
                .defineInRange("suitUpTicksHulkbuster", 80, 1, 400);
        SUIT_DOWN_FACTOR = builder.comment("A take-off lasts this fraction of the suit-up.")
                .defineInRange("suitDownFactor", 0.5, 0.05, 1.0);
        GANTRY_RANGE = builder.comment("Distance (blocks) from a suit display within which the Mark 3 is bolted on by",
                "the gantry's arms.")
                .defineInRange("gantryRange", 4.0, 0.0, 32.0);
        NANO_REBUILD_TICKS = builder.comment("Ticks for a stowed nanotech housing to rebuild one point of durability.")
                .defineInRange("nanoRebuildTicks", 40, 1, 12000);
        HARD_LANDING_DURABILITY = builder.comment("Durability a hard landing in flight costs the suit (he is unhurt).")
                .defineInRange("hardLandingDurability", 5, 0, 1000);
        MAGNETIC_CLASSIC_SUITS = builder.comment("Whether Magneto can lift Iron Man in a powered classic suit (not",
                "the nanotech ones).")
                .define("magneticClassicSuits", true);
        BOOMS_BREAK_BLOCKS = builder.comment("Whether the suits' booms break blocks (only with mobGriefing too).")
                .define("boomsBreakBlocks", false);
        builder.pop();
        builder.push("mark_1");
        MARK1_ARMOR = builder.comment("Armour of the suit (one item: a whole set).")
                .defineInRange("armor", 18.0, 0.0, 30.0);
        MARK1_TOUGHNESS = builder.comment("Armour toughness of the suit.")
                .defineInRange("toughness", 3.0, 0.0, 20.0);
        MARK1_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the suit.")
                .defineInRange("knockbackResistance", 0.5, 0.0, 1.0);
        MARK1_DURABILITY = builder.comment("Durability of the suit.")
                .defineInRange("durability", 825, 1, 100000);
        MARK1_SPEED_PENALTY = builder.comment("Walking speed taken away by the heavy suit, as a fraction.")
                .defineInRange("speedPenalty", 0.2, 0.0, 0.9);
        MARK1_FLAME_DAMAGE = builder.comment("Damage of the flamethrower (every 10 ticks).")
                .defineInRange("flameDamage", 2.0, 0.0, 200.0);
        MARK1_FLAME_LENGTH = builder.comment("Length (blocks) of the flamethrower's cone.")
                .defineInRange("flameLength", 5.0, 1.0, 16.0);
        MARK1_FLAME_ANGLE = builder.comment("Angle (degrees) of the flamethrower's cone.")
                .defineInRange("flameAngle", 30.0, 5.0, 120.0);
        MARK1_FLAME_FIRE_SECONDS = builder.comment("Seconds the flamethrower sets creatures burning.")
                .defineInRange("flameFireSeconds", 4, 0, 60);
        MARK1_FLAME_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the flamethrower.")
                .defineInRange("flameEnergyPerSecond", 4.0, 0.0, 100.0);
        MARK1_FLAME_MAX_TICKS = builder.comment("The longest (ticks) the flamethrower can be held.")
                .defineInRange("flameMaxTicks", 60, 1, 1200);
        MARK1_FLAME_COOLDOWN = builder.comment("Cooldown (ticks) of the flamethrower.")
                .defineInRange("flameCooldown", 40, 0, 72000);
        MARK1_FLAME_SETS_BLOCKS = builder.comment("Whether the flamethrower sets blocks on fire (only with",
                "mobGriefing too).")
                .define("mark1FlameSetsBlocks", false);
        MARK1_BOOST_UP_TICKS = builder.comment("Ticks the Rocket Boost goes straight up.")
                .defineInRange("boostUpTicks", 10, 0, 100);
        MARK1_BOOST_FORWARD_TICKS = builder.comment("Ticks the Rocket Boost goes forward.")
                .defineInRange("boostForwardTicks", 20, 0, 100);
        MARK1_BOOST_ENERGY = builder.comment("Suit energy the Rocket Boost costs.")
                .defineInRange("boostEnergy", 30.0, 0.0, 100.0);
        MARK1_BOOST_COOLDOWN = builder.comment("Cooldown (ticks) of the Rocket Boost.")
                .defineInRange("boostCooldown", 200, 0, 72000);
        MARK1_CRASH_DURABILITY = builder.comment("Durability the crash landing costs the suit.")
                .defineInRange("crashDurability", 30, 0, 1000);
        MARK1_CRASH_DAMAGE = builder.comment("Damage of the crash landing to creatures near.")
                .defineInRange("crashDamage", 3.0, 0.0, 200.0);
        MARK1_ROCKET_DAMAGE = builder.comment("Damage of the Arm Rocket.")
                .defineInRange("rocketDamage", 8.0, 0.0, 200.0);
        MARK1_ROCKET_POWER = builder.comment("Power of the small boom of the Arm Rocket (it never breaks blocks",
                "unless [iron_man] boomsBreakBlocks and mobGriefing).")
                .defineInRange("rocketPower", 1.2, 0.0, 8.0);
        MARK1_ROCKET_ENERGY = builder.comment("Suit energy the Arm Rocket costs.")
                .defineInRange("rocketEnergy", 15.0, 0.0, 100.0);
        MARK1_ROCKET_COOLDOWN = builder.comment("Cooldown (ticks) of the Arm Rocket.")
                .defineInRange("rocketCooldown", 80, 0, 72000);
        MARK1_BRACE_REDUCTION = builder.comment("Fraction of incoming damage the Brace takes away.")
                .defineInRange("braceReduction", 0.6, 0.0, 1.0);
        MARK1_BRACE_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the Brace.")
                .defineInRange("braceEnergyPerSecond", 2.0, 0.0, 100.0);
        MARK1_PUNCH_DAMAGE = builder.comment("Damage of the Heavy Punch.")
                .defineInRange("punchDamage", 10.0, 0.0, 200.0);
        MARK1_PUNCH_COOLDOWN = builder.comment("Cooldown (ticks) of the Heavy Punch.")
                .defineInRange("punchCooldown", 40, 0, 72000);
        builder.pop();
        builder.push("mark_3");
        MARK3_ARMOR = builder.comment("Armour of the suit (one item: a whole set).")
                .defineInRange("armor", 16.0, 0.0, 30.0);
        MARK3_TOUGHNESS = builder.comment("Armour toughness of the suit.")
                .defineInRange("toughness", 2.0, 0.0, 20.0);
        MARK3_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the suit.")
                .defineInRange("knockbackResistance", 0.2, 0.0, 1.0);
        MARK3_DURABILITY = builder.comment("Durability of the suit.")
                .defineInRange("durability", 1100, 1, 100000);
        MARK3_FLIGHT_SPEED = builder.comment("Flight speed in the suit.")
                .defineInRange("flightSpeed", 0.1, 0.01, 0.5);
        MARK3_FLIGHT_ENERGY_PER_SECOND = builder.comment("Suit energy per second of flight.")
                .defineInRange("flightEnergyPerSecond", 0.6, 0.0, 100.0);
        MARK3_REPULSOR_DAMAGE = builder.comment("Damage of the Repulsor.")
                .defineInRange("repulsorDamage", 6.0, 0.0, 200.0);
        MARK3_REPULSOR_RANGE = builder.comment("Range (blocks) of the Repulsor.")
                .defineInRange("repulsorRange", 32, 4, 128);
        MARK3_REPULSOR_POWER = builder.comment("Power of the small boom of the Repulsor (it never breaks blocks",
                "unless [iron_man] boomsBreakBlocks and mobGriefing).")
                .defineInRange("repulsorPower", 0.8, 0.0, 8.0);
        MARK3_REPULSOR_ENERGY = builder.comment("Suit energy the Repulsor costs.")
                .defineInRange("repulsorEnergy", 4.0, 0.0, 100.0);
        MARK3_REPULSOR_GAP = builder.comment("Ticks between two Repulsor blasts.")
                .defineInRange("repulsorGap", 10, 1, 200);
        MARK3_DOUBLE_BLAST_DAMAGE = builder.comment("Damage of the Double-Hand Blast.")
                .defineInRange("doubleBlastDamage", 10.0, 0.0, 200.0);
        MARK3_DOUBLE_BLAST_POWER = builder.comment("Power of the small boom of the Double-Hand Blast (it never breaks",
                "blocks unless [iron_man] boomsBreakBlocks and mobGriefing).")
                .defineInRange("doubleBlastPower", 1.2, 0.0, 8.0);
        MARK3_DOUBLE_BLAST_ENERGY = builder.comment("Suit energy the Double-Hand Blast costs.")
                .defineInRange("doubleBlastEnergy", 8.0, 0.0, 100.0);
        MARK3_DOUBLE_BLAST_COOLDOWN = builder.comment("Cooldown (ticks) of the Double-Hand Blast.")
                .defineInRange("doubleBlastCooldown", 40, 0, 72000);
        MARK3_UNIBEAM_DAMAGE = builder.comment("Damage of the Unibeam (every 10 ticks).")
                .defineInRange("unibeamDamage", 4.0, 0.0, 200.0);
        MARK3_UNIBEAM_RANGE = builder.comment("Range (blocks) of the Unibeam.")
                .defineInRange("unibeamRange", 24, 4, 128);
        MARK3_UNIBEAM_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the Unibeam.")
                .defineInRange("unibeamEnergyPerSecond", 6.0, 0.0, 100.0);
        MARK3_UNIBEAM_MAX_TICKS = builder.comment("The longest (ticks) the Unibeam can be held.")
                .defineInRange("unibeamMaxTicks", 60, 1, 1200);
        MARK3_FLARE_RADIUS = builder.comment("Radius (blocks) of the Flares.")
                .defineInRange("flareRadius", 16.0, 1.0, 64.0);
        MARK3_FLARE_TICKS = builder.comment("Ticks monsters cannot target him after the Flares.")
                .defineInRange("flareTicks", 100, 1, 1200);
        MARK3_FLARE_ENERGY = builder.comment("Suit energy the Flares cost.")
                .defineInRange("flareEnergy", 10.0, 0.0, 100.0);
        MARK3_FLARE_COOLDOWN = builder.comment("Cooldown (ticks) of the Flares.")
                .defineInRange("flareCooldown", 300, 0, 72000);
        MARK3_MISSILE_COUNT = builder.comment("Shoulder missiles in a volley.")
                .defineInRange("missileCount", 4, 1, 16);
        MARK3_MISSILE_DAMAGE = builder.comment("Damage of each shoulder missile.")
                .defineInRange("missileDamage", 3.0, 0.0, 200.0);
        MARK3_MISSILE_POWER = builder.comment("Power of the small boom of each shoulder missile (it never breaks",
                "blocks unless [iron_man] boomsBreakBlocks and mobGriefing).")
                .defineInRange("missilePower", 0.6, 0.0, 8.0);
        MARK3_MISSILE_ENERGY = builder.comment("Suit energy a volley costs.")
                .defineInRange("missileEnergy", 12.0, 0.0, 100.0);
        MARK3_MISSILE_COOLDOWN = builder.comment("Cooldown (ticks) of the shoulder missiles.")
                .defineInRange("missileCooldown", 100, 0, 72000);
        builder.pop();
        builder.push("mark_5");
        MARK5_ARMOR = builder.comment("Armour of the suit (one item: a whole set).")
                .defineInRange("armor", 12.0, 0.0, 30.0);
        MARK5_TOUGHNESS = builder.comment("Armour toughness of the suit.")
                .defineInRange("toughness", 1.0, 0.0, 20.0);
        MARK5_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the suit.")
                .defineInRange("knockbackResistance", 0.1, 0.0, 1.0);
        MARK5_DURABILITY = builder.comment("Durability of the suit.")
                .defineInRange("durability", 600, 1, 100000);
        MARK5_SPEED_BONUS = builder.comment("Walking speed added by the light suit, as a fraction.")
                .defineInRange("speedBonus", 0.05, 0.0, 1.0);
        MARK5_FLIGHT_SPEED = builder.comment("Flight speed in the suit.")
                .defineInRange("flightSpeed", 0.08, 0.01, 0.5);
        MARK5_FLIGHT_ENERGY_PER_SECOND = builder.comment("Suit energy per second of flight.")
                .defineInRange("flightEnergyPerSecond", 0.5, 0.0, 100.0);
        MARK5_REPULSOR_DAMAGE = builder.comment("Damage of the Repulsor.")
                .defineInRange("repulsorDamage", 5.0, 0.0, 200.0);
        MARK5_REPULSOR_ENERGY = builder.comment("Suit energy the Repulsor costs.")
                .defineInRange("repulsorEnergy", 3.0, 0.0, 100.0);
        MARK5_UNIBEAM_DAMAGE = builder.comment("Damage of the Unibeam (every 10 ticks).")
                .defineInRange("unibeamDamage", 3.0, 0.0, 200.0);
        MARK5_UNIBEAM_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the Unibeam.")
                .defineInRange("unibeamEnergyPerSecond", 5.0, 0.0, 100.0);
        MARK5_DASH_DISTANCE = builder.comment("Distance (blocks) of the Dash Punch.")
                .defineInRange("dashDistance", 6.0, 1.0, 32.0);
        MARK5_DASH_DAMAGE = builder.comment("Damage of the Dash Punch.")
                .defineInRange("dashDamage", 8.0, 0.0, 200.0);
        MARK5_DASH_ENERGY = builder.comment("Suit energy the Dash Punch costs.")
                .defineInRange("dashEnergy", 10.0, 0.0, 100.0);
        MARK5_DASH_COOLDOWN = builder.comment("Cooldown (ticks) of the Dash Punch.")
                .defineInRange("dashCooldown", 60, 0, 72000);
        MARK5_LASER_BURST_RADIUS = builder.comment("Radius (blocks) of the Laser Burst.")
                .defineInRange("laserBurstRadius", 6.0, 1.0, 24.0);
        MARK5_LASER_BURST_DAMAGE = builder.comment("Damage of the Laser Burst.")
                .defineInRange("laserBurstDamage", 6.0, 0.0, 200.0);
        MARK5_LASER_BURST_ENERGY = builder.comment("Suit energy the Laser Burst costs.")
                .defineInRange("laserBurstEnergy", 20.0, 0.0, 100.0);
        MARK5_LASER_BURST_COOLDOWN = builder.comment("Cooldown (ticks) of the Laser Burst.")
                .defineInRange("laserBurstCooldown", 200, 0, 72000);
        builder.pop();
        builder.push("mark_50");
        MARK50_ARMOR = builder.comment("Armour of the suit (one item: a whole set).")
                .defineInRange("armor", 20.0, 0.0, 30.0);
        MARK50_TOUGHNESS = builder.comment("Armour toughness of the suit.")
                .defineInRange("toughness", 8.0, 0.0, 20.0);
        MARK50_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the suit.")
                .defineInRange("knockbackResistance", 0.2, 0.0, 1.0);
        MARK50_DURABILITY = builder.comment("Durability of the suit.")
                .defineInRange("durability", 1815, 1, 100000);
        MARK50_SPEED_BONUS = builder.comment("Walking speed added, as a fraction.")
                .defineInRange("speedBonus", 0.05, 0.0, 1.0);
        MARK50_FLIGHT_SPEED = builder.comment("Flight speed in the suit.")
                .defineInRange("flightSpeed", 0.11, 0.01, 0.5);
        MARK50_FLIGHT_ENERGY_PER_SECOND = builder.comment("Suit energy per second of flight.")
                .defineInRange("flightEnergyPerSecond", 0.4, 0.0, 100.0);
        MARK50_REPULSOR_DAMAGE = builder.comment("Damage of the Nano Repulsor.")
                .defineInRange("repulsorDamage", 7.0, 0.0, 200.0);
        MARK50_BLADE_DAMAGE = builder.comment("Damage of the Nano Blade.")
                .defineInRange("bladeDamage", 9.0, 0.0, 200.0);
        MARK50_BLADE_TICKS = builder.comment("Ticks the Nano Blade lasts.")
                .defineInRange("bladeTicks", 100, 1, 1200);
        MARK50_BLADE_ENERGY = builder.comment("Suit energy the Nano Blade costs.")
                .defineInRange("bladeEnergy", 8.0, 0.0, 100.0);
        MARK50_BLADE_COOLDOWN = builder.comment("Cooldown (ticks) of the Nano Blade.")
                .defineInRange("bladeCooldown", 160, 0, 72000);
        MARK50_UNIBEAM_DAMAGE = builder.comment("Damage of the Unibeam (every 10 ticks).")
                .defineInRange("unibeamDamage", 5.0, 0.0, 200.0);
        MARK50_SHIELD_REDUCTION = builder.comment("Fraction of damage from the front the Nano Shield takes away.")
                .defineInRange("shieldReduction", 0.8, 0.0, 1.0);
        MARK50_SHIELD_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the Nano Shield.")
                .defineInRange("shieldEnergyPerSecond", 3.0, 0.0, 100.0);
        MARK50_SHIELD_MAX_TICKS = builder.comment("The longest (ticks) the Nano Shield can be held.")
                .defineInRange("shieldMaxTicks", 160, 1, 1200);
        MARK50_CANNON_CHARGE_TICKS = builder.comment("Ticks the Nano Cannon charges.")
                .defineInRange("cannonChargeTicks", 20, 1, 200);
        MARK50_CANNON_DAMAGE = builder.comment("Damage of the Nano Cannon.")
                .defineInRange("cannonDamage", 14.0, 0.0, 200.0);
        MARK50_CANNON_POWER = builder.comment("Power of the small boom of the Nano Cannon (it never breaks blocks",
                "unless [iron_man] boomsBreakBlocks and mobGriefing).")
                .defineInRange("cannonPower", 1.5, 0.0, 8.0);
        MARK50_CANNON_ENERGY = builder.comment("Suit energy the Nano Cannon costs.")
                .defineInRange("cannonEnergy", 25.0, 0.0, 100.0);
        MARK50_CANNON_COOLDOWN = builder.comment("Cooldown (ticks) of the Nano Cannon.")
                .defineInRange("cannonCooldown", 160, 0, 72000);
        builder.pop();
        builder.push("mark_85");
        MARK85_ARMOR = builder.comment("Armour of the suit (one item: a whole set).")
                .defineInRange("armor", 20.0, 0.0, 30.0);
        MARK85_TOUGHNESS = builder.comment("Armour toughness of the suit.")
                .defineInRange("toughness", 12.0, 0.0, 20.0);
        MARK85_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the suit.")
                .defineInRange("knockbackResistance", 0.4, 0.0, 1.0);
        MARK85_DURABILITY = builder.comment("Durability of the suit.")
                .defineInRange("durability", 2035, 1, 100000);
        MARK85_SPEED_BONUS = builder.comment("Walking speed added, as a fraction.")
                .defineInRange("speedBonus", 0.1, 0.0, 1.0);
        MARK85_FLIGHT_SPEED = builder.comment("Flight speed in the suit.")
                .defineInRange("flightSpeed", 0.12, 0.01, 0.5);
        MARK85_FLIGHT_ENERGY_PER_SECOND = builder.comment("Suit energy per second of flight.")
                .defineInRange("flightEnergyPerSecond", 0.35, 0.0, 100.0);
        MARK85_REPULSOR_DAMAGE = builder.comment("Damage of each Boosted Repulsor blast.")
                .defineInRange("repulsorDamage", 9.0, 0.0, 200.0);
        MARK85_REPULSOR_POWER = builder.comment("Power of the small boom of each Boosted Repulsor blast (it never",
                "breaks blocks unless [iron_man] boomsBreakBlocks and mobGriefing).")
                .defineInRange("repulsorPower", 1.0, 0.0, 8.0);
        MARK85_REPULSOR_ENERGY = builder.comment("Suit energy the Boosted Repulsors cost.")
                .defineInRange("repulsorEnergy", 6.0, 0.0, 100.0);
        MARK85_REPULSOR_GAP = builder.comment("Ticks between two pairs of Boosted Repulsor blasts.")
                .defineInRange("repulsorGap", 12, 1, 200);
        MARK85_BLADE_DAMAGE_1 = builder.comment("Damage of the first slash of the Energy Blades.")
                .defineInRange("bladeDamage1", 10.0, 0.0, 200.0);
        MARK85_BLADE_DAMAGE_2 = builder.comment("Damage of the second slash.")
                .defineInRange("bladeDamage2", 10.0, 0.0, 200.0);
        MARK85_BLADE_DAMAGE_3 = builder.comment("Damage of the third slash (the sweep).")
                .defineInRange("bladeDamage3", 14.0, 0.0, 200.0);
        MARK85_BLADE_HAND_DAMAGE = builder.comment("Damage of his empty hand's blow while the blades are out.")
                .defineInRange("bladeHandDamage", 12.0, 0.0, 200.0);
        MARK85_BLADE_ENERGY_PER_SECOND = builder.comment("Suit energy per second while the Energy Blades are out.")
                .defineInRange("bladeEnergyPerSecond", 1.0, 0.0, 100.0);
        MARK85_LASER_DAMAGE = builder.comment("Damage of the Giant Chest Laser (every 10 ticks).")
                .defineInRange("laserDamage", 7.0, 0.0, 200.0);
        MARK85_LASER_RANGE = builder.comment("Range (blocks) of the Giant Chest Laser.")
                .defineInRange("laserRange", 32, 4, 128);
        MARK85_LASER_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the Giant Chest Laser.")
                .defineInRange("laserEnergyPerSecond", 8.0, 0.0, 100.0);
        MARK85_LASER_MAX_TICKS = builder.comment("The longest (ticks) the Giant Chest Laser can be held.")
                .defineInRange("laserMaxTicks", 80, 1, 1200);
        MARK85_SHIELD_ANGLE = builder.comment("The angle (degrees) in front of him the Giant Energy Shield covers.")
                .defineInRange("shieldAngle", 120.0, 10.0, 360.0);
        MARK85_SHIELD_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the Giant Energy Shield.")
                .defineInRange("shieldEnergyPerSecond", 4.0, 0.0, 100.0);
        MARK85_SHIELD_MAX_TICKS = builder.comment("The longest (ticks) the Giant Energy Shield can be held.")
                .defineInRange("shieldMaxTicks", 200, 1, 1200);
        MARK85_REFOCUSER_DAMAGE = builder.comment("Damage of the Lightning Refocuser's first strike.")
                .defineInRange("refocuserDamage", 10.0, 0.0, 200.0);
        MARK85_REFOCUSER_JUMPS = builder.comment("Creatures the Lightning Refocuser jumps on to after the first.")
                .defineInRange("refocuserJumps", 4, 0, 16);
        MARK85_REFOCUSER_JUMP_RADIUS = builder.comment("Radius (blocks) of each jump of the Lightning Refocuser.")
                .defineInRange("refocuserJumpRadius", 6.0, 1.0, 32.0);
        MARK85_REFOCUSER_ENERGY = builder.comment("Suit energy the Lightning Refocuser costs.")
                .defineInRange("refocuserEnergy", 25.0, 0.0, 100.0);
        MARK85_REFOCUSER_COOLDOWN = builder.comment("Cooldown (ticks) of the Lightning Refocuser.")
                .defineInRange("refocuserCooldown", 240, 0, 72000);
        MARK85_REFOCUSER_CHARGED_TICKS = builder.comment("Ticks after a lightning strike in which the Refocuser is",
                "charged (free and stronger).")
                .defineInRange("refocuserChargedTicks", 200, 0, 12000);
        MARK85_REFOCUSER_CHARGED_FACTOR = builder.comment("How much stronger the charged Refocuser is.")
                .defineInRange("refocuserChargedFactor", 1.5, 1.0, 10.0);
        MARK85_MISSILE_COUNT = builder.comment("Homing mini-missiles in a volley.")
                .defineInRange("missileCount", 6, 1, 16);
        MARK85_MISSILE_DAMAGE = builder.comment("Damage of each homing mini-missile.")
                .defineInRange("missileDamage", 4.0, 0.0, 200.0);
        MARK85_MISSILE_POWER = builder.comment("Power of the small boom of each homing mini-missile (it never breaks",
                "blocks unless [iron_man] boomsBreakBlocks and mobGriefing).")
                .defineInRange("missilePower", 0.6, 0.0, 8.0);
        MARK85_MISSILE_LOCK_RANGE = builder.comment("Range (blocks) in which the missiles lock on.")
                .defineInRange("missileLockRange", 24.0, 4.0, 128.0);
        MARK85_MISSILE_HOMING_TICKS = builder.comment("Ticks a missile follows its target before it pops.")
                .defineInRange("missileHomingTicks", 200, 1, 2400);
        MARK85_MISSILE_TURN_DEGREES = builder.comment("Degrees a missile may turn per tick.")
                .defineInRange("missileTurnDegrees", 12.0, 1.0, 180.0);
        MARK85_MISSILE_ENERGY = builder.comment("Suit energy a volley costs.")
                .defineInRange("missileEnergy", 15.0, 0.0, 100.0);
        MARK85_MISSILE_COOLDOWN = builder.comment("Cooldown (ticks) of the homing mini-missiles.")
                .defineInRange("missileCooldown", 120, 0, 72000);
        MARK85_DRONE_COUNT = builder.comment("Nanotech drones that form behind him.")
                .defineInRange("droneCount", 2, 1, 8);
        MARK85_DRONE_TICKS = builder.comment("Ticks the drones stay.")
                .defineInRange("droneTicks", 400, 1, 12000);
        MARK85_DRONE_DAMAGE = builder.comment("Damage of each drone's laser.")
                .defineInRange("droneDamage", 6.0, 0.0, 200.0);
        MARK85_DRONE_SHOT_GAP = builder.comment("Ticks between two shots of a drone.")
                .defineInRange("droneShotGap", 15, 1, 400);
        MARK85_DRONE_RANGE = builder.comment("Range (blocks) of the drones' lasers.")
                .defineInRange("droneRange", 20.0, 2.0, 64.0);
        MARK85_DRONE_ENERGY = builder.comment("Suit energy the drones cost.")
                .defineInRange("droneEnergy", 30.0, 0.0, 100.0);
        MARK85_DRONE_COOLDOWN = builder.comment("Cooldown (ticks) of the nanotech drones.")
                .defineInRange("droneCooldown", 900, 0, 72000);
        builder.pop();
        builder.push("hulkbuster");
        HULKBUSTER_ARMOR = builder.comment("Armour of the suit (one item: a whole set).")
                .defineInRange("armor", 20.0, 0.0, 30.0);
        HULKBUSTER_TOUGHNESS = builder.comment("Armour toughness of the suit.")
                .defineInRange("toughness", 12.0, 0.0, 20.0);
        HULKBUSTER_KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance of the suit.")
                .defineInRange("knockbackResistance", 1.0, 0.0, 1.0);
        HULKBUSTER_DURABILITY = builder.comment("Durability of the suit.")
                .defineInRange("durability", 3000, 1, 100000);
        HULKBUSTER_SCALE_BONUS = builder.comment("Scale added in the Hulkbuster (2.5: three and a half times).")
                .defineInRange("scaleBonus", 2.5, 0.0, 6.0);
        HULKBUSTER_SPEED_PENALTY = builder.comment("Walking speed taken away, as a fraction.")
                .defineInRange("speedPenalty", 0.1, 0.0, 0.9);
        HULKBUSTER_FLIGHT_SPEED = builder.comment("Flight speed in the suit.")
                .defineInRange("flightSpeed", 0.06, 0.01, 0.5);
        HULKBUSTER_FLIGHT_ENERGY_PER_SECOND = builder.comment("Suit energy per second of flight.")
                .defineInRange("flightEnergyPerSecond", 1.0, 0.0, 100.0);
        HULKBUSTER_EXPLOSION_IMMUNE = builder.comment("Whether explosions never hurt the Hulkbuster's wearer.")
                .define("explosionImmune", true);
        HULKBUSTER_DROP_HEIGHT = builder.comment("Height (blocks) Veronica drops the frame from.")
                .defineInRange("dropHeight", 40, 4, 200);
        HULKBUSTER_PUNCH_DAMAGE = builder.comment("Damage of each punch of the Rapid Punch Combo.")
                .defineInRange("punchDamage", 6.0, 0.0, 200.0);
        HULKBUSTER_PUNCH_FINAL_DAMAGE = builder.comment("Damage of the combo's last punch.")
                .defineInRange("punchFinalDamage", 10.0, 0.0, 200.0);
        HULKBUSTER_PUNCH_FINAL_KNOCKBACK = builder.comment("Knockback of the combo's last punch.")
                .defineInRange("punchFinalKnockback", 2.5, 0.0, 8.0);
        HULKBUSTER_PUNCH_COOLDOWN = builder.comment("Cooldown (ticks) of the Rapid Punch Combo.")
                .defineInRange("punchCooldown", 60, 0, 72000);
        HULKBUSTER_UNIBEAM_DAMAGE = builder.comment("Damage of the Heavy Unibeam (every 10 ticks).")
                .defineInRange("unibeamDamage", 8.0, 0.0, 200.0);
        HULKBUSTER_UNIBEAM_ENERGY_PER_SECOND = builder.comment("Suit energy per second of the Heavy Unibeam.")
                .defineInRange("unibeamEnergyPerSecond", 10.0, 0.0, 100.0);
        HULKBUSTER_GRAB_RANGE = builder.comment("Reach (blocks) of Grab & Throw.")
                .defineInRange("grabRange", 5.0, 1.0, 16.0);
        HULKBUSTER_GRAB_MAX_TICKS = builder.comment("The longest (ticks) a creature is held.")
                .defineInRange("grabMaxTicks", 60, 1, 1200);
        HULKBUSTER_THROW_DISTANCE = builder.comment("Distance (blocks) of the throw.")
                .defineInRange("throwDistance", 12.0, 1.0, 64.0);
        HULKBUSTER_THROW_DAMAGE = builder.comment("Damage of the throw's landing.")
                .defineInRange("throwDamage", 10.0, 0.0, 200.0);
        HULKBUSTER_GRAB_COOLDOWN = builder.comment("Cooldown (ticks) of Grab & Throw.")
                .defineInRange("grabCooldown", 160, 0, 72000);
        HULKBUSTER_VERONICA_REPAIR = builder.comment("Fraction of the suit's durability a new arm from Veronica",
                "repairs.")
                .defineInRange("veronicaRepair", 0.5, 0.0, 1.0);
        HULKBUSTER_VERONICA_COOLDOWN = builder.comment("Cooldown (ticks) of Veronica's new arm.")
                .defineInRange("veronicaCooldown", 1200, 0, 72000);
        HULKBUSTER_NEEDS_SKY = builder.comment("Whether Veronica needs open sky to drop the frame or an arm.")
                .define("needsSky", true);
        builder.pop();
    }

    private IronManConfig() {
    }
}
