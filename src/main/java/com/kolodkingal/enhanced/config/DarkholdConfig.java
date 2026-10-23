package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigSpec;

/**
 * The numbers of the Darkhold (3.2): Agatha's book. Its options are defined in {@link #define}, which {@code
 * EnhancedConfig} calls while it builds the server config (TOML section {@code [darkhold]}); they are read from the
 * server and synced to clients. Ticks are 1/20 s; "exhaustion" is Minecraft's hunger currency (4.0 of it is one food
 * point); ranges are in blocks. Every option is read when it is used, so editing the file changes it live.
 */
public final class DarkholdConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "darkhold";

    /** Who the retaliation of the book poisons. */
    public enum PoisonTarget {
        /** Whoever used the magic on the holder (the default). */
        ATTACKER,
        /** The holder themselves: the book takes its price for the magic it drew. */
        HOLDER
    }

    // ---- the book ----
    public static ConfigSpec.BooleanValue ENABLED;
    public static ConfigSpec.BooleanValue ANYONE_CAN_USE;

    // ---- the purple beam (hold use) ----
    public static ConfigSpec.DoubleValue BEAM_DAMAGE_PER_SECOND;
    public static ConfigSpec.IntValue BEAM_RANGE;
    public static ConfigSpec.IntValue BEAM_INTERVAL;
    public static ConfigSpec.DoubleValue BEAM_EXHAUSTION_PER_SECOND;

    // ---- the retaliation ----
    public static ConfigSpec.EnumValue<PoisonTarget> POISON_TARGET;
    public static ConfigSpec.IntValue POISON_TICKS;
    public static ConfigSpec.IntValue POISON_AMPLIFIER;
    public static ConfigSpec.IntValue POISON_COOLDOWN_TICKS;

    // ---- the hex mark (Sneak + use on a creature) ----
    public static ConfigSpec.IntValue MARK_RANGE;
    public static ConfigSpec.IntValue MARK_TICKS;
    public static ConfigSpec.IntValue MARK_SLOW_AMPLIFIER;
    public static ConfigSpec.DoubleValue MARK_DAMAGE_BONUS;
    public static ConfigSpec.IntValue MARK_COOLDOWN;
    public static ConfigSpec.DoubleValue MARK_EXHAUSTION;

    // ---- the mind glimpse (Sneak + use at nothing) ----
    public static ConfigSpec.IntValue GLIMPSE_RANGE;
    public static ConfigSpec.IntValue GLIMPSE_TICKS;
    public static ConfigSpec.IntValue GLIMPSE_MAX;
    public static ConfigSpec.IntValue GLIMPSE_COOLDOWN;
    public static ConfigSpec.DoubleValue GLIMPSE_EXHAUSTION;

    public static void define(ConfigSpec.Builder builder) {
        builder.comment("The Darkhold, Agatha's book: a purple beam, a retaliation against magic used on its holder, a hex mark and a",
                "mind glimpse. Its abilities need the book in hand and do not work in the Witches' Road.").push(SECTION);
        ENABLED = builder.comment("The Darkhold works (false: it can be held but does nothing).")
                .define("enabled", true);
        ANYONE_CAN_USE = builder.comment("Anyone can use the Darkhold, not only the sorcerers (Loki, the Scarlet Witch and Billy).")
                .define("anyoneCanUse", false);

        BEAM_DAMAGE_PER_SECOND = builder.comment("Damage a second of the purple beam does to what the crosshair is on (8 = four hearts).")
                .defineInRange("beamDamagePerSecond", 8.0, 0.0, 200.0);
        BEAM_RANGE = builder.comment("How far (blocks) the purple beam reaches.")
                .defineInRange("beamRange", 24, 4, 128);
        BEAM_INTERVAL = builder.comment("Ticks between two hits of the purple beam (5 = four hits a second). The beam ignores the creature's",
                        "damage cooldown, so every hit lands.")
                .defineInRange("beamInterval", 5, 1, 40);
        BEAM_EXHAUSTION_PER_SECOND = builder.comment("Food exhaustion a second of the purple beam costs (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("beamExhaustionPerSecond", 1.0, 0.0, 40.0);

        POISON_TARGET = builder.comment("Who is poisoned when magic is used on the holder of the Darkhold: ATTACKER (whoever used it, the",
                        "default) or HOLDER (the holder pays for the magic the book drew).")
                .defineEnum("poisonTarget", PoisonTarget.ATTACKER);
        POISON_TICKS = builder.comment("How long the poison lasts, in ticks (200 = 10 seconds).")
                .defineInRange("poisonTicks", 200, 20, 72000);
        POISON_AMPLIFIER = builder.comment("The poison's level (0 = Poison I).")
                .defineInRange("poisonAmplifier", 0, 0, 9);
        POISON_COOLDOWN_TICKS = builder.comment("Ticks before the same attacker can be poisoned again, so a beam does not renew it every tick.")
                .defineInRange("poisonCooldownTicks", 40, 0, 72000);

        MARK_RANGE = builder.comment("How far (blocks) the hex mark reaches (Sneak + use on the creature you look at).")
                .defineInRange("markRange", 24, 2, 128);
        MARK_TICKS = builder.comment("How long a hex mark lasts, in ticks (200 = 10 seconds): the creature glows, is slowed and takes extra",
                        "damage from the one who marked it.")
                .defineInRange("markTicks", 200, 20, 72000);
        MARK_SLOW_AMPLIFIER = builder.comment("The Slowness of a marked creature (0 = Slowness I).")
                .defineInRange("markSlowAmplifier", 1, 0, 9);
        MARK_DAMAGE_BONUS = builder.comment("Extra damage the marker does to a marked creature (0.5 = half as much again).")
                .defineInRange("markDamageBonus", 0.5, 0.0, 10.0);
        MARK_COOLDOWN = builder.comment("Cooldown of the hex mark, in ticks.")
                .defineInRange("markCooldown", 100, 0, 72000);
        MARK_EXHAUSTION = builder.comment("Food exhaustion of a hex mark (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("markExhaustion", 2.0, 0.0, 40.0);

        GLIMPSE_RANGE = builder.comment("How far (blocks) the mind glimpse sees (Sneak + use while looking at no creature).")
                .defineInRange("glimpseRange", 24, 2, 128);
        GLIMPSE_TICKS = builder.comment("How long the creatures glow after a mind glimpse, in ticks (100 = 5 seconds).")
                .defineInRange("glimpseTicks", 100, 20, 72000);
        GLIMPSE_MAX = builder.comment("The most creatures one mind glimpse shows (the nearest ones).")
                .defineInRange("glimpseMax", 16, 1, 64);
        GLIMPSE_COOLDOWN = builder.comment("Cooldown of the mind glimpse, in ticks.")
                .defineInRange("glimpseCooldown", 200, 0, 72000);
        GLIMPSE_EXHAUSTION = builder.comment("Food exhaustion of a mind glimpse (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("glimpseExhaustion", 1.0, 0.0, 40.0);
        builder.pop();
    }

    private DarkholdConfig() {
    }
}
