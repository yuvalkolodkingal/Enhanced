package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of the Ultron boss fight (the rules all bosses share are in {@link BossConfig}). Its options are
 * defined in {@link #define}, which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code
 * [ultron_boss]}). The fields are assigned there, at mod construction, before anything reads them.
 *
 * <p>The health and damage numbers are those of Normal difficulty; the boss and its minions scale like every mob's, and
 * {@code [boss].difficultyScale} multiplies them all.
 */
public final class UltronBossConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "ultron_boss";

    /** How the island of Sokovia is made (the Sokovia lift sets the rest of the fight's pace by it). */
    public enum LiftMode {
        /** The island is a copy built in the sky and the real ground is never edited. */
        SKY_ISLAND,
        /** The real ground is cut out, lifted and put back (a later release, if ever). */
        CUT_GROUND,
        /** Nothing moves: the ground shakes and the fight goes on where it began. */
        TREMOR
    }

    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.DoubleValue BODY_HEALTH;
    public static ModConfigSpec.DoubleValue BODY_HEALTH_DECAY;
    public static ModConfigSpec.DoubleValue BODY_HEALTH_FLOOR;
    public static ModConfigSpec.DoubleValue HEALTH_PER_EXTRA_PLAYER;
    public static ModConfigSpec.DoubleValue BODY_ARMOR;
    public static ModConfigSpec.DoubleValue BODY_TOUGHNESS;
    public static ModConfigSpec.DoubleValue MAX_HIT_FRACTION;
    public static ModConfigSpec.DoubleValue EXPLOSION_DAMAGE_FACTOR;
    public static ModConfigSpec.DoubleValue LIFT_AT;
    public static ModConfigSpec.DoubleValue HEART_RIP_THRESHOLD;
    public static ModConfigSpec.DoubleValue VOLLEY_BOLT_DAMAGE;
    public static ModConfigSpec.DoubleValue FINGER_BEAM_DAMAGE;
    public static ModConfigSpec.DoubleValue DIVE_DAMAGE;
    public static ModConfigSpec.DoubleValue PULL_DAMAGE;
    public static ModConfigSpec.DoubleValue PUNCH_DAMAGE;
    public static ModConfigSpec.DoubleValue DEBRIS_DAMAGE;
    public static ModConfigSpec.IntValue DEBRIS_INTERVAL;
    public static ModConfigSpec.DoubleValue SENTRY_HEALTH;
    public static ModConfigSpec.DoubleValue SENTRY_BOLT_DAMAGE;
    public static ModConfigSpec.DoubleValue SENTRY_MELEE_DAMAGE;
    public static ModConfigSpec.IntValue SENTRY_STUN_TICKS;
    public static ModConfigSpec.IntValue SENTRY_BUDGET_BASE;
    public static ModConfigSpec.IntValue SENTRY_BUDGET_PER_PLAYER;
    public static ModConfigSpec.IntValue SENTRIES_AT_START;
    public static ModConfigSpec.IntValue SENTRY_WAVE_SIZE;
    public static ModConfigSpec.IntValue SENTRY_WAVE_INTERVAL;
    public static ModConfigSpec.IntValue SENTRY_MAX_ALIVE;
    public static ModConfigSpec.IntValue SENTRY_MAX_ALIVE_PER_PLAYER;
    public static ModConfigSpec.IntValue MAX_UPLOADS;
    public static ModConfigSpec.BooleanValue LIFT_ENABLED;
    public static ModConfigSpec.EnumValue<LiftMode> LIFT_MODE;
    public static ModConfigSpec.BooleanValue HIDE_GROUND;
    public static ModConfigSpec.BooleanValue LIFT_REQUIRES_MOB_GRIEFING;
    public static ModConfigSpec.IntValue ISLAND_RADIUS;
    public static ModConfigSpec.IntValue LIFT_HEIGHT;
    public static ModConfigSpec.IntValue LIFT_TICKS;
    public static ModConfigSpec.IntValue SKIN_DEPTH;
    public static ModConfigSpec.IntValue COVER_HEIGHT;
    public static ModConfigSpec.IntValue CONE_DEPTH;
    public static ModConfigSpec.IntValue MAX_RELIEF;
    public static ModConfigSpec.IntValue EDITS_PER_TICK;
    public static ModConfigSpec.DoubleValue RESCUE_DAMAGE;
    public static ModConfigSpec.IntValue REJOIN_LIMIT;
    public static ModConfigSpec.DoubleValue VISION_BEAM_BONUS;
    public static ModConfigSpec.IntValue HOLD_TICKS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether the Ultron ritual works at all.")
                .define("enabled", true);
        BODY_HEALTH = builder.comment("Health of the first body, Ultron Prime, for one player.",
                        "(A creature can hold 1024 at most: for more, every blow is made smaller in proportion.)")
                .defineInRange("bodyHealth", 600.0, 50.0, 20000.0);
        BODY_HEALTH_DECAY = builder.comment("Each new body has this fraction of the last one's health.")
                .defineInRange("bodyHealthDecay", 0.8, 0.3, 1.0);
        BODY_HEALTH_FLOOR = builder.comment("No body has less than this fraction of the first one's health.")
                .defineInRange("bodyHealthFloor", 0.25, 0.05, 1.0);
        HEALTH_PER_EXTRA_PLAYER = builder.comment("Extra health (as a fraction of the first body) for each participant after the first",
                        "(at most 6 count).")
                .defineInRange("healthPerExtraPlayer", 0.35, 0.0, 2.0);
        BODY_ARMOR = builder.comment("Armour of Prime (a body later has less).")
                .defineInRange("bodyArmor", 8.0, 0.0, 30.0);
        BODY_TOUGHNESS = builder.comment("Armour toughness of Prime (a body later has less).")
                .defineInRange("bodyToughness", 4.0, 0.0, 20.0);
        MAX_HIT_FRACTION = builder.comment("The most a single hit can take from a body, as a fraction of its maximum health (before armour).")
                .defineInRange("maxHitFraction", 0.15, 0.02, 1.0);
        EXPLOSION_DAMAGE_FACTOR = builder.comment("Explosion damage to a body is multiplied by this.")
                .defineInRange("explosionDamageFactor", 0.5, 0.0, 1.0);
        LIFT_AT = builder.comment("Prime's health fraction at which the Sokovia phase starts.")
                .defineInRange("liftAt", 0.55, 0.1, 0.95);
        HEART_RIP_THRESHOLD = builder.comment("A Scarlet Witch can tear a body's core out below this fraction of its health (0 turns it off).")
                .defineInRange("heartRipThreshold", 0.15, 0.0, 0.5);
        VOLLEY_BOLT_DAMAGE = builder.comment("Damage of one Repulsor Volley bolt (Normal difficulty).")
                .defineInRange("volleyBoltDamage", 6.0, 0.0, 100.0);
        FINGER_BEAM_DAMAGE = builder.comment("Damage of one Finger Beams hit (Normal difficulty).")
                .defineInRange("fingerBeamDamage", 6.0, 0.0, 100.0);
        DIVE_DAMAGE = builder.comment("Damage of the Thruster Dive's landing.")
                .defineInRange("diveDamage", 18.0, 0.0, 100.0);
        PULL_DAMAGE = builder.comment("Damage of the Gravity Pull's punch.")
                .defineInRange("pullDamage", 14.0, 0.0, 100.0);
        PUNCH_DAMAGE = builder.comment("Damage of the Punch.")
                .defineInRange("punchDamage", 12.0, 0.0, 100.0);
        DEBRIS_DAMAGE = builder.comment("Damage of a piece of falling Sokovia.")
                .defineInRange("debrisDamage", 10.0, 0.0, 100.0);
        DEBRIS_INTERVAL = builder.comment("Ticks between falling debris on the island, for every two participants.")
                .defineInRange("debrisInterval", 100, 20, 1200);
        SENTRY_HEALTH = builder.comment("Health of an Ultron Sentry of the fight.")
                .defineInRange("sentryHealth", 40.0, 4.0, 500.0);
        SENTRY_BOLT_DAMAGE = builder.comment("Damage of a Sentry's plasma bolt.")
                .defineInRange("sentryBoltDamage", 4.0, 0.0, 100.0);
        SENTRY_MELEE_DAMAGE = builder.comment("Damage of a Sentry's punch.")
                .defineInRange("sentryMeleeDamage", 6.0, 0.0, 100.0);
        SENTRY_STUN_TICKS = builder.comment("How long lightning stuns a Sentry.")
                .defineInRange("sentryStunTicks", 60, 0, 600);
        SENTRY_BUDGET_BASE = builder.comment("How many Sentries Ultron can still call, beyond the first ones, for one player.")
                .defineInRange("sentryBudgetBase", 20, 4, 200);
        SENTRY_BUDGET_PER_PLAYER = builder.comment("How many more Sentries Ultron can call for each extra player.")
                .defineInRange("sentryBudgetPerPlayer", 8, 0, 50);
        SENTRIES_AT_START = builder.comment("The Sentries beside him when he wakes.")
                .defineInRange("sentriesAtStart", 6, 0, 40);
        SENTRY_WAVE_SIZE = builder.comment("How many Sentries come in a wave.")
                .defineInRange("sentryWaveSize", 4, 1, 20);
        SENTRY_WAVE_INTERVAL = builder.comment("Ticks between waves of Sentries.")
                .defineInRange("sentryWaveInterval", 500, 100, 6000);
        SENTRY_MAX_ALIVE = builder.comment("How many Sentries may be alive at once, for one player.")
                .defineInRange("sentryMaxAlive", 8, 2, 60);
        SENTRY_MAX_ALIVE_PER_PLAYER = builder.comment("How many more Sentries may be alive for each extra player.")
                .defineInRange("sentryMaxAlivePerPlayer", 3, 0, 20);
        MAX_UPLOADS = builder.comment("Safety valve: after this many uploads Ultron is destroyed even if Sentries remain.")
                .defineInRange("maxUploads", 12, 1, 100);
        LIFT_ENABLED = builder.comment("Whether the island rises at all; off means the Tremor (nothing moves).")
                .define("liftEnabled", true);
        LIFT_MODE = builder.comment("How the island is made: SKY_ISLAND (a copy built in the sky, the ground is never edited),",
                        "CUT_GROUND (the real ground is lifted and put back; a later release) or TREMOR (nothing moves).")
                .defineEnum("liftMode", LiftMode.SKY_ISLAND);
        HIDE_GROUND = builder.comment("Sky Island only: clients hide the columns the island was copied from while it is up",
                        "(cosmetic; the server's ground never changes).")
                .define("hideGround", true);
        LIFT_REQUIRES_MOB_GRIEFING = builder.comment("CUT_GROUND only: if the game rule mobGriefing is off, use the Tremor instead of lifting.")
                .define("liftRequiresMobGriefing", true);
        ISLAND_RADIUS = builder.comment("Radius of the lifted ground, and of the ground fight before it.")
                .defineInRange("islandRadius", 20, 12, 32);
        LIFT_HEIGHT = builder.comment("How far the island rises.")
                .defineInRange("liftHeight", 40, 16, 80);
        LIFT_TICKS = builder.comment("How long the rise and the descent take, in ticks (1 = a cut).")
                .defineInRange("liftTicks", 160, 1, 600);
        SKIN_DEPTH = builder.comment("Layers of real ground that rise with the island.")
                .defineInRange("skinDepth", 5, 3, 8);
        COVER_HEIGHT = builder.comment("How far above the ground things are carried (grass, flowers, trees).")
                .defineInRange("coverHeight", 16, 8, 24);
        CONE_DEPTH = builder.comment("The depth of the rock cone under the middle of the island.")
                .defineInRange("coneDepth", 14, 6, 24);
        MAX_RELIEF = builder.comment("The most the ground may rise and fall inside the island's disc.")
                .defineInRange("maxRelief", 6, 2, 12);
        EDITS_PER_TICK = builder.comment("Block edits per tick while the island is built or taken down (lower it on a weak server).")
                .defineInRange("editsPerTick", 2500, 200, 20000);
        RESCUE_DAMAGE = builder.comment("Damage (never lethal) when the field pulls someone back onto the island.")
                .defineInRange("rescueDamage", 2.0, 0.0, 10.0);
        REJOIN_LIMIT = builder.comment("How many times a player may be taken up again by the pit's tractor.")
                .defineInRange("rejoinLimit", 3, 0, 20);
        VISION_BEAM_BONUS = builder.comment("The Mind Stone beam does this much to Ultron and the Sentries.")
                .defineInRange("visionBeamBonus", 1.5, 1.0, 5.0);
        HOLD_TICKS = builder.comment("How long telekinesis can hold a body of Ultron before he breaks free (before the hero's factor,",
                        "boss.billyHoldFactor or boss.magnetoHoldFactor).")
                .defineInRange("holdTicks", 60, 10, 600);
        builder.pop();
    }

    private UltronBossConfig() {
    }
}
