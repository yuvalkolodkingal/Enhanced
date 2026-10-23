package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The healing factor (3.7) of Wolverine, Deadpool and a custom hero with their body rows: regeneration, the DOWN state
 * that replaces a death, the rise, and the only things that defeat a healer. Every option of the plan is defined here
 * by the 3.7 skeleton (lane L1 reads them); {@code EnhancedConfig} calls {@link #define} while it builds the server
 * config (TOML section {@code [healing_factor]}). They are read from the server and read when used (never cached), so
 * an edit applies at once.
 */
public final class HealingConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "healing_factor";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.BooleanValue CUSTOM_HEROES;
    public static ConfigSpec.DoubleValue REGEN_PER_SECOND;
    public static ConfigSpec.DoubleValue REGEN_OUT_OF_COMBAT;
    public static ConfigSpec.IntValue OUT_OF_COMBAT_TICKS;
    public static ConfigSpec.DoubleValue EXHAUSTION_PER_HEALTH;
    public static ConfigSpec.DoubleValue SHRUG_FACTOR;
    public static ConfigSpec.IntValue DOWN_TICKS;
    public static ConfigSpec.DoubleValue RISE_HEALTH;
    public static ConfigSpec.IntValue RISE_RESISTANCE_TICKS;
    public static ConfigSpec.BooleanValue DOWN_MOBS_FORGET;
    public static ConfigSpec.IntValue RESCUE_RADIUS;
    public static ConfigSpec.BooleanValue STONES_DEFEAT;
    public static ConfigSpec.BooleanValue CHAOS_DEFEATS;
    public static ConfigSpec.BooleanValue ODIN_DEFEATS;
    public static ConfigSpec.BooleanValue PHOENIX_DEFEATS;
    public static ConfigSpec.BooleanValue DARKHOLD_DEFEATS;
    public static ConfigSpec.BooleanValue VORMIR_DEFEATS;
    public static ConfigSpec.BooleanValue POISON_STOPS_HEALING;
    public static ConfigSpec.DoubleValue BURST_FRACTION;
    public static ConfigSpec.IntValue BURST_COOLDOWN;
    public static ConfigSpec.BooleanValue TIP_ONCE;

    public static void define(ConfigSpec.Builder builder) {
        builder.push("healing_factor");
        ENABLED = builder.comment("Whether the healing factor works at all (off: Wolverine and Deadpool die as",
                "anyone).")
                .define("enabled", true);
        CUSTOM_HEROES = builder.comment("Whether a custom hero with Wolverine's or Deadpool's body row has the",
                "healing factor.")
                .define("customHeroes", true);
        REGEN_PER_SECOND = builder.comment("Health healed per second, always (also in a fight).")
                .defineInRange("regenPerSecond", 1.0, 0.0, 20.0);
        REGEN_OUT_OF_COMBAT = builder.comment("Health healed per second after outOfCombatTicks without taking damage.")
                .defineInRange("regenOutOfCombat", 3.0, 0.0, 20.0);
        OUT_OF_COMBAT_TICKS = builder.comment("Ticks without damage before the faster regeneration.")
                .defineInRange("outOfCombatTicks", 60, 0, 1200);
        EXHAUSTION_PER_HEALTH = builder.comment("Food exhaustion per health healed (at food 0 the regeneration is a",
                "quarter, never zero).")
                .defineInRange("exhaustionPerHealth", 0.05, 0.0, 4.0);
        SHRUG_FACTOR = builder.comment("Harmful effects (not Poison, not the mod's spells) last this fraction of",
                "their time on a healer.")
                .defineInRange("shrugFactor", 0.5, 0.0, 1.0);
        DOWN_TICKS = builder.comment("Ticks a healer stays DOWN (kneeling, untouchable) after a blow that would have",
                "killed him.")
                .defineInRange("downTicks", 100, 20, 600);
        RISE_HEALTH = builder.comment("Health he rises with.")
                .defineInRange("riseHealth", 8.0, 1.0, 40.0);
        RISE_RESISTANCE_TICKS = builder.comment("Ticks of Resistance I after he rises.")
                .defineInRange("riseResistanceTicks", 60, 0, 600);
        DOWN_MOBS_FORGET = builder.comment("Whether monsters lose interest in a healer who is DOWN.")
                .define("downMobsForget", true);
        RESCUE_RADIUS = builder.comment("Radius (blocks) searched for dry, safe ground when he rises in water, lava,",
                "fire or a wall.")
                .defineInRange("rescueRadius", 32, 4, 128);
        STONES_DEFEAT = builder.comment("Whether the Infinity Stones defeat a healer (damage types in",
                "#enhanced:defeats_healing/stones).")
                .define("stonesDefeat", true);
        CHAOS_DEFEATS = builder.comment("Whether chaos magic (the Scarlet Witch, Billy) defeats a healer",
                "(#enhanced:defeats_healing/chaos).")
                .define("chaosDefeats", true);
        ODIN_DEFEATS = builder.comment("Whether the Allfather's power (the Odinforce, Gungnir) defeats a healer",
                "(#enhanced:defeats_healing/odin).")
                .define("odinDefeats", true);
        PHOENIX_DEFEATS = builder.comment("Whether the Phoenix defeats a healer (#enhanced:defeats_healing/phoenix).")
                .define("phoenixDefeats", true);
        DARKHOLD_DEFEATS = builder.comment("Whether the Darkhold's dark magic (Agatha) defeats a healer",
                "(#enhanced:defeats_healing/darkhold).")
                .define("darkholdDefeats", false);
        VORMIR_DEFEATS = builder.comment("Whether a healer pushed off the Vormir Cliff and sacrificed dies (the Soul",
                "Stone's own magic).")
                .define("vormirDefeats", true);
        POISON_STOPS_HEALING = builder.comment("Whether Poison (the effects in #enhanced:stops_healing) switches the",
                "healing off while it lasts.")
                .define("poisonStopsHealing", true);
        BURST_FRACTION = builder.comment("Fraction of his max health the Healing Burst heals at once.")
                .defineInRange("burstFraction", 0.5, 0.0, 1.0);
        BURST_COOLDOWN = builder.comment("Cooldown (ticks) of the Healing Burst.")
                .defineInRange("burstCooldown", 400, 0, 72000);
        TIP_ONCE = builder.comment("Whether the chat tip about the healing factor is shown only at a player's first",
                "DOWN.")
                .define("tipOnce", true);
        builder.pop();
    }

    private HealingConfig() {
    }
}
