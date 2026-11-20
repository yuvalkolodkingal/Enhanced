package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Jim Hammond (3.17), the android first Human Torch: his Fire Beam (a continuous beam while R is held, fed by his solar
 * charge), his fire immunity, his body (almost as durable as Vision's, healing five times as fast), his fireballs, the
 * Nova Flame (hold to charge, release to fire: stronger the longer it charged; charged enough, it turns the ground to magma;
 * then a long cooldown) and his burning body while he flies. TOML section {@code [jim]};
 * {@code EnhancedConfig} calls {@link #define}; read when used.
 */
public final class JimConfig {
    public static ConfigSpec.DoubleValue HEALTH_BONUS;
    public static ConfigSpec.DoubleValue ARMOR_BONUS;
    public static ConfigSpec.DoubleValue REGEN_MULTIPLIER;
    public static ConfigSpec.IntValue ATTACKER_BURN_SECONDS;
    public static ConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ConfigSpec.BooleanValue FLAMES_WHILE_FLYING;
    public static ConfigSpec.BooleanValue SOURCE_CODE_REPLACES_HERO;
    public static ConfigSpec.BooleanValue SOLAR_CHARGE;
    public static ConfigSpec.IntValue SOLAR_CAPACITY;
    public static ConfigSpec.IntValue SOLAR_FILL_SUN;
    public static ConfigSpec.IntValue SOLAR_FILL_SHADE;

    public static ConfigSpec.IntValue BEAM_COOLDOWN;
    public static ConfigSpec.DoubleValue BEAM_DAMAGE;
    public static ConfigSpec.IntValue BEAM_RANGE;
    public static ConfigSpec.DoubleValue BEAM_WIDTH;
    public static ConfigSpec.IntValue BEAM_BURN_SECONDS;
    public static ConfigSpec.BooleanValue BEAM_FIRE;
    public static ConfigSpec.IntValue BEAM_COST_PER_SECOND;
    public static ConfigSpec.IntValue BEAM_MIN_CHARGE;

    public static ConfigSpec.DoubleValue FIREBALL_DAMAGE;
    public static ConfigSpec.DoubleValue FIREBALL_SPEED;
    public static ConfigSpec.IntValue FIREBALL_COOLDOWN;
    public static ConfigSpec.IntValue FIREBALL_BURN_SECONDS;
    public static ConfigSpec.IntValue FIREBALL_RANGE;
    public static ConfigSpec.BooleanValue FIREBALL_FIRE;
    public static ConfigSpec.IntValue FIREBALL_COST;
    public static ConfigSpec.IntValue FIREBALL_CHARGE_TICKS;

    public static ConfigSpec.IntValue NOVA_CHARGE_TICKS;
    public static ConfigSpec.IntValue NOVA_TAP_TICKS;
    public static ConfigSpec.IntValue NOVA_DURATION_TICKS;
    public static ConfigSpec.IntValue NOVA_MIN_RANGE;
    public static ConfigSpec.IntValue NOVA_RANGE;
    public static ConfigSpec.DoubleValue NOVA_WIDTH;
    public static ConfigSpec.DoubleValue NOVA_MIN_DAMAGE;
    public static ConfigSpec.DoubleValue NOVA_DAMAGE;
    public static ConfigSpec.IntValue NOVA_BURN_SECONDS;
    public static ConfigSpec.BooleanValue NOVA_MAGMA;
    public static ConfigSpec.DoubleValue NOVA_MAGMA_MIN_CHARGE;
    public static ConfigSpec.IntValue NOVA_MAGMA_RADIUS;
    public static ConfigSpec.DoubleValue NOVA_BURN_RADIUS_MIN;
    public static ConfigSpec.DoubleValue NOVA_BURN_RADIUS_MAX;
    public static ConfigSpec.BooleanValue NOVA_EXPLOSION;
    public static ConfigSpec.IntValue NOVA_COOLDOWN;
    public static ConfigSpec.BooleanValue NOVA_FIRE;
    public static ConfigSpec.IntValue NOVA_MIN_COST;
    public static ConfigSpec.IntValue NOVA_COST;
    public static ConfigSpec.IntValue NOVA_MIN_CHARGE;

    public static void define(ConfigSpec.Builder builder) {
        builder.comment("Jim Hammond (3.17), the android first Human Torch. In survival the only way to become him is to use the",
                "Source Code item as a player who is not Vision. R: Fire Beam (a continuous beam while held); G: Flight; V: Fireball;",
                "B: Nova Flame (hold to charge, release to fire); N: the human disguise. Fire is only placed, and magma only made,",
                "where the mod's block rules allow it and never with the game rule mobGriefing off.")
                .push("jim");
        HEALTH_BONUS = builder.comment("Extra health of his android body (Vision's is 10: Jim's is a little less).")
                .defineInRange("healthBonus", 8.0, 0.0, 200.0);
        ARMOR_BONUS = builder.comment("Extra armor of his android body (Vision's is 8: Jim's is a little less).")
                .defineInRange("armorBonus", 6.0, 0.0, 30.0);
        REGEN_MULTIPLIER = builder.comment("How many times as fast as a normal player he heals (a normal player: one health in four seconds).")
                .defineInRange("regenMultiplier", 5.0, 1.0, 40.0);
        ATTACKER_BURN_SECONDS = builder.comment("While he flies in flames, whoever strikes him in melee is set alight for this many seconds",
                "(and still hurts him). 0: nobody is.")
                .defineInRange("attackerBurnSeconds", 6, 0, 60);
        FLIGHT_SPEED = builder.defineInRange("flightSpeed", 0.08, 0.01, 0.5);
        FLAMES_WHILE_FLYING = builder.comment("Whether his whole body burns while he flies (the flames, and the attackers who burn).")
                .define("flamesWhileFlying", true);
        SOURCE_CODE_REPLACES_HERO = builder.comment("Whether the Source Code item also turns a player who is already another hero into Jim.",
                "Off: only a player with no hero. (Vision's own use of it is unchanged.)")
                .define("sourceCodeReplacesHero", true);

        SOLAR_CHARGE = builder.comment("Jim Hammond charges on the sun, as Source Code Vision's Solar Jewel does: a meter that fills under the open sky by day",
                "(the Solar Jewel's own rule: none at night, underground or under a roof, a little in shade and rain) and that his attacks spend.",
                "He starts full. Flight, fire immunity, his body and the burning body cost nothing. Off: his attacks are free.")
                .define("solarCharge", true);
        SOLAR_CAPACITY = builder.comment("How much solar charge he holds (his meter's 100%).").defineInRange("solarCapacity", 1000, 100, 100000);
        SOLAR_FILL_SUN = builder.comment("Charge he gains each tick under the open sky by day (no rain).").defineInRange("solarFillSun", 5, 0, 1000);
        SOLAR_FILL_SHADE = builder.comment("Charge he gains each tick by day out of the sun (shade, indoors with daylight, rain). None by night.")
                .defineInRange("solarFillShade", 1, 0, 1000);

        BEAM_COOLDOWN = builder.comment("Ticks before the Fire Beam can be held on again after it stops.")
                .defineInRange("beamCooldown", 20, 0, 12000);
        BEAM_DAMAGE = builder.comment("Damage of each hit of the Fire Beam on a creature it crosses (it hits every 4 ticks while it is held).")
                .defineInRange("beamDamage", 4.0, 0.0, 500.0);
        BEAM_RANGE = builder.comment("Reach (blocks) of the Fire Beam.").defineInRange("beamRange", 40, 2, 256);
        BEAM_WIDTH = builder.comment("Radius (blocks) of the Fire Beam: what it touches is struck.").defineInRange("beamWidth", 0.4, 0.1, 8.0);
        BEAM_BURN_SECONDS = builder.comment("Seconds what the beam strikes burns.").defineInRange("beamBurnSeconds", 5, 0, 60);
        BEAM_FIRE = builder.comment("Whether the beam leaves fire where it strikes the ground (placed on the air, nothing is deleted).")
                .define("beamFire", true);
        BEAM_COST_PER_SECOND = builder.comment("Solar charge the Fire Beam drains each second it is held on (spent every tick). When it runs out the beam stops.")
                .defineInRange("beamCostPerSecond", 120, 0, 100000);
        BEAM_MIN_CHARGE = builder.comment("Solar charge he must have to begin the Fire Beam.").defineInRange("beamMinCharge", 40, 0, 100000);

        FIREBALL_DAMAGE = builder.comment("Damage of a fireball that hits a creature.").defineInRange("fireballDamage", 6.0, 0.0, 200.0);
        FIREBALL_SPEED = builder.comment("Blocks a fireball flies each tick.").defineInRange("fireballSpeed", 1.5, 0.2, 4.0);
        FIREBALL_COOLDOWN = builder.comment("Ticks between two fireballs.").defineInRange("fireballCooldown", 14, 0, 1200);
        FIREBALL_BURN_SECONDS = builder.comment("Seconds a creature a fireball hits burns.").defineInRange("fireballBurnSeconds", 5, 0, 60);
        FIREBALL_RANGE = builder.comment("How far (blocks) a fireball flies before it goes out.").defineInRange("fireballRange", 48, 4, 256);
        FIREBALL_FIRE = builder.comment("Whether a fireball leaves fire where it strikes the ground.").define("fireballFire", true);
        FIREBALL_COST = builder.comment("Solar charge a fireball costs (a fully charged one costs twice as much).").defineInRange("fireballCost", 30, 0, 100000);
        FIREBALL_CHARGE_TICKS = builder.comment("Ticks of holding V to charge a fireball fully: it grows bigger, hits harder and splashes wider",
                "(a quick tap throws a small one).").defineInRange("fireballChargeTicks", 30, 1, 400);

        NOVA_CHARGE_TICKS = builder.comment("Ticks he must hold B for a full charge of the Nova Flame (release sooner: a weaker beam).")
                .defineInRange("novaChargeTicks", 60, 10, 600);
        NOVA_TAP_TICKS = builder.comment("A press shorter than this many ticks is a tap: the weakest Nova.")
                .defineInRange("novaTapTicks", 6, 1, 100);
        NOVA_DURATION_TICKS = builder.comment("How long (ticks) the Nova Flame's beam is seen after it fires.")
                .defineInRange("novaDurationTicks", 14, 3, 100);
        NOVA_MIN_RANGE = builder.comment("Reach (blocks) of a tap of the Nova Flame.").defineInRange("novaMinRange", 24, 2, 256);
        NOVA_RANGE = builder.comment("Reach (blocks) of a fully charged Nova Flame.").defineInRange("novaRange", 96, 8, 256);
        NOVA_WIDTH = builder.comment("Radius (blocks) of a fully charged Nova Flame's beam: what it touches is struck.")
                .defineInRange("novaWidth", 2.2, 0.3, 12.0);
        NOVA_MIN_DAMAGE = builder.comment("Damage of a tap of the Nova Flame.").defineInRange("novaMinDamage", 6.0, 0.0, 500.0);
        NOVA_DAMAGE = builder.comment("Damage of a fully charged Nova Flame on every creature it crosses. Never to him.")
                .defineInRange("novaDamage", 24.0, 0.0, 500.0);
        NOVA_BURN_SECONDS = builder.comment("Seconds what the Nova Flame strikes burns (a full charge burns twice as long).")
                .defineInRange("novaBurnSeconds", 6, 0, 60);
        NOVA_MAGMA = builder.comment("Whether a charged Nova Flame turns the ground it strikes into magma blocks (only where the mod's block rules",
                "allow it, never with mobGriefing off, never blocks harder than 8).").define("novaMagma", true);
        NOVA_MAGMA_MIN_CHARGE = builder.comment("How charged (0 to 1) the Nova Flame must be to make magma.")
                .defineInRange("novaMagmaMinCharge", 0.8, 0.0, 1.0);
        NOVA_MAGMA_RADIUS = builder.comment("Radius (blocks) of the patch of ground that turns to magma where the Nova Flame strikes.")
                .defineInRange("novaMagmaRadius", 2, 0, 8);
        NOVA_BURN_RADIUS_MIN = builder.comment("Everything within this many blocks of a tap of the Nova Flame's beam line is set alight (creatures), and fire is",
                "placed on the open faces of the blocks along that band.").defineInRange("novaBurnRadiusMin", 2.0, 0.0, 16.0);
        NOVA_BURN_RADIUS_MAX = builder.comment("The same band for a fully charged Nova Flame (it grows with the charge).")
                .defineInRange("novaBurnRadiusMax", 4.0, 0.0, 16.0);
        NOVA_EXPLOSION = builder.comment("Whether the Nova Flame explodes where it strikes a block (as Source Code Vision's charged beam does, scaled by the",
                "charge, with the vision.chargedBeam explosion settings; never hurts him; blocks only through the usual rules).")
                .define("novaExplosion", true);
        NOVA_COOLDOWN = builder.comment("Ticks after a Nova Flame before the next (1800 = 90 seconds).")
                .defineInRange("novaCooldown", 1800, 0, 72000);
        NOVA_FIRE = builder.comment("Whether the Nova Flame sets the ground it strikes on fire (placed; never with mobGriefing off).")
                .define("novaFire", true);
        NOVA_MIN_COST = builder.comment("Solar charge a tap of the Nova Flame costs (the cost grows with the charge up to novaCost).")
                .defineInRange("novaMinCost", 200, 0, 100000);
        NOVA_COST = builder.comment("Solar charge a fully charged Nova Flame costs.").defineInRange("novaCost", 700, 0, 100000);
        NOVA_MIN_CHARGE = builder.comment("Solar charge he must have to begin gathering the Nova Flame.").defineInRange("novaMinCharge", 700, 0, 100000);
        builder.pop();
    }

    private JimConfig() {
    }
}
