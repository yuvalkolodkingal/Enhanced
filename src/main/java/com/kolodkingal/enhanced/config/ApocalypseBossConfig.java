package com.kolodkingal.enhanced.config;


import com.kolodkingal.enhanced.config.core.ConfigSpec;
import java.util.List;

/**
 * The balance numbers of the Apocalypse boss fight (the rules all bosses share are in {@link BossConfig}, the rule of
 * his shield in {@link ApocalypseShieldConfig}). Its options are defined in {@link #define}, which {@code
 * EnhancedConfig} calls while it builds the server config (TOML section {@code [apocalypse_boss]}). Every damage is
 * for Normal difficulty and scales like any mob's.
 */
public final class ApocalypseBossConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "apocalypse_boss";

    // 3.2: the master factor
    public static ConfigSpec.DoubleValue POWER_FACTOR;
    // the ritual
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.ConfigValue<List<? extends String>> RITUAL_DIMENSIONS;
    public static ConfigSpec.BooleanValue RITUAL_NEEDS_SAND;
    public static ConfigSpec.BooleanValue RITUAL_DAYLIGHT_ONLY;
    public static ConfigSpec.BooleanValue REQUIRE_BREAKER;
    public static ConfigSpec.IntValue BREAKER_RANGE;
    // the arena
    public static ConfigSpec.IntValue ARENA_RADIUS;
    public static ConfigSpec.IntValue ARENA_HEIGHT;
    public static ConfigSpec.DoubleValue FIELD_DAMAGE;
    // his body
    public static ConfigSpec.DoubleValue BODY_HEALTH;
    public static ConfigSpec.DoubleValue HEALTH_PER_EXTRA_PLAYER;
    public static ConfigSpec.DoubleValue ARMOR;
    public static ConfigSpec.DoubleValue ARMOR_TOUGHNESS;
    public static ConfigSpec.DoubleValue MAX_HIT_FRACTION;
    public static ConfigSpec.DoubleValue EXPLOSION_DAMAGE_FACTOR;
    // his shield and its windows
    public static ConfigSpec.DoubleValue SHIELD_STRENGTH;
    public static ConfigSpec.DoubleValue SHIELD_GROWTH;
    public static ConfigSpec.DoubleValue SHIELD_PER_EXTRA_PLAYER;
    public static ConfigSpec.IntValue WINDOW_TICKS;
    public static ConfigSpec.IntValue STAGGER_TICKS;
    public static ConfigSpec.DoubleValue WINDOW_DAMAGE_FACTOR;
    public static ConfigSpec.DoubleValue REGEN_PER_SECOND;
    public static ConfigSpec.DoubleValue TRUE_FORM_REGEN_PER_SECOND;
    // the escape and the end
    public static ConfigSpec.IntValue ESCAPE_CHANNEL_TICKS;
    public static ConfigSpec.DoubleValue ESCAPE_INTERRUPT_FRACTION;
    public static ConfigSpec.DoubleValue ESCAPE_HEAL_TO;
    public static ConfigSpec.IntValue PHOENIX_FINISH_SECONDS;
    // his attacks
    public static ConfigSpec.DoubleValue LANCE_DAMAGE;
    public static ConfigSpec.IntValue LANCE_COUNT;
    public static ConfigSpec.DoubleValue WAVE_DAMAGE;
    public static ConfigSpec.DoubleValue SPIKE_DAMAGE;
    public static ConfigSpec.DoubleValue BOULDER_DAMAGE;
    public static ConfigSpec.DoubleValue BACKHAND_DAMAGE;
    public static ConfigSpec.DoubleValue PORTAL_SHOCK_DAMAGE;
    public static ConfigSpec.DoubleValue STORM_DAMAGE;
    public static ConfigSpec.IntValue TORNADO_TICKS;
    public static ConfigSpec.DoubleValue RING_DAMAGE;
    public static ConfigSpec.IntValue ENTOMB_TICKS;
    public static ConfigSpec.DoubleValue ENTOMB_DAMAGE_PER_SECOND;
    public static ConfigSpec.DoubleValue COCOON_HEALTH;
    public static ConfigSpec.IntValue MIND_CRUSH_RANGE;
    public static ConfigSpec.IntValue MIND_CRUSH_NAUSEA_TICKS;
    // the Horsemen
    public static ConfigSpec.BooleanValue HORSEMEN;
    public static ConfigSpec.IntValue HORSEMEN_MAX;
    public static ConfigSpec.DoubleValue HORSEMAN_HEALTH_FACTOR;
    public static ConfigSpec.DoubleValue HORSEMAN_DAMAGE_BONUS;
    // the heroes against him
    public static ConfigSpec.IntValue ADAPT_TICKS;
    public static ConfigSpec.IntValue ADAPTED_TICKS;
    public static ConfigSpec.IntValue MAGNETO_STRIP_EXTEND_TICKS;
    public static ConfigSpec.IntValue NO_BREAKER_SECONDS;
    public static ConfigSpec.BooleanValue DROPS_PHOENIX_EMBER;
    public static ConfigSpec.IntValue HOLD_TICKS;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        POWER_FACTOR = builder.comment("3.2: how strong the boss fights, one master knob (1.0 = the 3.1 numbers). It multiplies every damage he and his",
                        "Horsemen deal, the number of his lances, Horsemen and tornadoes, and it slows his attacks: the pause between two",
                        "attacks and every attack's cooldown last 1/powerFactor times as long. His health, his armour and his shield are not",
                        "touched by it (they have their own options).")
                .defineInRange("powerFactor", 0.65, 0.1, 1.0);
        ENABLED = builder.comment("If false, the Capstone's ritual is refused.")
                .define("enabled", true);
        RITUAL_DIMENSIONS = builder.comment("The dimensions where he can be woken.")
                .defineListAllowEmpty("ritualDimensions", List.of("minecraft:overworld"), () -> "minecraft:overworld",
                        element -> element instanceof String);
        RITUAL_NEEDS_SAND = builder.comment("The ritual block must be sand or sandstone (the block tag enhanced:ritual_ground).")
                .define("ritualNeedsSand", true);
        RITUAL_DAYLIGHT_ONLY = builder.comment("It must be day, under the open sky, at the start of the ritual.")
                .define("ritualDaylightOnly", true);
        REQUIRE_BREAKER = builder.comment("The ritual is refused unless a hero who can break his shield (one of apocalypse_shield.mindPowerHeroes)",
                        "stands within breakerRange: without one nobody could win.")
                .define("requireBreaker", true);
        BREAKER_RANGE = builder.comment("Blocks.")
                .defineInRange("breakerRange", 64, 8, 256);

        ARENA_RADIUS = builder.comment("Radius of the circle he fights in (the sand wall stands 4 blocks further out).")
                .defineInRange("arenaRadius", 24, 12, 48);
        ARENA_HEIGHT = builder.comment("How high the sand wall reaches (nobody flies over it).")
                .defineInRange("arenaHeight", 40, 16, 100);
        FIELD_DAMAGE = builder.comment("Damage (never lethal) a player takes touching the sand wall, once a second.")
                .defineInRange("fieldDamage", 1.0, 0.0, 10.0);

        BODY_HEALTH = builder.comment("His health for one fighter.")
                .defineInRange("bodyHealth", 900.0, 100.0, 50000.0);
        HEALTH_PER_EXTRA_PLAYER = builder.comment("Extra health per additional fighter (a fraction of bodyHealth).")
                .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 3.0);
        ARMOR = builder.comment("His armour in the armoured form.")
                .defineInRange("armor", 10.0, 0.0, 30.0);
        ARMOR_TOUGHNESS = builder.comment("His armour toughness in the armoured form.")
                .defineInRange("armorToughness", 6.0, 0.0, 20.0);
        MAX_HIT_FRACTION = builder.comment("The most one hit can take while he is vulnerable, as a fraction of his maximum health (before armour).")
                .defineInRange("maxHitFraction", 0.12, 0.01, 1.0);
        EXPLOSION_DAMAGE_FACTOR = builder.comment("Explosion damage to him is multiplied by this.")
                .defineInRange("explosionDamageFactor", 0.5, 0.0, 1.0);

        SHIELD_STRENGTH = builder.comment("Strength of shield 1 for one fighter.")
                .defineInRange("shieldStrength", 100.0, 10.0, 10000.0);
        SHIELD_GROWTH = builder.comment("Each later shield is this many times stronger.")
                .defineInRange("shieldGrowth", 1.2, 1.0, 3.0);
        SHIELD_PER_EXTRA_PLAYER = builder.comment("Extra shield strength per additional fighter (a fraction).")
                .defineInRange("shieldPerExtraPlayer", 0.3, 0.0, 3.0);
        WINDOW_TICKS = builder.comment("How long a broken shield leaves him vulnerable (240 = 12 seconds).")
                .defineInRange("windowTicks", 240, 40, 2400);
        STAGGER_TICKS = builder.comment("How long he is staggered and attacks nothing at the start of a window.")
                .defineInRange("staggerTicks", 80, 0, 400);
        WINDOW_DAMAGE_FACTOR = builder.comment("Damage he takes in a window is multiplied by this.")
                .defineInRange("windowDamageFactor", 1.25, 1.0, 5.0);
        REGEN_PER_SECOND = builder.comment("His healing while a shield is up (never above the phase's ceiling). 0: his health never heals (his",
                        "shield still knits).")
                .defineInRange("regenPerSecond", 0.0, 0.0, 100.0);
        TRUE_FORM_REGEN_PER_SECOND = builder.comment("His healing in the true form after 80 ticks without being hurt. 0: his health never heals.")
                .defineInRange("trueFormRegenPerSecond", 0.0, 0.0, 100.0);

        ESCAPE_CHANNEL_TICKS = builder.comment("How long his escape portal takes to open.")
                .defineInRange("escapeChannelTicks", 100, 20, 600);
        ESCAPE_INTERRUPT_FRACTION = builder.comment("Damage (a fraction of his full health) that interrupts the escape; any lightning damage also does.")
                .defineInRange("escapeInterruptFraction", 0.04, 0.005, 0.5);
        ESCAPE_HEAL_TO = builder.comment("The fraction of his health he returns with if the escape was not interrupted.")
                .defineInRange("escapeHealTo", 0.40, 0.05, 0.9);
        PHOENIX_FINISH_SECONDS = builder.comment("If a Phoenix dealt Burst or slam damage in this many seconds before the end, he dies in her fire.")
                .defineInRange("phoenixFinishSeconds", 15, 0, 120);

        LANCE_DAMAGE = builder.comment("One Sand Lance.")
                .defineInRange("lanceDamage", 6.0, 0.0, 100.0);
        LANCE_COUNT = builder.comment("Lances in a fan (scaled by powerFactor, rounded up: 5 lances are 4 at the default 0.65).")
                .defineInRange("lanceCount", 5, 1, 9);
        WAVE_DAMAGE = builder.comment("The Sand Wave.")
                .defineInRange("waveDamage", 10.0, 0.0, 100.0);
        SPIKE_DAMAGE = builder.comment("A Sand Spike.")
                .defineInRange("spikeDamage", 9.0, 0.0, 100.0);
        BOULDER_DAMAGE = builder.comment("A Sand Boulder.")
                .defineInRange("boulderDamage", 9.0, 0.0, 100.0);
        BACKHAND_DAMAGE = builder.comment("The Backhand.")
                .defineInRange("backhandDamage", 12.0, 0.0, 100.0);
        PORTAL_SHOCK_DAMAGE = builder.comment("The shockwave where he arrives from a portal.")
                .defineInRange("portalShockDamage", 6.0, 0.0, 100.0);
        STORM_DAMAGE = builder.comment("A Tornado's damage, every 10 ticks. (The number of tornadoes, one in phase 2 and three in phase 3, is scaled by powerFactor too.)")
                .defineInRange("stormDamage", 2.0, 0.0, 100.0);
        TORNADO_TICKS = builder.comment("How long a Tornado lasts.")
                .defineInRange("tornadoTicks", 160, 20, 1200);
        RING_DAMAGE = builder.comment("A ring of the Cairo Collapse.")
                .defineInRange("ringDamage", 10.0, 0.0, 100.0);
        ENTOMB_TICKS = builder.comment("How long the Entomb holds.")
                .defineInRange("entombTicks", 80, 10, 1200);
        ENTOMB_DAMAGE_PER_SECOND = builder.comment("Damage while entombed (never below 2 hearts).")
                .defineInRange("entombDamagePerSecond", 1.5, 0.0, 20.0);
        COCOON_HEALTH = builder.comment("Health of the boss's cocoon (a friend breaks it by hitting it).")
                .defineInRange("cocoonHealth", 20.0, 1.0, 200.0);
        MIND_CRUSH_RANGE = builder.comment("Blocks.")
                .defineInRange("mindCrushRange", 28, 8, 64);
        MIND_CRUSH_NAUSEA_TICKS = builder.comment("Nausea of the Mind Crush (Slowness for 3 seconds and Blindness for 2 go with it).")
                .defineInRange("mindCrushNauseaTicks", 80, 0, 1200);

        HORSEMEN = builder.comment("He calls the Four Horsemen at the first transition.")
                .define("horsemen", true);
        HORSEMEN_MAX = builder.comment("The most Horsemen (min(max, 1 + fighters)); max is scaled by powerFactor, rounded up (4 is 3 at the default 0.65, and then Death, the",
                        "fourth, takes the place of the third in a fight big enough for four).")
                .defineInRange("horsemenMax", 4, 0, 8);
        HORSEMAN_HEALTH_FACTOR = builder.comment("Husk health multiplier.")
                .defineInRange("horsemanHealthFactor", 1.5, 1.0, 10.0);
        HORSEMAN_DAMAGE_BONUS = builder.comment("Added attack damage.")
                .defineInRange("horsemanDamageBonus", 3.0, 0.0, 50.0);

        ADAPT_TICKS = builder.comment("Quicksilver Time slows him for this long, then he adapts.")
                .defineInRange("adaptTicks", 80, 0, 1200);
        ADAPTED_TICKS = builder.comment("How long he then ignores it.")
                .defineInRange("adaptedTicks", 600, 0, 12000);
        MAGNETO_STRIP_EXTEND_TICKS = builder.comment("Magneto's Armor and Weapon Strip on him in a window cuts his armour and extends the window by this much (once per window).")
                .defineInRange("magnetoStripExtendTicks", 100, 0, 1200);
        NO_BREAKER_SECONDS = builder.comment("He withdraws if no one who can break his shield is in the arena while it is up for this long.")
                .defineInRange("noBreakerSeconds", 60, 10, 600);
        DROPS_PHOENIX_EMBER = builder.comment("Also give a Phoenix Ember (if it exists) to a rewarded fighter with no power.")
                .define("dropsPhoenixEmber", false);
        HOLD_TICKS = builder.comment("How long telekinesis can hold Apocalypse before he breaks free (before the hero's factor,",
                        "boss.billyHoldFactor or boss.magnetoHoldFactor).")
                .defineInRange("holdTicks", 60, 10, 600);
        builder.pop();
    }

    /** The master factor {@code powerFactor}. */
    public static double power() {
        return POWER_FACTOR.get();
    }

    /** A damage he or a Horseman deals, after the master factor. */
    public static float dealt(double damage) {
        return (float) (damage * power());
    }

    /** A number of lances, Horsemen or tornadoes, after the master factor: rounded up, never below one (zero stays zero). */
    public static int count(int count) {
        return count <= 0 ? 0 : Math.max(1, (int) Math.ceil(count * power() - 1.0E-9));
    }

    /** A pause or cooldown of his attacks in ticks, after the master factor: the lower the factor, the longer. */
    public static int slower(int ticks) {
        return (int) Math.round(ticks / power());
    }

    private ApocalypseBossConfig() {
    }
}
