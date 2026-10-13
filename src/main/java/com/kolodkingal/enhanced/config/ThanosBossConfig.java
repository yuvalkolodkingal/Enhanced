package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of the Thanos boss fight (the rules all bosses share are in {@link BossConfig}). Its options are
 * defined in {@link #define}, which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code
 * [thanos_boss]}). Ticks are 1/20 s; every damage is Normal difficulty, raw, before armour, and is multiplied by the
 * shared {@link BossConfig#DIFFICULTY_SCALE} (as is his health).
 */
public final class ThanosBossConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "thanos_boss";

    // ---- the summoning ----
    public static ModConfigSpec.BooleanValue SUMMON_ENABLED;
    public static ModConfigSpec.BooleanValue SUMMON_OVERWORLD_ONLY;
    public static ModConfigSpec.IntValue SUMMON_CHARGE_TICKS;
    public static ModConfigSpec.IntValue SUMMON_COUNTDOWN_TICKS;
    public static ModConfigSpec.IntValue SUMMON_SKY_BLOCKS;
    public static ModConfigSpec.DoubleValue SUMMON_OPEN_FRACTION;
    public static ModConfigSpec.IntValue SUMMON_COOLDOWN_TICKS;

    // ---- the body, the clock and the arena ----
    public static ModConfigSpec.DoubleValue HEALTH;
    public static ModConfigSpec.DoubleValue HEALTH_PER_EXTRA_PLAYER;
    public static ModConfigSpec.IntValue MAX_FIGHTERS_COUNTED;
    public static ModConfigSpec.IntValue ARENA_RADIUS;
    public static ModConfigSpec.IntValue XP_REWARD;
    public static ModConfigSpec.IntValue MAX_STONES;
    public static ModConfigSpec.IntValue STONE_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue SURGE_TICKS;
    public static ModConfigSpec.DoubleValue SURGE_DAMAGE_FACTOR;
    public static ModConfigSpec.DoubleValue DAMAGE_SCALE_PER_STONE;
    public static ModConfigSpec.IntValue ENRAGE_AFTER_TICKS;
    public static ModConfigSpec.DoubleValue ENRAGE_STEP_PER_MINUTE;
    public static ModConfigSpec.DoubleValue ENRAGE_MAX;
    public static ModConfigSpec.IntValue GAP_PER_EXTRA_PLAYER;
    public static ModConfigSpec.BooleanValue BLOCK_DAMAGE;

    // ---- his damage (Normal, raw) ----
    public static ModConfigSpec.DoubleValue SWORD_DAMAGE;
    public static ModConfigSpec.DoubleValue SMASH_DAMAGE;
    public static ModConfigSpec.DoubleValue SMASH_RING_DAMAGE;
    public static ModConfigSpec.DoubleValue JAB_DAMAGE;
    public static ModConfigSpec.DoubleValue SWORD_THROW_DAMAGE;
    public static ModConfigSpec.DoubleValue CHOKE_DAMAGE;
    public static ModConfigSpec.DoubleValue LEAP_DAMAGE;
    public static ModConfigSpec.DoubleValue POWER_BLAST_DAMAGE;
    public static ModConfigSpec.DoubleValue POWER_BLAST_SPLASH;
    public static ModConfigSpec.DoubleValue OVERLOAD_DAMAGE;
    public static ModConfigSpec.DoubleValue SHOCKWAVE_DAMAGE;
    public static ModConfigSpec.DoubleValue METEOR_DAMAGE;
    public static ModConfigSpec.DoubleValue PULL_CRUSH_DAMAGE;
    public static ModConfigSpec.DoubleValue SOUL_DRAIN_TICK;
    public static ModConfigSpec.DoubleValue PULSE_DAMAGE;
    public static ModConfigSpec.DoubleValue INFINITY_BEAM_DAMAGE;
    public static ModConfigSpec.DoubleValue PHANTOM_DAMAGE;
    public static ModConfigSpec.DoubleValue SWORD_SHIELD_FACTOR;

    // ---- his cooldowns (ticks) ----
    public static ModConfigSpec.IntValue SWORD_COOLDOWN;
    public static ModConfigSpec.IntValue SMASH_COOLDOWN;
    public static ModConfigSpec.IntValue JAB_COOLDOWN;
    public static ModConfigSpec.IntValue SWORD_THROW_COOLDOWN;
    public static ModConfigSpec.IntValue CHOKE_COOLDOWN;
    public static ModConfigSpec.IntValue LEAP_COOLDOWN;
    public static ModConfigSpec.IntValue POWER_BLAST_COOLDOWN;
    public static ModConfigSpec.IntValue OVERLOAD_COOLDOWN;
    public static ModConfigSpec.IntValue SHOCKWAVE_COOLDOWN;
    public static ModConfigSpec.IntValue STEP_COOLDOWN;
    public static ModConfigSpec.IntValue METEOR_COOLDOWN;
    public static ModConfigSpec.IntValue PULL_COOLDOWN;
    public static ModConfigSpec.IntValue BARRIER_COOLDOWN;
    public static ModConfigSpec.IntValue VEIL_COOLDOWN;
    public static ModConfigSpec.IntValue PHANTOMS_COOLDOWN;
    public static ModConfigSpec.IntValue ORB_COOLDOWN;
    public static ModConfigSpec.IntValue SIGHT_COOLDOWN;
    public static ModConfigSpec.IntValue DRAIN_COOLDOWN;
    public static ModConfigSpec.IntValue SOULWORLD_COOLDOWN;
    public static ModConfigSpec.IntValue REWIND_COOLDOWN;
    public static ModConfigSpec.IntValue SNARE_COOLDOWN;
    public static ModConfigSpec.IntValue INFINITY_BEAM_COOLDOWN;
    public static ModConfigSpec.IntValue PULSE_COOLDOWN;

    // ---- durations and rules ----
    public static ModConfigSpec.IntValue BARRIER_TICKS;
    public static ModConfigSpec.IntValue VEIL_TICKS;
    public static ModConfigSpec.IntValue SNARE_TICKS;
    public static ModConfigSpec.IntValue SOULWORLD_TICKS;
    public static ModConfigSpec.DoubleValue SOULWORLD_HEAL;
    public static ModConfigSpec.DoubleValue DRAIN_HEAL_FRACTION;
    public static ModConfigSpec.IntValue REWIND_SECONDS;
    public static ModConfigSpec.DoubleValue REWIND_MAX_HEAL;
    public static ModConfigSpec.DoubleValue REWIND_TRIGGER;
    public static ModConfigSpec.DoubleValue PLATE_REDUCTION;
    public static ModConfigSpec.IntValue PLATE_TEAR_SECONDS;
    public static ModConfigSpec.IntValue STAGGER_POISE;
    public static ModConfigSpec.DoubleValue POISE_HIT;
    public static ModConfigSpec.IntValue LOCK_TICKS;
    public static ModConfigSpec.DoubleValue STORMBREAKER_FACTOR;
    public static ModConfigSpec.DoubleValue MAX_HIT_FRACTION;
    public static ModConfigSpec.DoubleValue MAX_DAMAGE_PER_SECOND;
    public static ModConfigSpec.DoubleValue MOB_DAMAGE_FACTOR;
    public static ModConfigSpec.IntValue METEOR_EXTRA;
    public static ModConfigSpec.IntValue UNREACHABLE_TICKS;
    public static ModConfigSpec.DoubleValue IDLE_REGEN_PER_MINUTE;
    public static ModConfigSpec.DoubleValue REWARD_MIN_SHARE;
    public static ModConfigSpec.IntValue GAUNTLET_RITUAL_TICKS;
    public static ModConfigSpec.IntValue WEB_LOCK_TICKS;
    public static ModConfigSpec.IntValue WEB_LOCK_COOLDOWN;
    public static ModConfigSpec.IntValue HOLD_TICKS;
    public static ModConfigSpec.IntValue SWORD_HOLD_TICKS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);

        // The summoning.
        SUMMON_ENABLED = builder.comment("Whether the Tesseract and Loki's Scepter, held together, can call Thanos.")
                .define("summonEnabled", true);
        SUMMON_OVERWORLD_ONLY = builder.comment("Whether Thanos answers only in the Overworld.")
                .define("summonOverworldOnly", true);
        SUMMON_CHARGE_TICKS = builder.comment("How long the two items are held up before the portal opens (at most the 100 ticks the items can be held).")
                .defineInRange("summonChargeTicks", 100, 20, 100);
        SUMMON_COUNTDOWN_TICKS = builder.comment("From the portal opening to his arrival.")
                .defineInRange("summonCountdownTicks", 120, 20, 1200);
        SUMMON_SKY_BLOCKS = builder.comment("Clear air needed above the summoner.")
                .defineInRange("summonSkyBlocks", 24, 4, 64);
        SUMMON_OPEN_FRACTION = builder.comment("The fraction of the 24 sample spots round the summoner that need standing room.")
                .defineInRange("summonOpenFraction", 0.6, 0.0, 1.0);
        SUMMON_COOLDOWN_TICKS = builder.comment("The rest after a fight before he can be called again (half after he left or the fighters were wiped).")
                .defineInRange("summonCooldownTicks", 12000, 0, 1728000);

        // The body, the clock and the arena.
        HEALTH = builder.comment("His health for one fighter.")
                .defineInRange("health", 3000.0, 100.0, 100000.0);
        HEALTH_PER_EXTRA_PLAYER = builder.comment("The fraction of his health added for each extra fighter.")
                .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 2.0);
        MAX_FIGHTERS_COUNTED = builder.comment("How many fighters count for his health and the targets of his area moves.")
                .defineInRange("maxFightersCounted", 4, 1, 16);
        ARENA_RADIUS = builder.comment("The radius of the fight round the summoning point (the clock and the leash reach 24 blocks beyond it).")
                .defineInRange("arenaRadius", 40, 16, 96);
        XP_REWARD = builder.comment("The experience dropped when he is defeated.")
                .defineInRange("xpReward", 12000, 0, 100000);
        MAX_STONES = builder.comment("The most Infinity Stones he can have. There is no sixth: the Mind Stone never comes.")
                .defineInRange("maxStones", 5, 1, 5);
        STONE_INTERVAL_TICKS = builder.comment("Combat time between one stone and the next (1800 = 90 seconds).")
                .defineInRange("stoneIntervalTicks", 1800, 200, 36000);
        SURGE_TICKS = builder.comment("The power surge when a stone arrives: he kneels and takes extra damage.")
                .defineInRange("surgeTicks", 60, 0, 200);
        SURGE_DAMAGE_FACTOR = builder.comment("The damage he takes in the surge, times.")
                .defineInRange("surgeDamageFactor", 1.5, 1.0, 4.0);
        DAMAGE_SCALE_PER_STONE = builder.comment("The extra damage he deals for each stone beyond the first (0.06 = 6 percent).")
                .defineInRange("damageScalePerStone", 0.06, 0.0, 0.5);
        ENRAGE_AFTER_TICKS = builder.comment("The time after his last stone before his damage starts to grow.")
                .defineInRange("enrageAfterTicks", 3600, 0, 72000);
        ENRAGE_STEP_PER_MINUTE = builder.comment("How much his damage grows each minute of enrage (0.04 = 4 percent).")
                .defineInRange("enrageStepPerMinute", 0.04, 0.0, 0.5);
        ENRAGE_MAX = builder.comment("The most the enrage adds to his damage (0.4 = 40 percent).")
                .defineInRange("enrageMax", 0.40, 0.0, 2.0);
        GAP_PER_EXTRA_PLAYER = builder.comment("Ticks cut from his rest between moves for each extra fighter.")
                .defineInRange("gapPerExtraPlayer", 2, 0, 10);
        BLOCK_DAMAGE = builder.comment("If true, and the mobGriefing rule is on, Overload and the meteors break blocks with small explosions.",
                        "Nothing else he does ever breaks a block.")
                .define("blockDamage", false);

        // His damage.
        SWORD_DAMAGE = damage(builder, "swordDamage", 30.0, "A Sword Sweep.");
        SMASH_DAMAGE = damage(builder, "smashDamage", 26.0, "The impact of an Overhead Smash.");
        SMASH_RING_DAMAGE = damage(builder, "smashRingDamage", 12.0, "The ground ring of an Overhead Smash.");
        JAB_DAMAGE = damage(builder, "jabDamage", 14.0, "Each Gauntlet Jab.");
        SWORD_THROW_DAMAGE = damage(builder, "swordThrowDamage", 22.0, "Each pass of the thrown sword.");
        CHOKE_DAMAGE = damage(builder, "chokeDamage", 16.0, "The whole Choke hold (the throw at the end adds half again).");
        LEAP_DAMAGE = damage(builder, "leapDamage", 12.0, "The landing of a Leap.");
        POWER_BLAST_DAMAGE = damage(builder, "powerBlastDamage", 20.0, "The Power Blast beam.");
        POWER_BLAST_SPLASH = damage(builder, "powerBlastSplash", 8.0, "The explosion at the end of the Power Blast.");
        OVERLOAD_DAMAGE = damage(builder, "overloadDamage", 16.0, "Each sigil of Overload.");
        SHOCKWAVE_DAMAGE = damage(builder, "shockwaveDamage", 12.0, "The Titan Shockwave ring.");
        METEOR_DAMAGE = damage(builder, "meteorDamage", 16.0, "Each meteor.");
        PULL_CRUSH_DAMAGE = damage(builder, "pullCrushDamage", 10.0, "The crush at the end of the Gravity Pull.");
        SOUL_DRAIN_TICK = damage(builder, "soulDrainTick", 3.0, "Each pulse of the Soul Drain (every 10 ticks; ignores armour and enchantments).");
        PULSE_DAMAGE = damage(builder, "pulseDamage", 12.0, "The Power Pulse that finds a fighter hiding behind walls.");
        INFINITY_BEAM_DAMAGE = damage(builder, "infinityBeamDamage", 40.0, "The Infinity Beam.");
        PHANTOM_DAMAGE = damage(builder, "phantomDamage", 3.5, "A jab of one of his illusions.");
        SWORD_SHIELD_FACTOR = builder.comment("What his sword costs a Chaos Shield: this many times the damage it would have done.")
                .defineInRange("swordShieldFactor", 2.0, 1.0, 10.0);

        // His cooldowns.
        SWORD_COOLDOWN = cooldown(builder, "swordCooldown", 40);
        SMASH_COOLDOWN = cooldown(builder, "smashCooldown", 100);
        JAB_COOLDOWN = cooldown(builder, "jabCooldown", 30);
        SWORD_THROW_COOLDOWN = cooldown(builder, "swordThrowCooldown", 280);
        CHOKE_COOLDOWN = cooldown(builder, "chokeCooldown", 400);
        LEAP_COOLDOWN = cooldown(builder, "leapCooldown", 160);
        POWER_BLAST_COOLDOWN = cooldown(builder, "powerBlastCooldown", 120);
        OVERLOAD_COOLDOWN = cooldown(builder, "overloadCooldown", 200);
        SHOCKWAVE_COOLDOWN = cooldown(builder, "shockwaveCooldown", 240);
        STEP_COOLDOWN = cooldown(builder, "stepCooldown", 160);
        METEOR_COOLDOWN = cooldown(builder, "meteorCooldown", 440);
        PULL_COOLDOWN = cooldown(builder, "pullCooldown", 360);
        BARRIER_COOLDOWN = cooldown(builder, "barrierCooldown", 300);
        VEIL_COOLDOWN = cooldown(builder, "veilCooldown", 280);
        PHANTOMS_COOLDOWN = cooldown(builder, "phantomsCooldown", 560);
        ORB_COOLDOWN = cooldown(builder, "orbCooldown", 240);
        SIGHT_COOLDOWN = cooldown(builder, "sightCooldown", 240);
        DRAIN_COOLDOWN = cooldown(builder, "drainCooldown", 320);
        SOULWORLD_COOLDOWN = cooldown(builder, "soulworldCooldown", 800);
        REWIND_COOLDOWN = cooldown(builder, "rewindCooldown", 600);
        SNARE_COOLDOWN = cooldown(builder, "snareCooldown", 360);
        INFINITY_BEAM_COOLDOWN = cooldown(builder, "infinityBeamCooldown", 900);
        PULSE_COOLDOWN = cooldown(builder, "pulseCooldown", 80);

        // Durations and rules.
        BARRIER_TICKS = builder.comment("How long the Space Barrier stands.").defineInRange("barrierTicks", 60, 10, 400);
        VEIL_TICKS = builder.comment("How long his Reality Veil stands.").defineInRange("veilTicks", 120, 10, 600);
        SNARE_TICKS = builder.comment("How long the Time Snare locks whoever it catches.").defineInRange("snareTicks", 80, 10, 400);
        SOULWORLD_TICKS = builder.comment("How long a soul stays in the Soul World.").defineInRange("soulworldTicks", 80, 10, 400);
        SOULWORLD_HEAL = builder.comment("The fraction of his maximum health he heals for each soul taken into the Soul World.")
                .defineInRange("soulworldHeal", 0.03, 0.0, 0.5);
        DRAIN_HEAL_FRACTION = builder.comment("How much of the Soul Drain's damage he heals (0.4 = 40 percent).")
                .defineInRange("drainHealFraction", 0.4, 0.0, 2.0);
        REWIND_SECONDS = builder.comment("How far back his Rewind goes, in seconds.").defineInRange("rewindSeconds", 6, 1, 20);
        REWIND_MAX_HEAL = builder.comment("The most health a Rewind returns (a fraction of his maximum).")
                .defineInRange("rewindMaxHeal", 0.08, 0.0, 0.5);
        REWIND_TRIGGER = builder.comment("The damage he took in the window that makes him rewind (a fraction of his maximum).")
                .defineInRange("rewindTrigger", 0.06, 0.0, 1.0);
        PLATE_REDUCTION = builder.comment("The share of the damage each intact armour plate takes off (four plates, so 0.1 means 40 percent).")
                .defineInRange("plateReduction", 0.10, 0.0, 0.2);
        PLATE_TEAR_SECONDS = builder.comment("Seconds of a Scarlet Witch's Chaos Beam on him that tear a plate away.")
                .defineInRange("plateTearSeconds", 3, 1, 30);
        STAGGER_POISE = builder.comment("The poise damage during a stone move's wind-up that interrupts it and staggers him.")
                .defineInRange("staggerPoise", 100, 10, 1000);
        POISE_HIT = builder.comment("Poise per point of damage he takes (three times that for damage born of an Infinity Stone).")
                .defineInRange("poiseHit", 2.0, 0.1, 10.0);
        LOCK_TICKS = builder.comment("How long his stones stay dark after a stagger.").defineInRange("lockTicks", 80, 10, 400);
        STORMBREAKER_FACTOR = builder.comment("The damage Stormbreaker does to him, times.").defineInRange("stormbreakerFactor", 2.5, 1.0, 10.0);
        MAX_HIT_FRACTION = builder.comment("The most one hit can take from him (a fraction of his maximum health).")
                .defineInRange("maxHitFraction", 0.06, 0.005, 1.0);
        MAX_DAMAGE_PER_SECOND = builder.comment("The most one second can take from him (a fraction of his maximum health).")
                .defineInRange("maxDamagePerSecond", 0.15, 0.01, 1.0);
        MOB_DAMAGE_FACTOR = builder.comment("What golems, wolves and other creatures that are not players do to him, times.")
                .defineInRange("mobDamageFactor", 0.25, 0.0, 1.0);
        METEOR_EXTRA = builder.comment("Random meteors beyond one for each player.").defineInRange("meteorExtra", 4, 0, 8);
        UNREACHABLE_TICKS = builder.comment("How long a fighter can hide where he can neither walk nor see before the Power Pulse finds them.")
                .defineInRange("unreachableTicks", 160, 40, 1200);
        IDLE_REGEN_PER_MINUTE = builder.comment("His healing per minute while no fighter is in the arena (a fraction of his maximum).")
                .defineInRange("idleRegenPerMinute", 0.02, 0.0, 0.5);
        REWARD_MIN_SHARE = builder.comment("The damage (a fraction of his maximum health) that earns an Infinity Gauntlet, together with the shared",
                        "rewardMinSeconds. Whoever strikes the last blow always earns one.")
                .defineInRange("rewardMinShare", 0.03, 0.0, 0.5);
        GAUNTLET_RITUAL_TICKS = builder.comment("How long the Infinity Gauntlet must be held to become Thanos.")
                .defineInRange("gauntletRitualTicks", 60, 20, 400);
        WEB_LOCK_TICKS = builder.comment("A Spider-Man's web shot locks his Gauntlet for this long (0 turns it off).")
                .defineInRange("webLockTicks", 40, 0, 400);
        WEB_LOCK_COOLDOWN = builder.comment("A web can lock his Gauntlet at most this often.")
                .defineInRange("webLockCooldown", 400, 0, 6000);
        HOLD_TICKS = builder.comment("How long telekinesis can hold Thanos before he breaks free (before the hero's factor, boss.billyHoldFactor",
                        "or boss.magnetoHoldFactor).")
                .defineInRange("holdTicks", 40, 10, 600);
        SWORD_HOLD_TICKS = builder.comment("How long telekinesis can hold his thrown sword before it tears free and flies home (every hero; Magneto",
                        "boss.magnetoHoldFactor times as long).")
                .defineInRange("swordHoldTicks", 40, 5, 200);

        builder.pop();
    }

    private static ModConfigSpec.DoubleValue damage(ModConfigSpec.Builder builder, String name, double value, String what) {
        return builder.comment("Damage of his attack: " + what).defineInRange(name, value, 0.0, 400.0);
    }

    private static ModConfigSpec.IntValue cooldown(ModConfigSpec.Builder builder, String name, int ticks) {
        return builder.comment("How long that move waits before he can use it again (ticks).").defineInRange(name, ticks, 0, 6000);
    }

    private ThanosBossConfig() {
    }
}
