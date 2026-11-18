package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Solar Jewel Vision (3.16): Vision as his Source Code (in the VisionQuest suit) runs on the Solar Jewel in his brow
 * instead of the Mind Stone. TOML section {@code [vision.solar_jewel]}, defined inside {@code [vision]} by
 * {@code EnhancedConfig}; read when used.
 */
public final class SolarJewelConfig {
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.DoubleValue SOURCE_CODE_BEAM_FACTOR;
    public static ConfigSpec.IntValue MAX_CHARGE;
    public static ConfigSpec.IntValue ABSORB_SUN;
    public static ConfigSpec.IntValue ABSORB_SHADE;
    public static ConfigSpec.DoubleValue BEAM_DAMAGE;
    public static ConfigSpec.IntValue BEAM_COST;
    public static ConfigSpec.IntValue BEAM_BURN_SECONDS;
    public static ConfigSpec.BooleanValue BEAM_FIRE;
    public static ConfigSpec.IntValue SOURCE_CODE_BURN_SECONDS;
    public static ConfigSpec.IntValue OVERLOAD_MIN_CHARGE;
    public static ConfigSpec.IntValue OVERLOAD_CHARGE_TICKS;
    public static ConfigSpec.IntValue OVERLOAD_FIRE_TICKS;
    public static ConfigSpec.DoubleValue OVERLOAD_DAMAGE;
    public static ConfigSpec.IntValue OVERLOAD_RANGE;
    public static ConfigSpec.DoubleValue OVERLOAD_WIDTH;
    public static ConfigSpec.IntValue OVERLOAD_COOLDOWN;
    public static ConfigSpec.DoubleValue EYE_BEAM_DAMAGE;
    public static ConfigSpec.IntValue EYE_BEAM_RANGE;
    public static ConfigSpec.IntValue EYE_BEAM_COST;
    public static ConfigSpec.IntValue EYE_BEAM_INTERVAL;
    public static ConfigSpec.IntValue EYE_BEAM_BURN_SECONDS;

    public static void define(ConfigSpec.Builder builder) {
        builder.comment("Solar Jewel Vision (3.16): the Source Code look (in the VisionQuest suit) runs on the Solar Jewel in his",
                "brow, which drinks in sunlight and stores it. R: the Solar Beam from the jewel; Sneak + R: Jewel Overload, all",
                "his stored light at once; N: infrared and microwave beams from his eyes. Flight, Phase and Density stay.")
                .push("solar_jewel");
        ENABLED = builder.comment("On: Source Code Vision has the Solar Jewel's powers below. Off (the default): he has Vision's own",
                "powers, only stronger (sourceCodeBeamFactor, and his tougher body).")
                .define("enabled", false);
        SOURCE_CODE_BEAM_FACTOR = builder.comment("With the Solar Jewel off: how many times as strong Source Code Vision's Mind Stone beam is.")
                .defineInRange("sourceCodeBeamFactor", 1.5, 1.0, 10.0);
        MAX_CHARGE = builder.comment("The most solar energy the jewel holds.")
                .defineInRange("maxCharge", 1000, 100, 100000);
        ABSORB_SUN = builder.comment("Energy the jewel drinks in each tick in open daylight (the sky above him, no rain).")
                .defineInRange("absorbSun", 5, 0, 1000);
        ABSORB_SHADE = builder.comment("Energy it drinks in each tick by day out of the sun (in shade, indoors, in rain). None by night:",
                "then he runs on what he stored.")
                .defineInRange("absorbShade", 1, 0, 1000);
        BEAM_DAMAGE = builder.comment("Damage per hit of the Solar Beam (two hits per second at most).")
                .defineInRange("beamDamage", 7.0, 0.0, 100.0);
        BEAM_COST = builder.comment("Energy the Solar Beam spends each tick it is held.")
                .defineInRange("beamCost", 3, 0, 1000);
        BEAM_BURN_SECONDS = builder.comment("Seconds a creature the Solar Beam strikes burns.")
                .defineInRange("beamBurnSeconds", 3, 0, 60);
        BEAM_FIRE = builder.comment("Source Code Vision's beam (the Solar Beam, and with the jewel off his Mind Stone beam) is fire: it sets",
                "what it strikes alight and, where it hits the ground, leaves fire there and lights what can burn (fire is placed;",
                "no block is deleted). Never with the game rule mobGriefing off, nor where the mod's block rules refuse. Off: it only",
                "burns the creatures it hits.")
                .define("beamFire", true);
        SOURCE_CODE_BURN_SECONDS = builder.comment("With the Solar Jewel off: seconds a creature Source Code Vision's beam strikes burns",
                "(0: they are not set alight). With it on, beamBurnSeconds is used.")
                .defineInRange("sourceCodeBurnSeconds", 4, 0, 60);
        OVERLOAD_MIN_CHARGE = builder.comment("Energy the jewel needs to overload (it then spends all it holds).")
                .defineInRange("overloadMinCharge", 400, 1, 100000);
        OVERLOAD_CHARGE_TICKS = builder.comment("Ticks the light gathers in the jewel before the Overload fires.")
                .defineInRange("overloadChargeTicks", 30, 5, 200);
        OVERLOAD_FIRE_TICKS = builder.comment("Ticks the Overload's great beam lasts.")
                .defineInRange("overloadFireTicks", 24, 5, 200);
        OVERLOAD_DAMAGE = builder.comment("Damage of the Overload to each creature in its beam (once), at full charge; less with less.")
                .defineInRange("overloadDamage", 22.0, 0.0, 500.0);
        OVERLOAD_RANGE = builder.comment("How far (blocks) the Overload's beam reaches.")
                .defineInRange("overloadRange", 64, 8, 256);
        OVERLOAD_WIDTH = builder.comment("Radius (blocks) of the Overload's beam: what it touches is struck.")
                .defineInRange("overloadWidth", 1.5, 0.3, 8.0);
        OVERLOAD_COOLDOWN = builder.comment("Ticks after an Overload before the jewel can overload again (it is drained).")
                .defineInRange("overloadCooldown", 400, 0, 12000);
        EYE_BEAM_DAMAGE = builder.comment("Damage per hit of the infrared eye beams.")
                .defineInRange("eyeBeamDamage", 3.0, 0.0, 100.0);
        EYE_BEAM_RANGE = builder.comment("How far (blocks) the eye beams reach.")
                .defineInRange("eyeBeamRange", 28, 4, 128);
        EYE_BEAM_COST = builder.comment("Energy the eye beams spend each tick they are held.")
                .defineInRange("eyeBeamCost", 2, 0, 1000);
        EYE_BEAM_INTERVAL = builder.comment("Ticks between two hits of the eye beams.")
                .defineInRange("eyeBeamInterval", 5, 1, 100);
        EYE_BEAM_BURN_SECONDS = builder.comment("Seconds a creature the eye beams strike burns (their heat also melts snow and ice).")
                .defineInRange("eyeBeamBurnSeconds", 5, 0, 60);
        builder.pop();
    }

    private SolarJewelConfig() {
    }
}
