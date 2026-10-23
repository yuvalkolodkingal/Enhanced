package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The fight against Hela in the throne room of Asgard (3.5). Every option of the plan is defined here by the 3.5
 * skeleton (lane L4 reads them); {@code EnhancedConfig} calls {@link #define} while it builds the server config (TOML
 * section {@code [hela_boss]}). They are read from the server and synced to clients, and read when used (never cached),
 * so an edit applies at once.
 */
public final class HelaBossConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "hela_boss";

    /** Assigned by {@link #define} when the config is built, at mod construction: before anything reads them. */
    public static ConfigSpec.BooleanValue SUMMON_ENABLED;
    public static ConfigSpec.DoubleValue HEALTH;
    public static ConfigSpec.DoubleValue HEALTH_PER_EXTRA_FIGHTER;
    public static ConfigSpec.DoubleValue ARMOR;
    public static ConfigSpec.DoubleValue PHASE2_AT;
    public static ConfigSpec.DoubleValue PHASE3_AT;
    public static ConfigSpec.DoubleValue VOLLEY_DAMAGE;
    public static ConfigSpec.DoubleValue WALL_DAMAGE;
    public static ConfigSpec.DoubleValue RAIN_DAMAGE;
    public static ConfigSpec.DoubleValue MELEE_DAMAGE;
    public static ConfigSpec.DoubleValue STORM_DAMAGE;
    public static ConfigSpec.IntValue CHANNEL_SECONDS;
    public static ConfigSpec.IntValue CHANNEL_EVERY;
    public static ConfigSpec.DoubleValue CHANNEL_BREAK_FRACTION;
    public static ConfigSpec.DoubleValue CHANNEL_HEAL;
    public static ConfigSpec.IntValue BERSERKER_WAVE;
    public static ConfigSpec.IntValue BERSERKER_PER_FIGHTER;
    public static ConfigSpec.IntValue BERSERKER_MAX;
    public static ConfigSpec.DoubleValue FENRIS_HEALTH;
    public static ConfigSpec.IntValue REST_TICKS;
    public static ConfigSpec.BooleanValue CATCH_MJOLNIR;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        SUMMON_ENABLED = builder.comment("Whether breaking Hela's Seal calls her (operators can always start the fight by command).")
                .define("summonEnabled", true);
        HEALTH = builder.comment("Hela's health with one fighter.")
                .defineInRange("health", 600.0, 1.0, 100000.0);
        HEALTH_PER_EXTRA_FIGHTER = builder.comment("Extra health for each fighter after the first (0.35: 35% more).")
                .defineInRange("healthPerExtraFighter", 0.35, 0.0, 10.0);
        ARMOR = builder.comment("Hela's armour.")
                .defineInRange("armor", 10.0, 0.0, 30.0);
        PHASE2_AT = builder.comment("The fraction of her health at which the Army of the Dead begins.")
                .defineInRange("phase2At", 0.7, 0.0, 1.0);
        PHASE3_AT = builder.comment("The fraction of her health at which the Strength of Asgard begins.")
                .defineInRange("phase3At", 0.35, 0.0, 1.0);
        VOLLEY_DAMAGE = builder.comment("Damage of each necrosword of her volleys.")
                .defineInRange("volleyDamage", 7.0, 0.0, 100.0);
        WALL_DAMAGE = builder.comment("Damage of each spike of her Blade Wall.")
                .defineInRange("wallDamage", 8.0, 0.0, 100.0);
        RAIN_DAMAGE = builder.comment("Damage of each blade of her Blade Rain.")
                .defineInRange("rainDamage", 6.0, 0.0, 100.0);
        MELEE_DAMAGE = builder.comment("Damage of each blow of her blade combo.")
                .defineInRange("meleeDamage", 9.0, 0.0, 100.0);
        STORM_DAMAGE = builder.comment("Damage of her Blade Storm.")
                .defineInRange("stormDamage", 10.0, 0.0, 100.0);
        CHANNEL_SECONDS = builder.comment("How long (seconds) she channels the Strength of Asgard.")
                .defineInRange("channelSeconds", 4, 1, 30);
        CHANNEL_EVERY = builder.comment("How often (seconds) she channels in the third phase.")
                .defineInRange("channelEvery", 20, 5, 600);
        CHANNEL_BREAK_FRACTION = builder.comment("The fraction of her health the fighters must deal during a channel to break it.")
                .defineInRange("channelBreakFraction", 0.06, 0.0, 1.0);
        CHANNEL_HEAL = builder.comment("The fraction of her health an unbroken channel heals.")
                .defineInRange("channelHeal", 0.1, 0.0, 1.0);
        BERSERKER_WAVE = builder.comment("Berserkers in a wave with one fighter.")
                .defineInRange("berserkerWave", 4, 1, 20);
        BERSERKER_PER_FIGHTER = builder.comment("Extra berserkers in a wave for each fighter after the first.")
                .defineInRange("berserkerPerFighter", 2, 0, 10);
        BERSERKER_MAX = builder.comment("The most berserkers alive at once.")
                .defineInRange("berserkerMax", 10, 1, 40);
        FENRIS_HEALTH = builder.comment("The health of Fenris.")
                .defineInRange("fenrisHealth", 140.0, 1.0, 10000.0);
        REST_TICKS = builder.comment("Ticks before Hela's Seal can be broken again after a fight.")
                .defineInRange("restTicks", 24000, 0, 720000);
        CATCH_MJOLNIR = builder.comment("Whether Hela catches a thrown Mjolnir and shatters it (at most once a minute, from the "
                        + "second phase).")
                .define("catchMjolnir", true);
        builder.pop();
    }

    private HelaBossConfig() {
    }
}
