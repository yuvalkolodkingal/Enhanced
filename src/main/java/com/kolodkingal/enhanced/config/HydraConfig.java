package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Hydra soldiers of the Hydra outposts (3.6). Every option of the plan is defined here by the 3.6 skeleton (lane
 * L8 reads them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML section {@code
 * [hydra]}). They are read from the server and synced to clients, and read when used (never cached).
 */
public final class HydraConfig {
    /** When a beaten garrison comes back. */
    public enum Respawn {
        /** A cleared outpost stays empty. */
        NEVER,
        /** After respawnDays in-game days, while nobody is near. */
        DAYS,
        /** Every check refills it. */
        ALWAYS
    }

    /** The TOML section of these options. */
    public static final String SECTION = "hydra";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue GARRISON_SIZE;
    public static ModConfigSpec.EnumValue<Respawn> RESPAWN;
    public static ModConfigSpec.IntValue RESPAWN_DAYS;
    public static ModConfigSpec.IntValue ACTIVATE_RADIUS;
    public static ModConfigSpec.IntValue LEASH_RADIUS;
    public static ModConfigSpec.IntValue MAX_PER_OUTPOST;
    public static ModConfigSpec.IntValue MAX_LOADED;
    public static ModConfigSpec.BooleanValue ALARM;
    public static ModConfigSpec.IntValue ALARM_TICKS;
    public static ModConfigSpec.IntValue REINFORCEMENTS;
    public static ModConfigSpec.IntValue MAX_ALARMS_PER_DAY;
    public static ModConfigSpec.IntValue SIGHT_RANGE;
    public static ModConfigSpec.DoubleValue SNEAK_FACTOR;
    public static ModConfigSpec.DoubleValue RIFLE_DAMAGE;
    public static ModConfigSpec.IntValue BURST_SHOTS;
    public static ModConfigSpec.IntValue BURST_COOLDOWN;
    public static ModConfigSpec.DoubleValue SNIPER_DAMAGE;
    public static ModConfigSpec.IntValue SNIPER_RANGE;
    public static ModConfigSpec.IntValue SNIPER_AIM_TICKS;
    public static ModConfigSpec.DoubleValue BATON_DAMAGE;
    public static ModConfigSpec.DoubleValue PISTOL_DAMAGE;
    public static ModConfigSpec.DoubleValue BOLT_SPEED;
    public static ModConfigSpec.BooleanValue SCEPTER_TURNS;
    public static ModConfigSpec.IntValue SCEPTER_TURN_TICKS;
    public static ModConfigSpec.BooleanValue BLASTER_ENABLED;
    public static ModConfigSpec.DoubleValue BLASTER_DAMAGE;
    public static ModConfigSpec.IntValue BLASTER_COOLDOWN;
    public static ModConfigSpec.DoubleValue BLASTER_DROP;
    public static ModConfigSpec.DoubleValue INTEL_MAP_DROP;
    public static ModConfigSpec.DoubleValue ENERGY_CELL_DROP;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("hydra");
        ENABLED = builder.comment("Whether Hydra soldiers guard the Hydra outposts (false stops new ones; those there",
                "stay).")
                .define("enabled", true);
        GARRISON_SIZE = builder.comment("Posts of an outpost's garrison.")
                .defineInRange("garrisonSize", 9, 0, 14);
        RESPAWN = builder.comment("When a beaten garrison comes back: NEVER, DAYS (after respawnDays, while nobody is",
                "near) or ALWAYS.")
                .defineEnum("respawn", Respawn.DAYS);
        RESPAWN_DAYS = builder.comment("In-game days before a dead soldier's post refills (respawn DAYS).")
                .defineInRange("respawnDays", 3, 1, 100);
        ACTIVATE_RADIUS = builder.comment("A garrison is filled while a player is within this many blocks.")
                .defineInRange("activateRadius", 64, 16, 256);
        LEASH_RADIUS = builder.comment("The farthest (blocks) a soldier goes from his outpost.")
                .defineInRange("leashRadius", 48, 8, 128);
        MAX_PER_OUTPOST = builder.comment("The most Hydra alive at one outpost (posts and reinforcements).")
                .defineInRange("maxPerOutpost", 14, 0, 64);
        MAX_LOADED = builder.comment("The most Hydra in one dimension.")
                .defineInRange("maxLoaded", 80, 0, 1000);
        ALARM = builder.comment("Whether Hydra raises the alarm and calls reinforcements.")
                .define("alarm", true);
        ALARM_TICKS = builder.comment("Ticks the alarm sounds.")
                .defineInRange("alarmTicks", 600, 20, 12000);
        REINFORCEMENTS = builder.comment("Soldiers each wave of reinforcements brings.")
                .defineInRange("reinforcements", 4, 0, 8);
        MAX_ALARMS_PER_DAY = builder.comment("The most waves of reinforcements per outpost per in-game day.")
                .defineInRange("maxAlarmsPerDay", 2, 0, 24);
        SIGHT_RANGE = builder.comment("How far (blocks) a soldier sees.")
                .defineInRange("sightRange", 24, 4, 64);
        SNEAK_FACTOR = builder.comment("How much of his sight a sneaking intruder leaves him.")
                .defineInRange("sneakFactor", 0.5, 0.0, 1.0);
        RIFLE_DAMAGE = builder.comment("Damage of each bolt of a rifleman's burst.")
                .defineInRange("rifleDamage", 4.0, 0.0, 100.0);
        BURST_SHOTS = builder.comment("Bolts in a burst.")
                .defineInRange("burstShots", 3, 1, 10);
        BURST_COOLDOWN = builder.comment("Ticks between two bursts.")
                .defineInRange("burstCooldown", 50, 5, 400);
        SNIPER_DAMAGE = builder.comment("Damage of the sniper's bolt.")
                .defineInRange("sniperDamage", 8.0, 0.0, 100.0);
        SNIPER_RANGE = builder.comment("Range (blocks) of the sniper.")
                .defineInRange("sniperRange", 40, 8, 128);
        SNIPER_AIM_TICKS = builder.comment("Ticks the sniper aims (his red laser shows) before he fires.")
                .defineInRange("sniperAimTicks", 30, 0, 200);
        BATON_DAMAGE = builder.comment("Damage of the enforcer's stun baton.")
                .defineInRange("batonDamage", 6.0, 0.0, 100.0);
        PISTOL_DAMAGE = builder.comment("Damage of the officer's energy pistol.")
                .defineInRange("pistolDamage", 3.0, 0.0, 100.0);
        BOLT_SPEED = builder.comment("Speed (blocks per tick) of a Hydra bolt.")
                .defineInRange("boltSpeed", 2.2, 0.2, 8.0);
        SCEPTER_TURNS = builder.comment("Whether a hit of Loki's Scepter turns a Hydra soldier to the hitter's side.")
                .define("scepterTurns", true);
        SCEPTER_TURN_TICKS = builder.comment("Ticks a turned soldier fights for the hitter (an officer half).")
                .defineInRange("scepterTurnTicks", 1200, 20, 24000);
        BLASTER_ENABLED = builder.comment("Whether players can use the Hydra blaster.")
                .define("blasterEnabled", true);
        BLASTER_DAMAGE = builder.comment("Damage of the Hydra blaster's bolt.")
                .defineInRange("blasterDamage", 5.0, 0.0, 100.0);
        BLASTER_COOLDOWN = builder.comment("Cooldown (ticks) of the Hydra blaster.")
                .defineInRange("blasterCooldown", 20, 0, 1200);
        BLASTER_DROP = builder.comment("Chance an officer drops a Hydra blaster.")
                .defineInRange("blasterDrop", 0.1, 0.0, 1.0);
        INTEL_MAP_DROP = builder.comment("Chance an officer drops Hydra intel (a map to an Avengers Tower).")
                .defineInRange("intelMapDrop", 0.25, 0.0, 1.0);
        ENERGY_CELL_DROP = builder.comment("Chance a rifleman drops an energy cell.")
                .defineInRange("energyCellDrop", 0.2, 0.0, 1.0);
        builder.pop();
    }

    private HydraConfig() {
    }
}
