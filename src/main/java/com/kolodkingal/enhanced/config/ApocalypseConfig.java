package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The balance numbers of Apocalypse, the hero (a player who became En Sabah Nur by the Essence). Its options are
 * defined in {@link #define}, which {@code EnhancedConfig} calls while it builds the server config (TOML section
 * {@code [apocalypse]}); the shield's mind-power rule is shared with the boss, in {@link ApocalypseShieldConfig}.
 */
public final class ApocalypseConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "apocalypse";

    /**
     * Assigned by {@link #define} when the config is built, which is at mod construction: before anything reads it (it
     * cannot be final because the options are defined in a method, not in a static block).
     */
    public static ConfigSpec.DoubleValue FLIGHT_SPEED;
    // the body
    public static ConfigSpec.DoubleValue BODY_HEALTH_BONUS;
    public static ConfigSpec.DoubleValue ARMOR;
    public static ConfigSpec.DoubleValue ARMOR_TOUGHNESS;
    public static ConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static ConfigSpec.DoubleValue REGEN_PER_SECOND;
    public static ConfigSpec.IntValue REGEN_DELAY_TICKS;
    // Sand Control: the Wave and the Entomb
    public static ConfigSpec.DoubleValue WAVE_DAMAGE;
    public static ConfigSpec.IntValue WAVE_LENGTH;
    public static ConfigSpec.DoubleValue WAVE_SPEED;
    public static ConfigSpec.IntValue WAVE_SLOW_TICKS;
    public static ConfigSpec.IntValue WAVE_COOLDOWN;
    public static ConfigSpec.DoubleValue WAVE_EXHAUSTION;
    public static ConfigSpec.IntValue ENTOMB_RANGE;
    public static ConfigSpec.IntValue ENTOMB_TICKS;
    public static ConfigSpec.DoubleValue ENTOMB_DAMAGE_PER_SECOND;
    public static ConfigSpec.DoubleValue COCOON_HEALTH;
    public static ConfigSpec.IntValue ENTOMB_COOLDOWN;
    public static ConfigSpec.DoubleValue ENTOMB_EXHAUSTION;
    // Portal
    public static ConfigSpec.IntValue PORTAL_RANGE;
    public static ConfigSpec.IntValue PORTAL_ALLY_RADIUS;
    public static ConfigSpec.IntValue PORTAL_COOLDOWN;
    public static ConfigSpec.DoubleValue PORTAL_EXHAUSTION;
    // Apocalypse's Shield
    public static ConfigSpec.DoubleValue SHIELD_RADIUS;
    public static ConfigSpec.IntValue SHIELD_WIND_UP_TICKS;
    public static ConfigSpec.IntValue SHIELD_MAX_TICKS;
    public static ConfigSpec.DoubleValue SHIELD_STRENGTH;
    public static ConfigSpec.DoubleValue SHIELD_OTHER_WEAR;
    public static ConfigSpec.IntValue SHIELD_COOLDOWN;
    public static ConfigSpec.IntValue SHIELD_BROKEN_COOLDOWN;
    public static ConfigSpec.DoubleValue SHIELD_EXHAUSTION;
    // Power Bestowal and the Sandstorm
    public static ConfigSpec.IntValue BESTOW_RANGE;
    public static ConfigSpec.IntValue BESTOW_TICKS;
    public static ConfigSpec.IntValue BESTOW_MAX;
    public static ConfigSpec.IntValue BESTOW_COOLDOWN;
    public static ConfigSpec.DoubleValue BESTOW_EXHAUSTION;
    public static ConfigSpec.DoubleValue EMPOWER_COOLDOWN_BOOST;
    public static ConfigSpec.IntValue STORM_RADIUS;
    public static ConfigSpec.IntValue STORM_TICKS;
    public static ConfigSpec.DoubleValue STORM_DAMAGE;
    public static ConfigSpec.IntValue STORM_COOLDOWN;
    public static ConfigSpec.DoubleValue STORM_EXHAUSTION;
    // the Essence
    public static ConfigSpec.IntValue ESSENCE_RITUAL_TICKS;

    public static void define(ConfigSpec.Builder builder) {
        builder.push(SECTION);
        FLIGHT_SPEED = builder.comment("Apocalypse's levitation speed (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.08, 0.01, 0.5);

        BODY_HEALTH_BONUS = builder.comment("Extra max health (20 = 10 hearts, so 40).")
                .defineInRange("bodyHealthBonus", 20.0, 0.0, 200.0);
        ARMOR = builder.comment("Armour while Apocalypse.")
                .defineInRange("armor", 10.0, 0.0, 30.0);
        ARMOR_TOUGHNESS = builder.comment("Armour toughness while Apocalypse.")
                .defineInRange("armorToughness", 4.0, 0.0, 20.0);
        KNOCKBACK_RESISTANCE = builder.comment("Knockback resistance while Apocalypse.")
                .defineInRange("knockbackResistance", 0.5, 0.0, 1.0);
        REGEN_PER_SECOND = builder.comment("Healing factor: health a second after regenDelayTicks without damage. 0 turns it off.")
                .defineInRange("regenPerSecond", 1.0, 0.0, 20.0);
        REGEN_DELAY_TICKS = builder.comment("Ticks without damage before the healing factor starts.")
                .defineInRange("regenDelayTicks", 100, 0, 1200);

        WAVE_DAMAGE = builder.comment("The Sand Wave.")
                .defineInRange("waveDamage", 8.0, 0.0, 100.0);
        WAVE_LENGTH = builder.comment("Blocks.")
                .defineInRange("waveLength", 18, 4, 48);
        WAVE_SPEED = builder.comment("Blocks per tick.")
                .defineInRange("waveSpeed", 0.9, 0.2, 3.0);
        WAVE_SLOW_TICKS = builder.comment("Slowness II duration.")
                .defineInRange("waveSlowTicks", 40, 0, 600);
        WAVE_COOLDOWN = builder.comment("Ticks.")
                .defineInRange("waveCooldown", 60, 0, 72000);
        WAVE_EXHAUSTION = builder.comment("Hunger the Sand Wave costs.")
                .defineInRange("waveExhaustion", 1.0, 0.0, 40.0);
        ENTOMB_RANGE = builder.comment("Blocks.")
                .defineInRange("entombRange", 24, 4, 64);
        ENTOMB_TICKS = builder.comment("How long the Entomb holds.")
                .defineInRange("entombTicks", 80, 10, 1200);
        ENTOMB_DAMAGE_PER_SECOND = builder.comment("Damage while entombed (never below 2 hearts).")
                .defineInRange("entombDamagePerSecond", 1.0, 0.0, 20.0);
        COCOON_HEALTH = builder.comment("Health of the cocoon (a friend breaks it by hitting it).")
                .defineInRange("cocoonHealth", 16.0, 1.0, 200.0);
        ENTOMB_COOLDOWN = builder.comment("Ticks.")
                .defineInRange("entombCooldown", 240, 0, 72000);
        ENTOMB_EXHAUSTION = builder.comment("Hunger the Entomb costs.")
                .defineInRange("entombExhaustion", 3.0, 0.0, 40.0);

        PORTAL_RANGE = builder.comment("Blocks.")
                .defineInRange("portalRange", 40, 4, 128);
        PORTAL_ALLY_RADIUS = builder.comment("Pets, teammates and sneaking players within this come along.")
                .defineInRange("portalAllyRadius", 3, 0, 16);
        PORTAL_COOLDOWN = builder.comment("Ticks.")
                .defineInRange("portalCooldown", 160, 0, 72000);
        PORTAL_EXHAUSTION = builder.comment("Hunger the Portal costs.")
                .defineInRange("portalExhaustion", 2.0, 0.0, 40.0);

        SHIELD_RADIUS = builder.comment("Drawn radius of the sphere (and how close a projectile is neutralised).")
                .defineInRange("shieldRadius", 2.4, 1.0, 6.0);
        SHIELD_WIND_UP_TICKS = builder.comment("The shield stops nothing until it is complete.")
                .defineInRange("shieldWindUpTicks", 10, 0, 200);
        SHIELD_MAX_TICKS = builder.comment("How long a complete shield can be held.")
                .defineInRange("shieldMaxTicks", 120, 1, 6000);
        SHIELD_STRENGTH = builder.comment("How much mind power it takes to break it.")
                .defineInRange("shieldStrength", 60.0, 1.0, 1000.0);
        SHIELD_OTHER_WEAR = builder.comment("The part of any other damage it stops that wears it down (0 = it costs nothing, the literal rule;",
                        "the boss's shield always uses 0).")
                .defineInRange("shieldOtherWear", 0.5, 0.0, 1.0);
        SHIELD_COOLDOWN = builder.comment("Ticks after a drop.")
                .defineInRange("shieldCooldown", 160, 0, 6000);
        SHIELD_BROKEN_COOLDOWN = builder.comment("Ticks after it shattered or was held to its limit.")
                .defineInRange("shieldBrokenCooldown", 400, 0, 6000);
        SHIELD_EXHAUSTION = builder.comment("Hunger per tick while held.")
                .defineInRange("shieldExhaustion", 0.1, 0.0, 4.0);

        BESTOW_RANGE = builder.comment("Blocks.")
                .defineInRange("bestowRange", 24, 4, 64);
        BESTOW_TICKS = builder.comment("How long Empowered lasts.")
                .defineInRange("bestowTicks", 1200, 20, 72000);
        BESTOW_MAX = builder.comment("Allies empowered at once.")
                .defineInRange("bestowMax", 2, 1, 8);
        BESTOW_COOLDOWN = builder.comment("Ticks.")
                .defineInRange("bestowCooldown", 400, 0, 72000);
        BESTOW_EXHAUSTION = builder.comment("Hunger Power Bestowal costs.")
                .defineInRange("bestowExhaustion", 6.0, 0.0, 40.0);
        EMPOWER_COOLDOWN_BOOST = builder.comment("A hero's ability cooldowns recharge this many times as fast while Empowered.")
                .defineInRange("empowerCooldownBoost", 1.5, 1.0, 5.0);
        STORM_RADIUS = builder.comment("Blocks.")
                .defineInRange("stormRadius", 7, 2, 32);
        STORM_TICKS = builder.comment("How long the Sandstorm lasts.")
                .defineInRange("stormTicks", 120, 20, 1200);
        STORM_DAMAGE = builder.comment("Every 10 ticks.")
                .defineInRange("stormDamage", 2.0, 0.0, 100.0);
        STORM_COOLDOWN = builder.comment("Ticks.")
                .defineInRange("stormCooldown", 600, 0, 72000);
        STORM_EXHAUSTION = builder.comment("Hunger the Sandstorm costs.")
                .defineInRange("stormExhaustion", 8.0, 0.0, 40.0);

        ESSENCE_RITUAL_TICKS = builder.comment("How long the Essence must be held to awaken (the shared AwakeningItem; the Ember and the Gauntlet default to 60 too).")
                .defineInRange("essenceRitualTicks", 60, 20, 400);
        builder.pop();
    }

    private ApocalypseConfig() {
    }
}
