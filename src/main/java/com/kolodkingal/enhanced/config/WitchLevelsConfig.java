package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * 3.18.1: the witches' levels (TOML section {@code [spellbook_levels]}): the five levels of each spellbook's caster (and
 * of a witch the Demiurge made), the experience that raises them, and the cantrips that fill a witch's empty keys.
 * {@code EnhancedConfig} calls {@link #define} while it builds the server config. Read when used, never cached.
 */
public final class WitchLevelsConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "spellbook_levels";

    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.IntValue MAX_LEVEL;
    public static ConfigSpec.DoubleValue XP_SCALE;
    public static ConfigSpec.IntValue HOURLY_CAP;
    public static ConfigSpec.DoubleValue COVEN_BONUS;
    public static ConfigSpec.BooleanValue DEFAULTS_ENABLED;
    public static ConfigSpec.BooleanValue DEFAULTS_CUSTOM_HEROES;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        ENABLED = builder.comment("Whether the witches' levels are on. Off: every spellbook's caster has all of her book, as before.")
                .define("enabled", true);
        MAX_LEVEL = builder.comment("The highest level a witch reaches on this server (1 to 5).")
                .defineInRange("maxLevel", 5, 1, 5);
        XP_SCALE = builder.comment("Every point of witch experience is multiplied by this (2.0: twice as fast).")
                .defineInRange("xpScale", 1.0, 0.0, 10.0);
        HOURLY_CAP = builder.comment("The most experience an hour from hits and kills (bosses, study and practice are not counted).")
                .defineInRange("hourlyCap", 600, 0, 100000);
        COVEN_BONUS = builder.comment("The bonus to a witch's experience while another caster is within 16 blocks (0.25: a quarter more).")
                .defineInRange("covenBonus", 0.25, 0.0, 4.0);
        builder.push("defaults");
        DEFAULTS_ENABLED = builder.comment("Whether a witch's empty keys get the cantrips (mini shield, blink step, mend, repel, spark, light).")
                .define("enabled", true);
        DEFAULTS_CUSTOM_HEROES = builder.comment("Whether heroes made with the Hero Creator get the cantrips on their empty keys too.")
                .define("customHeroes", false);
        builder.pop();
        builder.pop();
    }

    private WitchLevelsConfig() {
    }
}
