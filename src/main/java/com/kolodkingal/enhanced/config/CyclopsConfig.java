package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The balance numbers of Cyclops (3.3): his body, the optic blast he fires through his ruby-quartz glasses, the giant
 * beam that pours out when he takes them off, and his other abilities. Its options are defined in {@link #define},
 * which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code [cyclops]}).
 */
public final class CyclopsConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "cyclops";

    /**
     * Assigned by {@link #define} when the config is built, which is at mod construction: before anything reads it (it
     * cannot be final because the options are defined in a method, not in a static block).
     */
    // the body
    public static ConfigSpec.DoubleValue BODY_HEALTH_BONUS;
    public static ConfigSpec.DoubleValue ARMOR;
    // the optic blast (R held)
    public static ConfigSpec.DoubleValue OPTIC_RANGE;
    public static ConfigSpec.DoubleValue OPTIC_DAMAGE;
    public static ConfigSpec.IntValue OPTIC_INTERVAL;
    public static ConfigSpec.DoubleValue OPTIC_PUSH;
    public static ConfigSpec.DoubleValue OPTIC_EXHAUSTION_PER_SECOND;
    // the glasses off: the giant beam
    public static ConfigSpec.DoubleValue UNLEASHED_RANGE;
    public static ConfigSpec.DoubleValue UNLEASHED_DAMAGE;
    public static ConfigSpec.IntValue UNLEASHED_INTERVAL;
    public static ConfigSpec.DoubleValue UNLEASHED_RADIUS;
    public static ConfigSpec.DoubleValue UNLEASHED_PUSH;
    public static ConfigSpec.BooleanValue UNLEASHED_BREAKS_BLOCKS;
    public static ConfigSpec.DoubleValue UNLEASHED_MAX_HARDNESS;
    // the ricochet (Sneak + R)
    public static ConfigSpec.DoubleValue RICOCHET_DAMAGE;
    public static ConfigSpec.IntValue RICOCHET_BOUNCES;
    public static ConfigSpec.DoubleValue RICOCHET_RANGE;
    public static ConfigSpec.IntValue RICOCHET_COOLDOWN;
    public static ConfigSpec.DoubleValue RICOCHET_EXHAUSTION;
    // the concussion blast (G)
    public static ConfigSpec.DoubleValue CONCUSSION_RANGE;
    public static ConfigSpec.DoubleValue CONCUSSION_ANGLE;
    public static ConfigSpec.DoubleValue CONCUSSION_DAMAGE;
    public static ConfigSpec.DoubleValue CONCUSSION_KNOCKBACK;
    public static ConfigSpec.IntValue CONCUSSION_COOLDOWN;
    public static ConfigSpec.DoubleValue CONCUSSION_EXHAUSTION;
    // the optic leap (V)
    public static ConfigSpec.DoubleValue LEAP_UP;
    public static ConfigSpec.DoubleValue LEAP_FORWARD;
    public static ConfigSpec.DoubleValue LEAP_BLAST_RADIUS;
    public static ConfigSpec.DoubleValue LEAP_BLAST_DAMAGE;
    public static ConfigSpec.IntValue LEAP_COOLDOWN;
    public static ConfigSpec.DoubleValue LEAP_EXHAUSTION;
    // the rally (B)
    public static ConfigSpec.IntValue RALLY_RADIUS;
    public static ConfigSpec.IntValue RALLY_TICKS;
    public static ConfigSpec.IntValue RALLY_COOLDOWN;
    public static ConfigSpec.DoubleValue RALLY_EXHAUSTION;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        BODY_HEALTH_BONUS = builder.comment("Extra max health while Cyclops (10 = 5 hearts, so 30).")
                .defineInRange("bodyHealthBonus", 10.0, 0.0, 200.0);
        ARMOR = builder.comment("Armour while Cyclops (his field suit).")
                .defineInRange("armor", 4.0, 0.0, 30.0);

        OPTIC_RANGE = builder.comment("Optic blast (R held, glasses on): how far the beam reaches, in blocks.")
                .defineInRange("opticRange", 32.0, 4.0, 128.0);
        OPTIC_DAMAGE = builder.comment("Optic blast: the damage of one hit on what the crosshair is on.")
                .defineInRange("opticDamage", 2.0, 0.0, 100.0);
        OPTIC_INTERVAL = builder.comment("Optic blast: ticks between two hits (2.0 every 5 ticks is 8 a second).")
                .defineInRange("opticInterval", 5, 1, 40);
        OPTIC_PUSH = builder.comment("Optic blast: how hard each hit pushes what it hits away (his blasts are force, not heat).")
                .defineInRange("opticPush", 0.35, 0.0, 3.0);
        OPTIC_EXHAUSTION_PER_SECOND = builder.comment("Optic blast: food exhaustion a second of the beam costs (4 = one food point).")
                .defineInRange("opticExhaustionPerSecond", 0.6, 0.0, 20.0);

        UNLEASHED_RANGE = builder.comment("The glasses off: how far the giant beam reaches, in blocks. It never stops until the",
                        "glasses are back on, and it never hurts Cyclops himself; it ends when he dies or leaves.")
                .defineInRange("unleashedRange", 48.0, 4.0, 128.0);
        UNLEASHED_DAMAGE = builder.comment("The glasses off: the damage of one hit on everything in the giant beam.")
                .defineInRange("unleashedDamage", 6.0, 0.0, 100.0);
        UNLEASHED_INTERVAL = builder.comment("The glasses off: ticks between two hits of the giant beam.")
                .defineInRange("unleashedInterval", 10, 1, 40);
        UNLEASHED_RADIUS = builder.comment("The glasses off: the radius of the giant beam, in blocks (what is that close to its line is hit).")
                .defineInRange("unleashedRadius", 1.2, 0.2, 4.0);
        UNLEASHED_PUSH = builder.comment("The glasses off: how hard each hit pushes what it hits along the beam.")
                .defineInRange("unleashedPush", 0.9, 0.0, 4.0);
        UNLEASHED_BREAKS_BLOCKS = builder.comment("The glasses off: whether the giant beam breaks the blocks it strikes (off by default;",
                        "never in a boss arena or where the player may not break, and never the blocks round his own feet).")
                .define("unleashedBreaksBlocks", false);
        UNLEASHED_MAX_HARDNESS = builder.comment("The glasses off, with unleashedBreaksBlocks: the hardest block it breaks (stone is 1.5,",
                        "obsidian 50).")
                .defineInRange("unleashedMaxHardness", 3.0, 0.0, 100.0);

        RICOCHET_DAMAGE = builder.comment("Ricochet Shot (Sneak + R): the damage to each creature the bolt hits (each once).")
                .defineInRange("ricochetDamage", 7.0, 0.0, 100.0);
        RICOCHET_BOUNCES = builder.comment("Ricochet Shot: how many times the bolt bounces off a block.")
                .defineInRange("ricochetBounces", 3, 0, 8);
        RICOCHET_RANGE = builder.comment("Ricochet Shot: how far each stretch of the bolt flies, in blocks.")
                .defineInRange("ricochetRange", 24.0, 2.0, 64.0);
        RICOCHET_COOLDOWN = builder.comment("Ricochet Shot: cooldown, in ticks.")
                .defineInRange("ricochetCooldown", 40, 0, 1200);
        RICOCHET_EXHAUSTION = builder.comment("Ricochet Shot: food exhaustion.")
                .defineInRange("ricochetExhaustion", 0.5, 0.0, 20.0);

        CONCUSSION_RANGE = builder.comment("Concussion Blast (G): how far the short, wide blast reaches, in blocks.")
                .defineInRange("concussionRange", 7.0, 1.0, 24.0);
        CONCUSSION_ANGLE = builder.comment("Concussion Blast: the half-angle of its cone, in degrees.")
                .defineInRange("concussionAngle", 40.0, 5.0, 90.0);
        CONCUSSION_DAMAGE = builder.comment("Concussion Blast: the damage to each creature in the cone.")
                .defineInRange("concussionDamage", 6.0, 0.0, 100.0);
        CONCUSSION_KNOCKBACK = builder.comment("Concussion Blast: how far it throws them back.")
                .defineInRange("concussionKnockback", 1.6, 0.0, 5.0);
        CONCUSSION_COOLDOWN = builder.comment("Concussion Blast: cooldown, in ticks.")
                .defineInRange("concussionCooldown", 100, 0, 1200);
        CONCUSSION_EXHAUSTION = builder.comment("Concussion Blast: food exhaustion.")
                .defineInRange("concussionExhaustion", 1.0, 0.0, 20.0);

        LEAP_UP = builder.comment("Optic Leap (V): a blast at the ground throws him up at this speed (blocks a tick).")
                .defineInRange("leapUp", 1.1, 0.1, 3.0);
        LEAP_FORWARD = builder.comment("Optic Leap: and forward, the way he faces, at this speed.")
                .defineInRange("leapForward", 0.9, 0.0, 3.0);
        LEAP_BLAST_RADIUS = builder.comment("Optic Leap: the radius of the blast where he leaves the ground.")
                .defineInRange("leapBlastRadius", 2.5, 0.0, 8.0);
        LEAP_BLAST_DAMAGE = builder.comment("Optic Leap: the damage of that blast to the creatures in it.")
                .defineInRange("leapBlastDamage", 3.0, 0.0, 100.0);
        LEAP_COOLDOWN = builder.comment("Optic Leap: cooldown, in ticks. He takes no fall damage from the leap he makes.")
                .defineInRange("leapCooldown", 80, 0, 1200);
        LEAP_EXHAUSTION = builder.comment("Optic Leap: food exhaustion.")
                .defineInRange("leapExhaustion", 0.8, 0.0, 20.0);

        RALLY_RADIUS = builder.comment("Rally (B, \"To me, X-Men!\"): the radius in which his allies (players, tamed pets, golems) and he",
                        "are given Speed, Strength and Resistance.")
                .defineInRange("rallyRadius", 16, 1, 64);
        RALLY_TICKS = builder.comment("Rally: how long the effects last, in ticks.")
                .defineInRange("rallyTicks", 300, 20, 6000);
        RALLY_COOLDOWN = builder.comment("Rally: cooldown, in ticks.")
                .defineInRange("rallyCooldown", 900, 0, 12000);
        RALLY_EXHAUSTION = builder.comment("Rally: food exhaustion.")
                .defineInRange("rallyExhaustion", 1.0, 0.0, 20.0);
        builder.pop();
    }

    private CyclopsConfig() {
    }
}
