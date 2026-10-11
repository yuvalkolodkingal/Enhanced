package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.EnhancedConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The balance numbers of the Phoenix (Jean Grey). Its options are defined in {@link #define}, which {@code
 * EnhancedConfig} calls while it builds the server config (TOML section {@code [phoenix]}); they are read from the
 * server and synced to clients.
 *
 * <p>Times are in ticks (20 to the second) unless the name says seconds; "exhaustion" is Minecraft's hunger currency
 * (4.0 of it is one food point).
 */
public final class PhoenixConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "phoenix";

    // ---- Flight and fire ----
    /**
     * Assigned by {@link #define} when the config is built, which is at mod construction: before anything reads it (it
     * cannot be final because the options are defined in a method, not in a static block).
     */
    public static ModConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ModConfigSpec.BooleanValue FIRE_IMMUNE;

    // ---- Mind Reading (R) and Mind Sense (Sneak + R) ----
    public static ModConfigSpec.IntValue MIND_READ_RANGE;
    public static ModConfigSpec.BooleanValue MIND_READ_PLAYERS;
    public static ModConfigSpec.BooleanValue MIND_READ_INVENTORY;
    public static ModConfigSpec.BooleanValue MIND_READ_NOTIFIES;
    public static ModConfigSpec.DoubleValue MIND_READ_EXHAUSTION;
    public static ModConfigSpec.IntValue MIND_SENSE_RANGE;
    public static ModConfigSpec.IntValue MIND_SENSE_TICKS;
    public static ModConfigSpec.IntValue MIND_SENSE_COOLDOWN;
    public static ModConfigSpec.DoubleValue MIND_SENSE_EXHAUSTION;

    // ---- Mind Control (B) and the Psionic Field (Sneak + B) ----
    public static ModConfigSpec.IntValue MIND_CONTROL_RANGE;
    public static ModConfigSpec.IntValue MIND_CONTROL_TICKS;
    public static ModConfigSpec.IntValue MIND_CONTROL_MAX;
    public static ModConfigSpec.IntValue MIND_CONTROL_PLAYER_TICKS;
    public static ModConfigSpec.IntValue MIND_CONTROL_COOLDOWN;
    public static ModConfigSpec.DoubleValue MIND_CONTROL_EXHAUSTION;
    public static ModConfigSpec.IntValue PSIONIC_RADIUS;
    public static ModConfigSpec.IntValue PSIONIC_TICKS;
    public static ModConfigSpec.IntValue PSIONIC_COOLDOWN;
    public static ModConfigSpec.DoubleValue PSIONIC_EXHAUSTION;

    // ---- The Phoenix Burst (N, held) ----
    public static ModConfigSpec.IntValue BURST_CHARGE_TICKS;
    public static ModConfigSpec.IntValue BURST_MIN_CHARGE_TICKS;
    public static ModConfigSpec.IntValue BURST_MIN_RADIUS;
    public static ModConfigSpec.IntValue BURST_MAX_RADIUS;
    public static ModConfigSpec.DoubleValue BURST_MIN_DAMAGE;
    public static ModConfigSpec.DoubleValue BURST_MAX_DAMAGE;
    public static ModConfigSpec.DoubleValue BURST_PLAYER_FACTOR;
    public static ModConfigSpec.DoubleValue BURST_WAVE_SPEED;
    public static ModConfigSpec.IntValue BURST_FIRE_SECONDS;
    public static ModConfigSpec.DoubleValue BURST_KNOCKBACK;
    public static ModConfigSpec.BooleanValue BURST_SETS_FIRE;
    public static ModConfigSpec.BooleanValue BURST_HURTS_PASSIVES;
    public static ModConfigSpec.IntValue BURST_MAX_FIRE_BLOCKS;
    public static ModConfigSpec.IntValue BURST_FOOD_COST;
    public static ModConfigSpec.IntValue BURST_COOLDOWN;
    public static ModConfigSpec.IntValue BURST_CANCEL_COOLDOWN;
    public static ModConfigSpec.IntValue BURST_DRAIN_TICKS;

    // ---- The Phoenix Ember ----
    public static ModConfigSpec.IntValue EMBER_RITUAL_TICKS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push(SECTION);
        FLIGHT_SPEED = builder.comment("The Phoenix's flying speed (Creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.09, 0.01, 0.5);
        FIRE_IMMUNE = builder.comment("Fire, lava and burning cannot hurt the Phoenix.")
                .define("fireImmune", true);

        MIND_READ_RANGE = builder.comment("How far (blocks) Mind Reading reaches (line of sight needed).")
                .defineInRange("mindReadRange", 24, 4, 64);
        MIND_READ_PLAYERS = builder.comment("If false, Mind Reading cannot read players' minds.")
                .define("mindReadPlayers", true);
        MIND_READ_INVENTORY = builder.comment("If true the panel lists a player's whole inventory; if false only the hands and the armour.")
                .define("mindReadInventory", true);
        MIND_READ_NOTIFIES = builder.comment("The player who is read feels a presence in their mind (once per 10 seconds).")
                .define("mindReadNotifies", true);
        MIND_READ_EXHAUSTION = builder.comment("Food exhaustion per tick while Mind Reading is on (4.0 = 1 food point).")
                .defineInRange("mindReadExhaustion", 0.01, 0.0, 4.0);
        MIND_SENSE_RANGE = builder.comment("How far (blocks) the Mind Sense ping reaches.")
                .defineInRange("mindSenseRange", 48, 8, 128);
        MIND_SENSE_TICKS = builder.comment("How long the sensed minds glow.")
                .defineInRange("mindSenseTicks", 120, 20, 1200);
        MIND_SENSE_COOLDOWN = builder.comment("Cooldown of Mind Sense, in ticks (400 = 20 s).")
                .defineInRange("mindSenseCooldown", 400, 0, 72000);
        MIND_SENSE_EXHAUSTION = builder.comment("Exhaustion of one Mind Sense.")
                .defineInRange("mindSenseExhaustion", 2.0, 0.0, 40.0);

        MIND_CONTROL_RANGE = builder.comment("How far (blocks) Mind Control reaches (10 = about 33 feet, as in the film).")
                .defineInRange("mindControlRange", 10, 2, 32);
        MIND_CONTROL_TICKS = builder.comment("How long a creature stays possessed (300 = 15 s).")
                .defineInRange("mindControlTicks", 300, 20, 2400);
        MIND_CONTROL_MAX = builder.comment("How many creatures she can possess at once; taking one more frees the oldest.")
                .defineInRange("mindControlMax", 1, 1, 5);
        MIND_CONTROL_PLAYER_TICKS = builder.comment("How long a player is held by Mind Control (needs PvP). 0 turns it off for players.")
                .defineInRange("mindControlPlayerTicks", 60, 0, 1200);
        MIND_CONTROL_COOLDOWN = builder.comment("Cooldown of Mind Control (ability 4), in ticks.")
                .defineInRange("mindControlCooldown", 400, 0, 72000);
        MIND_CONTROL_EXHAUSTION = builder.comment("Exhaustion of Mind Control.")
                .defineInRange("mindControlExhaustion", 6.0, 0.0, 40.0);
        PSIONIC_RADIUS = builder.comment("Radius (blocks) of the Psionic Field.")
                .defineInRange("psionicRadius", 16, 4, 64);
        PSIONIC_TICKS = builder.comment("How long the Psionic Field holds everyone (needs PvP for players).")
                .defineInRange("psionicTicks", 60, 10, 600);
        PSIONIC_COOLDOWN = builder.comment("Cooldown of the Psionic Field (ability 4), in ticks (1200 = 60 s).")
                .defineInRange("psionicCooldown", 1200, 0, 72000);
        PSIONIC_EXHAUSTION = builder.comment("Exhaustion of the Psionic Field.")
                .defineInRange("psionicExhaustion", 8.0, 0.0, 40.0);

        BURST_CHARGE_TICKS = builder.comment("Ticks of charge for a full Phoenix Burst. It fires by itself at full charge.")
                .defineInRange("burstChargeTicks", 60, 10, 200);
        BURST_MIN_CHARGE_TICKS = builder.comment("A shorter charge is cancelled.")
                .defineInRange("burstMinChargeTicks", 12, 0, 100);
        BURST_MIN_RADIUS = builder.comment("Radius (blocks) of a barely charged Burst.")
                .defineInRange("burstMinRadius", 8, 2, 64);
        BURST_MAX_RADIUS = builder.comment("Radius (blocks) of a fully charged Burst.")
                .defineInRange("burstMaxRadius", 24, 4, 96);
        BURST_MIN_DAMAGE = builder.comment("Damage of a barely charged Burst.")
                .defineInRange("burstMinDamage", 8.0, 0.0, 200.0);
        BURST_MAX_DAMAGE = builder.comment("Damage of a fully charged Burst (it fades to 60% at the edge).")
                .defineInRange("burstMaxDamage", 30.0, 0.0, 200.0);
        BURST_PLAYER_FACTOR = builder.comment("What a player takes of the Burst's damage (only with PvP on).")
                .defineInRange("burstPlayerFactor", 0.5, 0.0, 1.0);
        BURST_WAVE_SPEED = builder.comment("How fast the wave spreads (blocks per tick).")
                .defineInRange("burstWaveSpeed", 1.0, 0.2, 4.0);
        BURST_FIRE_SECONDS = builder.comment("Seconds a creature burns after a full Burst (a weaker one burns 40% as long).")
                .defineInRange("burstFireSeconds", 10, 0, 60);
        BURST_KNOCKBACK = builder.comment("Knockback of the wave.")
                .defineInRange("burstKnockback", 1.2, 0.0, 5.0);
        BURST_SETS_FIRE = builder.comment("Whether the wave sets flammable blocks on fire (it also needs mobGriefing).")
                .define("burstSetsFire", true);
        BURST_HURTS_PASSIVES = builder.comment("If true the wave also hurts villagers, golems, animals and other players' pets; "
                        + "by default it hurts only hostile creatures, creatures after her, and players (with PvP on).")
                .define("burstHurtsPassives", false);
        BURST_MAX_FIRE_BLOCKS = builder.comment("At most this many fires per Burst.")
                .defineInRange("burstMaxFireBlocks", 40, 0, 400);
        BURST_FOOD_COST = builder.comment("Food points a full Burst costs (and the least hunger needed to start charging).")
                .defineInRange("burstFoodCost", 6, 0, 20);
        BURST_COOLDOWN = builder.comment("Cooldown of the Burst, in ticks (1800 = 90 s).")
                .defineInRange("burstCooldown", 1800, 0, 72000);
        BURST_CANCEL_COOLDOWN = builder.comment("Cooldown after a cancelled charge, in ticks.")
                .defineInRange("burstCancelCooldown", 40, 0, 1200);
        BURST_DRAIN_TICKS = builder.comment("Weakness and Slowness after a Burst, in ticks.")
                .defineInRange("burstDrainTicks", 100, 0, 1200);

        EMBER_RITUAL_TICKS = builder.comment("How long the Phoenix Ember must be held to awaken, in ticks.")
                .defineInRange("emberRitualTicks", 60, 20, 400);
        builder.pop();
    }

    // ---- As the client reads them (the server's values are synced to it; these defaults hold until they arrive) ----

    /** The ticks of a full charge of the Burst. */
    public static int chargeTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? BURST_CHARGE_TICKS.get() : 60;
    }

    /** How fast the Burst's wave spreads, in blocks a tick. */
    public static double waveSpeed() {
        return EnhancedConfig.SPEC.isLoaded() ? BURST_WAVE_SPEED.get() : 1.0;
    }

    /** The radius of the Psionic Field, in blocks. */
    public static int psionicRadius() {
        return EnhancedConfig.SPEC.isLoaded() ? PSIONIC_RADIUS.get() : 16;
    }

    /** How far Mind Sense's ping reaches, in blocks. */
    public static int senseRange() {
        return EnhancedConfig.SPEC.isLoaded() ? MIND_SENSE_RANGE.get() : 48;
    }

    private PhoenixConfig() {
    }
}
