package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The numbers of Cosmic Spider-Man (3.18): Spider-Man filled with the Enigma Force. TOML section {@code [spider_man.cosmic]},
 * defined from {@link SpiderConfig#define}. Every option is read when it is used, so an edit of the file changes it live.
 */
public final class CosmicSpiderConfig {
    /** The TOML sub-section of these options (inside {@code [spider_man]}). */
    public static final String SECTION = "cosmic";

    public static ConfigSpec.BooleanValue ENABLED;
    // ---- the meter ----
    public static ConfigSpec.DoubleValue ENERGY_MAX;
    public static ConfigSpec.DoubleValue DRAIN_PER_SECOND;
    public static ConfigSpec.DoubleValue FLIGHT_DRAIN_PER_SECOND;
    public static ConfigSpec.DoubleValue REFILL_PER_SECOND;
    // ---- the body ----
    public static ConfigSpec.DoubleValue STRENGTH_FACTOR;
    public static ConfigSpec.DoubleValue ARMOR;
    public static ConfigSpec.DoubleValue TOUGHNESS;
    public static ConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ConfigSpec.IntValue REGEN_INTERVAL_TICKS;
    public static ConfigSpec.DoubleValue REGEN_AMOUNT;
    public static ConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ConfigSpec.DoubleValue DARK_MAGIC_FACTOR;
    public static ConfigSpec.DoubleValue SENSE_FACTOR;
    // ---- the blasts ----
    public static ConfigSpec.IntValue BLAST_RANGE;
    public static ConfigSpec.DoubleValue BLAST_DAMAGE;
    public static ConfigSpec.IntValue BLAST_INTERVAL;
    public static ConfigSpec.DoubleValue BLAST_ENERGY_PER_SECOND;
    public static ConfigSpec.IntValue EYE_BEAM_RANGE;
    public static ConfigSpec.DoubleValue EYE_BEAM_DAMAGE;
    public static ConfigSpec.IntValue EYE_BEAM_INTERVAL;
    public static ConfigSpec.DoubleValue EYE_BEAM_ENERGY_PER_SECOND;
    // ---- matter restructuring and the portals ----
    public static ConfigSpec.DoubleValue MATTER_FACTOR;
    public static ConfigSpec.DoubleValue PORTAL_ENERGY;
    public static ConfigSpec.DoubleValue WORLD_PORTAL_ENERGY;
    public static ConfigSpec.IntValue PORTAL_COOLDOWN;

    /** Defines the options (called inside the {@code [spider_man]} section). */
    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether Spider-Man can become Cosmic Spider-Man (Sneak + N when the cosmic meter is full).")
                .define("enabled", true);

        ENERGY_MAX = builder.comment("The cosmic meter when full. He can become cosmic only when it is full.")
                .defineInRange("energyMax", 170.0, 1.0, 10000.0);
        DRAIN_PER_SECOND = builder.comment("Cosmic energy spent every second while he is cosmic (it never recharges while he is).",
                        "Iron Man's Mark 3 flies 100 / 0.6 = about 167 seconds; 170 / (0.75 + 0.25) = 170 seconds flying, 227 standing.")
                .defineInRange("drainPerSecond", 0.75, 0.0, 100.0);
        FLIGHT_DRAIN_PER_SECOND = builder.comment("Extra cosmic energy spent every second while he flies.")
                .defineInRange("flightDrainPerSecond", 0.25, 0.0, 100.0);
        REFILL_PER_SECOND = builder.comment("Cosmic energy that comes back every second while he is plain Spider-Man.")
                .defineInRange("refillPerSecond", 1.0, 0.0, 100.0);

        STRENGTH_FACTOR = builder.comment("His fists, cosmic, hit this many times as hard as the Hulk's at 100% rage",
                        "((1 + hulk.attackBonus) x (1 + hulk.rageDamageBonus): 10.4 by default, so 31.2).")
                .defineInRange("strengthFactor", 3.0, 0.0, 20.0);
        ARMOR = builder.comment("His armour while cosmic (a full netherite set: 20).")
                .defineInRange("armor", 20.0, 0.0, 30.0);
        TOUGHNESS = builder.comment("His armour toughness while cosmic (a full netherite set: 12).")
                .defineInRange("toughness", 12.0, 0.0, 20.0);
        KNOCKBACK_RESISTANCE = builder.comment("His knockback resistance while cosmic (a full netherite set: 0.4).")
                .defineInRange("knockbackResistance", 0.4, 0.0, 1.0);
        REGEN_INTERVAL_TICKS = builder.comment("While cosmic he heals regenAmount every this many ticks on top of a player's own healing",
                        "(a fed player heals 1 every 80 ticks: twice as fast).")
                .defineInRange("regenIntervalTicks", 80, 1, 1200);
        REGEN_AMOUNT = builder.comment("Health healed every regenIntervalTicks while cosmic.")
                .defineInRange("regenAmount", 1.0, 0.0, 20.0);
        FLIGHT_SPEED = builder.comment("His flight speed while cosmic (Thor: 0.11).")
                .defineInRange("flightSpeed", 0.16, 0.01, 1.0);
        DARK_MAGIC_FACTOR = builder.comment("Pure life force: dark magic (the Darkhold, Agatha, Hela's blades...) hurts him this many times as much while cosmic.")
                .defineInRange("darkMagicFactor", 1.5, 1.0, 10.0);
        SENSE_FACTOR = builder.comment("How much wider (and faster) his spider-sense is while cosmic.")
                .defineInRange("senseFactor", 2.5, 1.0, 5.0);

        BLAST_RANGE = builder.comment("Range of the Cosmic Blast from his hand (R held).")
                .defineInRange("blastRange", 32, 4, 128);
        BLAST_DAMAGE = builder.comment("Damage of each hit of the Cosmic Blast.")
                .defineInRange("blastDamage", 6.0, 0.0, 200.0);
        BLAST_INTERVAL = builder.comment("Ticks between two hits of the Cosmic Blast.")
                .defineInRange("blastInterval", 5, 1, 40);
        BLAST_ENERGY_PER_SECOND = builder.comment("Cosmic energy the Cosmic Blast spends every second it burns.")
                .defineInRange("blastEnergyPerSecond", 3.0, 0.0, 100.0);
        EYE_BEAM_RANGE = builder.comment("Range of the Cosmic Eyes (Sneak + R held).")
                .defineInRange("eyeBeamRange", 24, 4, 128);
        EYE_BEAM_DAMAGE = builder.comment("Damage of each hit of the Cosmic Eyes.")
                .defineInRange("eyeBeamDamage", 8.0, 0.0, 200.0);
        EYE_BEAM_INTERVAL = builder.comment("Ticks between two hits of the Cosmic Eyes.")
                .defineInRange("eyeBeamInterval", 5, 1, 40);
        EYE_BEAM_ENERGY_PER_SECOND = builder.comment("Cosmic energy the Cosmic Eyes spend every second they burn.")
                .defineInRange("eyeBeamEnergyPerSecond", 4.0, 0.0, 100.0);

        MATTER_FACTOR = builder.comment("Matter Restructuring (Y) is Billy's Build Magic at this share of his reach, sizes, brush and counts.")
                .defineInRange("matterFactor", 0.667, 0.1, 1.0);
        PORTAL_ENERGY = builder.comment("Cosmic energy a portal to a saved place costs (B).")
                .defineInRange("portalEnergy", 15.0, 0.0, 1000.0);
        WORLD_PORTAL_ENERGY = builder.comment("Cosmic energy a portal to another world costs (Sneak + B, or a world on the portal screen).")
                .defineInRange("worldPortalEnergy", 25.0, 0.0, 1000.0);
        PORTAL_COOLDOWN = builder.comment("Ticks before he can open another cosmic portal.")
                .defineInRange("portalCooldown", 100, 0, 12000);
        builder.pop();
    }

    private CosmicSpiderConfig() {
    }
}
