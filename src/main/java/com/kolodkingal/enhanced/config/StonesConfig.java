package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The Infinity Stones (3.6): who may hold them, the Infinity Gauntlet, and what each stone does in a hand. Every
 * option of the plan is defined here by the 3.6 skeleton (lanes L1 and L2 read them); {@code EnhancedConfig} calls
 * {@link #define} while it builds the server config (TOML sections {@code [stones]}, {@code [gauntlet]}, {@code
 * [reality_stone]}, {@code [mind_stone]}, {@code [time_stone]}, {@code [power_stone]} and {@code [space_stone]}). They
 * are read from the server and synced to clients, and read when used (never cached), so an edit applies at once.
 */
public final class StonesConfig {
    /** How many of each site stone there are (the Reality, Time, Power and Soul Stones). */
    public enum Uniqueness {
        /** Each player may take each site stone once (two brothers can each have a set). */
        PLAYER,
        /** One of each in the whole world: a taken stone leaves every other site empty. */
        WORLD,
        /** Every site gives its stone every time (creative servers). */
        UNLIMITED
    }

    /** The TOML sections of these options. */
    public static final String SECTION_STONES = "stones";
    public static final String SECTION_GAUNTLET = "gauntlet";
    public static final String SECTION_REALITY_STONE = "reality_stone";
    public static final String SECTION_MIND_STONE = "mind_stone";
    public static final String SECTION_TIME_STONE = "time_stone";
    public static final String SECTION_POWER_STONE = "power_stone";
    public static final String SECTION_SPACE_STONE = "space_stone";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.EnumValue<Uniqueness> UNIQUENESS;
    public static ConfigSpec.IntValue GRACE_SECONDS;
    public static ConfigSpec.IntValue HAND_GRACE_FACTOR;
    public static ConfigSpec.DoubleValue REJECT_DAMAGE;
    public static ConfigSpec.BooleanValue CUSTOM_BEARERS;
    public static ConfigSpec.BooleanValue VISION_HOLDS_MIND;
    public static ConfigSpec.BooleanValue SORCERERS_HOLD_TIME;
    public static ConfigSpec.BooleanValue RETURN_LOST;
    public static ConfigSpec.BooleanValue GAUNTLET_CRAFTABLE;
    public static ConfigSpec.BooleanValue SNAP_ENABLED;
    public static ConfigSpec.IntValue SNAP_RADIUS;
    public static ConfigSpec.DoubleValue SNAP_FRACTION;
    public static ConfigSpec.IntValue SNAP_COOLDOWN_TICKS;
    public static ConfigSpec.IntValue SNAP_HOLD_TICKS;
    public static ConfigSpec.BooleanValue SNAP_IN_ASGARD;
    public static ConfigSpec.DoubleValue SNAP_COST_HEALTH;
    public static ConfigSpec.BooleanValue MIND_FOR_THANOS;
    public static ConfigSpec.DoubleValue REALITY_RADIUS;
    public static ConfigSpec.DoubleValue REALITY_EXHAUSTION_PER_BUBBLE;
    public static ConfigSpec.IntValue REALITY_BOSS_RATE;
    public static ConfigSpec.DoubleValue REALITY_BOSS_COST;
    public static ConfigSpec.IntValue REALITY_TRANSMUTE_RANGE;
    public static ConfigSpec.IntValue REALITY_TRANSMUTE_TICKS;
    public static ConfigSpec.IntValue REALITY_TRANSMUTE_COOLDOWN;
    public static ConfigSpec.IntValue REALITY_ILLUSION_TICKS;
    public static ConfigSpec.IntValue REALITY_ILLUSION_COOLDOWN;
    public static ConfigSpec.DoubleValue MIND_BEAM_DAMAGE;
    public static ConfigSpec.IntValue MIND_BEAM_RANGE;
    public static ConfigSpec.IntValue MIND_BEAM_MAX_TICKS;
    public static ConfigSpec.IntValue MIND_BEAM_COOLDOWN;
    public static ConfigSpec.IntValue MIND_CONFUSE_EVERY;
    public static ConfigSpec.IntValue MIND_COMMAND_RANGE;
    public static ConfigSpec.IntValue MIND_COMMAND_TICKS;
    public static ConfigSpec.IntValue MIND_COMMAND_COOLDOWN;
    public static ConfigSpec.DoubleValue MIND_EXHAUSTION_PER_TICK;
    public static ConfigSpec.IntValue TIME_SLOW_RADIUS;
    public static ConfigSpec.IntValue TIME_SLOW_FACTOR;
    public static ConfigSpec.IntValue TIME_SLOW_MAX_TICKS;
    public static ConfigSpec.IntValue TIME_SLOW_COOLDOWN;
    public static ConfigSpec.BooleanValue TIME_BOSS_FACTOR_HALF;
    public static ConfigSpec.IntValue TIME_REWIND_SECONDS;
    public static ConfigSpec.IntValue TIME_REWIND_COOLDOWN;
    public static ConfigSpec.BooleanValue TIME_CROP_MAGIC;
    public static ConfigSpec.IntValue TIME_CROP_COOLDOWN;
    public static ConfigSpec.DoubleValue POWER_BEAM_DAMAGE;
    public static ConfigSpec.IntValue POWER_BEAM_RANGE;
    public static ConfigSpec.IntValue POWER_EXPLODE_EVERY;
    public static ConfigSpec.DoubleValue POWER_EXPLOSION_DAMAGE;
    public static ConfigSpec.DoubleValue POWER_EXPLOSION_RADIUS;
    public static ConfigSpec.BooleanValue POWER_TERRAIN_DAMAGE;
    public static ConfigSpec.DoubleValue POWER_CRATER_RADIUS;
    public static ConfigSpec.IntValue POWER_BEAM_MAX_TICKS;
    public static ConfigSpec.IntValue POWER_OVERHEAT_COOLDOWN;
    public static ConfigSpec.IntValue POWER_RELEASE_COOLDOWN;
    public static ConfigSpec.DoubleValue POWER_BOLT_DAMAGE;
    public static ConfigSpec.DoubleValue POWER_BOLT_SPLASH;
    public static ConfigSpec.IntValue POWER_BOLT_RANGE;
    public static ConfigSpec.IntValue POWER_BOLT_COOLDOWN;
    public static ConfigSpec.IntValue SPACE_STEP_RANGE;
    public static ConfigSpec.IntValue SPACE_STEP_COOLDOWN;
    public static ConfigSpec.IntValue SPACE_REALM_COOLDOWN;
    public static ConfigSpec.BooleanValue SPACE_REACHES_ASGARD;
    public static ConfigSpec.IntValue SPACE_NETHER_SEARCH;
    public static ConfigSpec.IntValue SPACE_END_ARRIVAL_X;
    public static ConfigSpec.IntValue SPACE_ARRIVAL_RESISTANCE_TICKS;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("stones");
        UNIQUENESS = builder.comment("How many of each site stone (Reality, Time, Power, Soul) there are: PLAYER",
                "(each player may take each once), WORLD (one of each in the world) or UNLIMITED.")
                .defineEnum("uniqueness", Uniqueness.PLAYER);
        GRACE_SECONDS = builder.comment("Seconds a stone may stay in the inventory of one who may not hold it before",
                "it burns him and jumps out.")
                .defineInRange("graceSeconds", 10, 3, 60);
        HAND_GRACE_FACTOR = builder.comment("How many times faster the grace runs while the stone is held in a hand.")
                .defineInRange("handGraceFactor", 3, 1, 10);
        REJECT_DAMAGE = builder.comment("Damage of a stone's rejection (never below one heart of health).")
                .defineInRange("rejectDamage", 2.0, 0.0, 20.0);
        CUSTOM_BEARERS = builder.comment("Whether a custom hero with a god's row (the trait STONE_BEARER) may hold",
                "the stones.")
                .define("customBearers", true);
        VISION_HOLDS_MIND = builder.comment("Whether Vision may hold the Mind Stone (it is his own).")
                .define("visionHoldsMind", true);
        SORCERERS_HOLD_TIME = builder.comment("Whether a sorcerer may hold the Time Stone.")
                .define("sorcerersHoldTime", true);
        RETURN_LOST = builder.comment("Whether a lost site stone (in the void, burned, destroyed) returns to its",
                "sites.")
                .define("returnLost", true);
        builder.pop();
        builder.push("gauntlet");
        GAUNTLET_CRAFTABLE = builder.comment("Whether the empty Infinity Gauntlet can be crafted.")
                .define("craftable", true);
        SNAP_ENABLED = builder.comment("Whether the Snap works (all six stones in a Gauntlet): half of the hostile",
                "creatures near turn to dust.")
                .define("snapEnabled", true);
        SNAP_RADIUS = builder.comment("Radius (blocks) of the Snap.")
                .defineInRange("snapRadius", 64, 8, 256);
        SNAP_FRACTION = builder.comment("Fraction of the hostile creatures the Snap turns to dust.")
                .defineInRange("snapFraction", 0.5, 0.0, 1.0);
        SNAP_COOLDOWN_TICKS = builder.comment("Ticks before a Gauntlet can snap again.")
                .defineInRange("snapCooldownTicks", 24000, 0, 720000);
        SNAP_HOLD_TICKS = builder.comment("Ticks the use is held (sneaking) for the Snap.")
                .defineInRange("snapHoldTicks", 60, 10, 400);
        SNAP_IN_ASGARD = builder.comment("Whether the Snap works in Asgard.")
                .define("snapInAsgard", false);
        SNAP_COST_HEALTH = builder.comment("Health a wielder other than Thanos is left with after the Snap.")
                .defineInRange("snapCostHealth", 2.0, 1.0, 20.0);
        MIND_FOR_THANOS = builder.comment("Whether the hero Thanos gets the Mind Stone's powers from a Gauntlet he",
                "holds with it.")
                .define("mindForThanos", true);
        builder.pop();
        builder.push("reality_stone");
        REALITY_RADIUS = builder.comment("Radius (blocks) in which projectiles heading for the holder turn to",
                "bubbles.")
                .defineInRange("radius", 4.0, 1.0, 16.0);
        REALITY_EXHAUSTION_PER_BUBBLE = builder.comment("Food exhaustion each bubble costs.")
                .defineInRange("exhaustionPerBubble", 0.5, 0.0, 10.0);
        REALITY_BOSS_RATE = builder.comment("In a boss arena, at most one bubble per this many ticks.")
                .defineInRange("bossRate", 10, 1, 200);
        REALITY_BOSS_COST = builder.comment("How many times the hunger a bubble costs in a boss arena.")
                .defineInRange("bossCost", 3.0, 1.0, 20.0);
        REALITY_TRANSMUTE_RANGE = builder.comment("Range (blocks) of the Transmute.")
                .defineInRange("transmuteRange", 20, 4, 64);
        REALITY_TRANSMUTE_TICKS = builder.comment("Ticks a creature stays stone after the Transmute.")
                .defineInRange("transmuteTicks", 60, 10, 600);
        REALITY_TRANSMUTE_COOLDOWN = builder.comment("Cooldown (ticks) of the Transmute.")
                .defineInRange("transmuteCooldown", 240, 0, 72000);
        REALITY_ILLUSION_TICKS = builder.comment("Ticks the Illusion lasts.")
                .defineInRange("illusionTicks", 120, 20, 1200);
        REALITY_ILLUSION_COOLDOWN = builder.comment("Cooldown (ticks) of the Illusion.")
                .defineInRange("illusionCooldown", 400, 0, 72000);
        builder.pop();
        builder.push("mind_stone");
        MIND_BEAM_DAMAGE = builder.comment("Damage of each hit of the Mind Beam (Vision's mindStoneBeamDamage by",
                "default).")
                .defineInRange("beamDamage", 4.0, 0.0, 100.0);
        MIND_BEAM_RANGE = builder.comment("Range (blocks) of the Mind Beam.")
                .defineInRange("beamRange", 32, 4, 128);
        MIND_BEAM_MAX_TICKS = builder.comment("Longest (ticks) the Mind Beam can be held.")
                .defineInRange("beamMaxTicks", 120, 10, 1200);
        MIND_BEAM_COOLDOWN = builder.comment("Cooldown (ticks) of the Mind Beam after it was held to the end.")
                .defineInRange("beamCooldown", 60, 0, 72000);
        MIND_CONFUSE_EVERY = builder.comment("The Mind Beam confuses the same target at most once per this many",
                "ticks.")
                .defineInRange("confuseEvery", 40, 1, 400);
        MIND_COMMAND_RANGE = builder.comment("Range (blocks) of the Mind Command.")
                .defineInRange("commandRange", 16, 4, 64);
        MIND_COMMAND_TICKS = builder.comment("Ticks a commanded creature fights for the holder.")
                .defineInRange("commandTicks", 400, 20, 6000);
        MIND_COMMAND_COOLDOWN = builder.comment("Cooldown (ticks) of the Mind Command.")
                .defineInRange("commandCooldown", 600, 0, 72000);
        MIND_EXHAUSTION_PER_TICK = builder.comment("Food exhaustion per tick of the Mind Beam.")
                .defineInRange("exhaustionPerTick", 0.03, 0.0, 1.0);
        builder.pop();
        builder.push("time_stone");
        TIME_SLOW_RADIUS = builder.comment("Radius (blocks) of the Slow Time zone round the holder.")
                .defineInRange("slowRadius", 12, 2, 64);
        TIME_SLOW_FACTOR = builder.comment("How many times slower time runs in the zone.")
                .defineInRange("slowFactor", 3, 2, 20);
        TIME_SLOW_MAX_TICKS = builder.comment("Longest (ticks) Slow Time stays on.")
                .defineInRange("slowMaxTicks", 200, 20, 2400);
        TIME_SLOW_COOLDOWN = builder.comment("Cooldown (ticks) of Slow Time.")
                .defineInRange("slowCooldown", 200, 0, 72000);
        TIME_BOSS_FACTOR_HALF = builder.comment("Whether bosses are slowed at half the factor.")
                .define("bossFactorHalf", true);
        TIME_REWIND_SECONDS = builder.comment("How far back (seconds) the Rewind goes.")
                .defineInRange("rewindSeconds", 5, 2, 30);
        TIME_REWIND_COOLDOWN = builder.comment("Cooldown (ticks) of the Rewind.")
                .defineInRange("rewindCooldown", 600, 0, 72000);
        TIME_CROP_MAGIC = builder.comment("Whether the Time Stone grows (or, sneaking, un-grows) a crop, a sapling or",
                "a baby animal.")
                .define("cropMagic", true);
        TIME_CROP_COOLDOWN = builder.comment("Cooldown (ticks) of the crop magic.")
                .defineInRange("cropCooldown", 10, 0, 72000);
        builder.pop();
        builder.push("power_stone");
        POWER_BEAM_DAMAGE = builder.comment("Damage per hit (every 10 ticks) of the Purple Beam.")
                .defineInRange("beamDamage", 4.0, 0.0, 100.0);
        POWER_BEAM_RANGE = builder.comment("Range (blocks) of the Purple Beam.")
                .defineInRange("beamRange", 32, 4, 128);
        POWER_EXPLODE_EVERY = builder.comment("Ticks between two explosions at the end of the Purple Beam.")
                .defineInRange("explodeEvery", 10, 1, 200);
        POWER_EXPLOSION_DAMAGE = builder.comment("Damage of each explosion at the end of the Purple Beam.")
                .defineInRange("explosionDamage", 6.0, 0.0, 100.0);
        POWER_EXPLOSION_RADIUS = builder.comment("Radius (blocks) of each explosion.")
                .defineInRange("explosionRadius", 2.5, 0.5, 8.0);
        POWER_TERRAIN_DAMAGE = builder.comment("Whether the explosions break blocks (also needs the gamerule",
                "mobGriefing, never in protected places).")
                .define("terrainDamage", false);
        POWER_CRATER_RADIUS = builder.comment("Radius (blocks) of a crater when terrainDamage is on.")
                .defineInRange("craterRadius", 1.5, 0.5, 4.0);
        POWER_BEAM_MAX_TICKS = builder.comment("Longest (ticks) the Purple Beam can be held before it overheats.")
                .defineInRange("beamMaxTicks", 60, 10, 600);
        POWER_OVERHEAT_COOLDOWN = builder.comment("Cooldown (ticks) after the Purple Beam overheats.")
                .defineInRange("overheatCooldown", 140, 0, 72000);
        POWER_RELEASE_COOLDOWN = builder.comment("Cooldown (ticks) after the Purple Beam is let go of early.")
                .defineInRange("releaseCooldown", 60, 0, 72000);
        POWER_BOLT_DAMAGE = builder.comment("Damage of the Power Bolt.")
                .defineInRange("boltDamage", 12.0, 0.0, 100.0);
        POWER_BOLT_SPLASH = builder.comment("Splash damage of the Power Bolt.")
                .defineInRange("boltSplash", 6.0, 0.0, 100.0);
        POWER_BOLT_RANGE = builder.comment("Range (blocks) of the Power Bolt.")
                .defineInRange("boltRange", 28, 4, 128);
        POWER_BOLT_COOLDOWN = builder.comment("Cooldown (ticks) of the Power Bolt.")
                .defineInRange("boltCooldown", 70, 0, 72000);
        builder.pop();
        builder.push("space_stone");
        SPACE_STEP_RANGE = builder.comment("Range (blocks) of the Portal Step.")
                .defineInRange("stepRange", 64, 4, 256);
        SPACE_STEP_COOLDOWN = builder.comment("Cooldown (ticks) of the Portal Step.")
                .defineInRange("stepCooldown", 60, 0, 72000);
        SPACE_REALM_COOLDOWN = builder.comment("Cooldown (ticks) of the Realm Picker.")
                .defineInRange("realmCooldown", 600, 0, 72000);
        SPACE_REACHES_ASGARD = builder.comment("Whether the Space Stone reaches Asgard (the one exception to \"only",
                "the Bifrost\").")
                .define("reachesAsgard", true);
        SPACE_NETHER_SEARCH = builder.comment("Radius (blocks) searched for safe ground on arriving in the Nether.")
                .defineInRange("netherSearch", 32, 4, 128);
        SPACE_END_ARRIVAL_X = builder.comment("X of the arrival on the End's main island (Z 0).")
                .defineInRange("endArrivalX", 100, 0, 1000);
        SPACE_ARRIVAL_RESISTANCE_TICKS = builder.comment("Ticks of Resistance V on arriving in a realm.")
                .defineInRange("arrivalResistanceTicks", 40, 0, 600);
        builder.pop();
    }

    private StonesConfig() {
    }
}
