package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * Vision's Charged Beam (3.17), on the key of the Density it replaced (B): hold to charge, release to fire a beam whose
 * damage and reach grow with the charge; a tap fires a weak one. TOML section {@code [vision.charged_beam]}, defined inside
 * {@code [vision]} by {@code EnhancedConfig}; read when used.
 */
public final class ChargedBeamConfig {
    public static ConfigSpec.IntValue CHARGE_TICKS;
    public static ConfigSpec.IntValue TAP_TICKS;
    public static ConfigSpec.IntValue COOLDOWN;
    public static ConfigSpec.DoubleValue MIN_DAMAGE;
    public static ConfigSpec.DoubleValue MAX_DAMAGE;
    public static ConfigSpec.IntValue MIN_RANGE;
    public static ConfigSpec.IntValue MAX_RANGE;
    public static ConfigSpec.DoubleValue MAX_WIDTH;
    public static ConfigSpec.DoubleValue EXPLOSION_MIN_RADIUS;
    public static ConfigSpec.DoubleValue EXPLOSION_MAX_RADIUS;
    public static ConfigSpec.DoubleValue EXPLOSION_MAX_DAMAGE;
    public static ConfigSpec.BooleanValue EXPLOSION_BREAKS_BLOCKS;

    public static void define(ConfigSpec.Builder builder) {
        builder.comment("Vision's Charged Beam (3.17), on B where his Density was (Density is gone): hold B to charge, release to fire.",
                "The longer the charge, the more damage and the longer the reach; a tap fires a weak one. In his Source Code look it",
                "also burns (see solar_jewel.beamFire) and explodes where it strikes.")
                .push("charged_beam");
        CHARGE_TICKS = builder.comment("Ticks of holding B for a full charge (20 = one second).")
                .defineInRange("chargeTicks", 50, 5, 400);
        TAP_TICKS = builder.comment("A press shorter than this many ticks is a tap: the weakest beam.")
                .defineInRange("tapTicks", 6, 1, 100);
        COOLDOWN = builder.comment("Ticks before the beam can be charged again after it was fired.")
                .defineInRange("cooldown", 100, 0, 12000);
        MIN_DAMAGE = builder.comment("Damage of a tap (every creature in its path, once).")
                .defineInRange("minDamage", 4.0, 0.0, 500.0);
        MAX_DAMAGE = builder.comment("Damage of a full charge.")
                .defineInRange("maxDamage", 30.0, 0.0, 500.0);
        MIN_RANGE = builder.comment("Reach (blocks) of a tap.")
                .defineInRange("minRange", 14, 2, 256);
        MAX_RANGE = builder.comment("Reach (blocks) of a full charge.")
                .defineInRange("maxRange", 64, 2, 256);
        MAX_WIDTH = builder.comment("Radius (blocks) of a full charge's beam: what it touches is struck (a tap is a thin line).")
                .defineInRange("maxWidth", 1.0, 0.1, 8.0);
        EXPLOSION_MIN_RADIUS = builder.comment("Source Code look: radius (blocks) of the explosion where a tap strikes.")
                .defineInRange("explosionMinRadius", 1.0, 0.0, 16.0);
        EXPLOSION_MAX_RADIUS = builder.comment("Source Code look: radius (blocks) of the explosion where a full charge strikes (0: none).")
                .defineInRange("explosionMaxRadius", 4.0, 0.0, 16.0);
        EXPLOSION_MAX_DAMAGE = builder.comment("Source Code look: damage of the explosion at its centre for a full charge (less with less charge,",
                "less at its edge). Never to the shooter.")
                .defineInRange("explosionMaxDamage", 12.0, 0.0, 200.0);
        EXPLOSION_BREAKS_BLOCKS = builder.comment("Source Code look: whether the explosion breaks blocks. Only where the mod's block rules allow it and",
                "never with the game rule mobGriefing off (no block is broken then, whatever this says).")
                .define("explosionBreaksBlocks", true);
        builder.pop();
    }

    private ChargedBeamConfig() {
    }
}
