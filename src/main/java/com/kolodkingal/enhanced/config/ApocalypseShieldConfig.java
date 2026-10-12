package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * The numbers of Apocalypse's shield, the boss's and the hero's: which heroes' mind power wears it down and by how
 * much. Its options are defined in {@link #define}, which {@code EnhancedConfig} calls while it builds the server config
 * (TOML section {@code [apocalypse_shield]}).
 *
 * <p>The rule, in Eilon's words: the shield stops everything except the mind power of Wanda and of the Phoenix. "Mind
 * power" is a damage type in the data-driven tag {@code enhanced:mind_power} whose causing entity is a player whose power
 * is in {@link #MIND_POWER_HEROES}; a server that wants Billy's or Vision's magic to count adds their name here and their
 * damage types to the tag.
 */
public final class ApocalypseShieldConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "apocalypse_shield";

    public static ModConfigSpec.ConfigValue<List<? extends String>> MIND_POWER_HEROES;
    public static ModConfigSpec.DoubleValue CHAOS_BEAM_FACTOR;
    public static ModConfigSpec.DoubleValue SLAM_FACTOR;
    public static ModConfigSpec.DoubleValue PHOENIX_BURST_FACTOR;
    public static ModConfigSpec.DoubleValue OTHER_MIND_FACTOR;
    public static ModConfigSpec.DoubleValue PHOENIX_MIND_CONTROL_STRAIN;
    public static ModConfigSpec.DoubleValue PHOENIX_PSIONIC_STRAIN;
    public static ModConfigSpec.IntValue SHIELD_REGEN_DELAY_TICKS;
    public static ModConfigSpec.DoubleValue SHIELD_REGEN_FRACTION;
    public static ModConfigSpec.IntValue MIND_HIT_COOLDOWN_TICKS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        MIND_POWER_HEROES = builder.comment("Heroes whose mind power can wear down Apocalypse's shield (the boss's and the hero's).",
                        "Add \"billy\" or \"vision\" to count theirs (and add their damage types to the datapack tag enhanced:mind_power).")
                .defineListAllowEmpty("mindPowerHeroes", List.of("scarlet_witch", "phoenix"), () -> "scarlet_witch",
                        element -> element instanceof String);
        CHAOS_BEAM_FACTOR = builder.comment("Shield strength removed per point of Chaos Beam damage.")
                .defineInRange("chaosBeamFactor", 1.0, 0.0, 20.0);
        SLAM_FACTOR = builder.comment("Shield strength removed per point of telekinetic slam damage (a thing thrown by Wanda or the Phoenix).")
                .defineInRange("slamFactor", 1.0, 0.0, 20.0);
        PHOENIX_BURST_FACTOR = builder.comment("Shield strength removed per point of Phoenix Burst damage (a full Burst of 30 removes 150).")
                .defineInRange("phoenixBurstFactor", 5.0, 0.0, 50.0);
        OTHER_MIND_FACTOR = builder.comment("Shield strength removed per point of any other damage type in enhanced:mind_power.")
                .defineInRange("otherMindFactor", 1.0, 0.0, 20.0);
        PHOENIX_MIND_CONTROL_STRAIN = builder.comment("Shield strength a Phoenix's Mind Control (even refused) removes.")
                .defineInRange("phoenixMindControlStrain", 25.0, 0.0, 1000.0);
        PHOENIX_PSIONIC_STRAIN = builder.comment("Shield strength a Phoenix's Psionic Field that reaches him removes.")
                .defineInRange("phoenixPsionicStrain", 60.0, 0.0, 1000.0);
        SHIELD_REGEN_DELAY_TICKS = builder.comment("The boss's shield regenerates only after this many ticks without mind-power damage.")
                .defineInRange("shieldRegenDelayTicks", 160, 0, 2400);
        SHIELD_REGEN_FRACTION = builder.comment("The fraction of its strength the boss's shield regains each second once it regenerates.")
                .defineInRange("shieldRegenFraction", 0.03, 0.0, 1.0);
        MIND_HIT_COOLDOWN_TICKS = builder.comment("Ticks between two mind-power hits from the same attacker that the shield will count",
                        "(the Chaos Beam hits every tick, and a shield leaves no invulnerability frames, so it keeps its own).")
                .defineInRange("mindHitCooldownTicks", 10, 1, 40);
        builder.pop();
    }

    private ApocalypseShieldConfig() {
    }
}
