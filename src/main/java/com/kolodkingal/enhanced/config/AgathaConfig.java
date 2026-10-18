package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The numbers of Agatha Harkness (3.3), the hero a player with no power becomes by the Darkhold. Its options are defined
 * in {@link #define}, which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code
 * [agatha]}); they are read from the server and synced to clients. Ticks are 1/20 s; "exhaustion" is Minecraft's hunger
 * currency (4.0 of it is one food point); ranges are in blocks. Every option is read when it is used, so editing the file
 * changes it live. The hex mark, the mind glimpse and the poison are the Darkhold's own, with the numbers of {@code
 * [darkhold]}.
 */
public final class AgathaConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "agatha";

    // ---- becoming Agatha ----
    public static ModConfigSpec.BooleanValue DARKHOLD_AWAKENS;
    public static ModConfigSpec.IntValue AWAKEN_TICKS;

    // ---- the body ----
    public static ModConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue BODY_HEALTH_BONUS;
    public static ModConfigSpec.BooleanValue RETALIATES;

    // ---- R: the Purple Beam ----
    public static ModConfigSpec.DoubleValue BEAM_DAMAGE_PER_SECOND;
    public static ModConfigSpec.IntValue BEAM_RANGE;
    public static ModConfigSpec.IntValue BEAM_INTERVAL;
    public static ModConfigSpec.DoubleValue BEAM_EXHAUSTION_PER_SECOND;

    // ---- V: the Power Drain ----
    public static ModConfigSpec.IntValue DRAIN_RANGE;
    public static ModConfigSpec.IntValue DRAIN_TICKS;
    public static ModConfigSpec.DoubleValue DRAIN_DAMAGE;
    public static ModConfigSpec.DoubleValue DRAIN_HEAL;
    public static ModConfigSpec.IntValue DRAIN_COOLDOWN;
    public static ModConfigSpec.DoubleValue DRAIN_EXHAUSTION;
    public static ModConfigSpec.BooleanValue DRAIN_ONLY_SORCERERS;

    // ---- B: the Witch's Shield ----
    public static ModConfigSpec.DoubleValue SHIELD_RADIUS;
    public static ModConfigSpec.IntValue SHIELD_WIND_UP_TICKS;
    public static ModConfigSpec.IntValue SHIELD_MAX_TICKS;
    public static ModConfigSpec.DoubleValue SHIELD_STRENGTH;
    public static ModConfigSpec.IntValue SHIELD_COOLDOWN;
    public static ModConfigSpec.IntValue SHIELD_BROKEN_COOLDOWN;
    public static ModConfigSpec.DoubleValue SHIELD_EXHAUSTION;

    // ---- N: the Rune Trap ----
    public static ModConfigSpec.IntValue RUNE_RANGE;
    public static ModConfigSpec.DoubleValue RUNE_RADIUS;
    public static ModConfigSpec.IntValue RUNE_TICKS;
    public static ModConfigSpec.DoubleValue RUNE_DAMAGE_PER_SECOND;
    public static ModConfigSpec.IntValue RUNE_COOLDOWN;
    public static ModConfigSpec.DoubleValue RUNE_EXHAUSTION;

    public static void define(ModConfigSpec.Builder builder) {
        builder.comment("Agatha Harkness (3.3): a player with no power who holds use on the Darkhold becomes her. R the Purple Beam",
                "(Sneak: the hex mark or the mind glimpse), G flight, V the Power Drain, B the Witch's Shield (hold), N the Rune Trap.",
                "Magic used on her poisons whoever used it, as the Darkhold does for its holder.").push(SECTION);
        DARKHOLD_AWAKENS = builder.comment("A player with no power who holds use on the Darkhold becomes Agatha (false: the book only",
                        "refuses them, as in 3.2).")
                .define("darkholdAwakens", true);
        AWAKEN_TICKS = builder.comment("How long use is held on the Darkhold to become Agatha, in ticks (60 = 3 seconds). The book is kept.")
                .defineInRange("awakenTicks", 60, 1, 1200);

        FLIGHT_SPEED = builder.comment("Agatha's flying speed (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.085, 0.01, 0.5);
        BODY_HEALTH_BONUS = builder.comment("Extra maximum health while a player is Agatha (20 = ten more hearts: 40 in all).")
                .defineInRange("bodyHealthBonus", 20.0, 0.0, 1000.0);
        RETALIATES = builder.comment("Magic used on Agatha poisons whoever used it, with or without the Darkhold in hand (the numbers and",
                        "the target are the [darkhold] poison options).")
                .define("retaliates", true);

        BEAM_DAMAGE_PER_SECOND = builder.comment("Damage a second of her Purple Beam (R, hold) does to what the crosshair is on (8 = four hearts).")
                .defineInRange("beamDamagePerSecond", 8.0, 0.0, 200.0);
        BEAM_RANGE = builder.comment("How far (blocks) her Purple Beam reaches.")
                .defineInRange("beamRange", 24, 4, 128);
        BEAM_INTERVAL = builder.comment("Ticks between two hits of her Purple Beam (5 = four hits a second).")
                .defineInRange("beamInterval", 5, 1, 40);
        BEAM_EXHAUSTION_PER_SECOND = builder.comment("Food exhaustion a second of her Purple Beam costs (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("beamExhaustionPerSecond", 1.0, 0.0, 40.0);

        DRAIN_RANGE = builder.comment("How far (blocks) the Power Drain (V) reaches: the creature or player under the crosshair.")
                .defineInRange("drainRange", 16, 2, 64);
        DRAIN_TICKS = builder.comment("How long a drained player's power is gone, in ticks (200 = 10 seconds): no ability key works and a",
                        "flier falls. Players are drained only where PvP lets her hurt them.")
                .defineInRange("drainTicks", 200, 20, 72000);
        DRAIN_DAMAGE = builder.comment("Damage the Power Drain does to a creature (its life is drained; a player loses their power instead).")
                .defineInRange("drainDamage", 6.0, 0.0, 200.0);
        DRAIN_HEAL = builder.comment("Health Agatha takes back from every drain (4 = two hearts).")
                .defineInRange("drainHeal", 4.0, 0.0, 200.0);
        DRAIN_COOLDOWN = builder.comment("Cooldown of the Power Drain, in ticks.")
                .defineInRange("drainCooldown", 300, 0, 72000);
        DRAIN_EXHAUSTION = builder.comment("Food exhaustion of a Power Drain (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("drainExhaustion", 2.0, 0.0, 40.0);
        DRAIN_ONLY_SORCERERS = builder.comment("If true (the default), the Power Drain takes only a witch's or sorcerer's power: Loki, the Scarlet",
                        "Witch, Billy, Agatha, and a custom hero with an ability of theirs. Any other hero is refused. If false, it takes any",
                        "hero's power (the rule before 3.5). A creature's life is drained either way.")
                .define("drainOnlySorcerers", true);

        SHIELD_RADIUS = builder.comment("The radius (blocks) of the Witch's Shield (B, hold), the purple sphere round her.")
                .defineInRange("shieldRadius", 1.9, 1.0, 6.0);
        SHIELD_WIND_UP_TICKS = builder.comment("Ticks the shield takes to form after B is pressed; it stops nothing until then.")
                .defineInRange("shieldWindUpTicks", 8, 0, 100);
        SHIELD_MAX_TICKS = builder.comment("How long a complete shield can be held, in ticks (200 = 10 seconds).")
                .defineInRange("shieldMaxTicks", 200, 20, 72000);
        SHIELD_STRENGTH = builder.comment("The shield's strength: every blow it stops takes the damage it would have done from it; at 0 it shatters.")
                .defineInRange("shieldStrength", 40.0, 1.0, 10000.0);
        SHIELD_COOLDOWN = builder.comment("Ticks before the shield can be raised again after it was let go of.")
                .defineInRange("shieldCooldown", 40, 0, 72000);
        SHIELD_BROKEN_COOLDOWN = builder.comment("Ticks before the shield can be raised again after it shattered or was held to its limit.")
                .defineInRange("shieldBrokenCooldown", 200, 0, 72000);
        SHIELD_EXHAUSTION = builder.comment("Food exhaustion every tick the shield is held (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("shieldExhaustion", 0.03, 0.0, 4.0);

        RUNE_RANGE = builder.comment("How far (blocks) she can lay the Rune Trap (N): where she looks, or round her own feet if at nothing.")
                .defineInRange("runeRange", 24, 2, 64);
        RUNE_RADIUS = builder.comment("The radius (blocks) of the circle of runes.")
                .defineInRange("runeRadius", 4.0, 1.0, 16.0);
        RUNE_TICKS = builder.comment("How long the runes burn, in ticks (100 = 5 seconds): what stands in them is bound (Spellbound) and hurt.")
                .defineInRange("runeTicks", 100, 20, 1200);
        RUNE_DAMAGE_PER_SECOND = builder.comment("Damage a second of the runes does to every creature bound in them.")
                .defineInRange("runeDamagePerSecond", 2.0, 0.0, 200.0);
        RUNE_COOLDOWN = builder.comment("Cooldown of the Rune Trap, in ticks.")
                .defineInRange("runeCooldown", 400, 0, 72000);
        RUNE_EXHAUSTION = builder.comment("Food exhaustion of a Rune Trap (4.0 = 1 food point). Creative players pay nothing.")
                .defineInRange("runeExhaustion", 3.0, 0.0, 40.0);
        builder.pop();
    }

    private AgathaConfig() {
    }
}
