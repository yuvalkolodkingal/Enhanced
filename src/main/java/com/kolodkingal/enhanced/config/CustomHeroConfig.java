package com.kolodkingal.enhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * The Hero Creator (3.3): whether it works and for whom, the limits of a custom hero (the power budget, how many powers,
 * the highest level, the skin) and the balance of the thirty power modules. Its options are defined in {@link #define},
 * which {@code EnhancedConfig} calls while it builds the server config (TOML section {@code [custom_hero]}); they are read
 * from the server and synced to clients (the creator's screen shows the server's budget). Every option is read when it
 * is used, so editing the file changes it live; a hero already made keeps its powers, and is judged again by the new
 * limits the next time it is saved. Ticks are 1/20 s; "exhaustion" is Minecraft's hunger currency (4.0 is one food point).
 *
 * <p>The numbers of each power follow its strength (1 to 10, see {@code ModuleStats}); the options here scale them all.
 */
public final class CustomHeroConfig {
    /** The TOML section of these options. */
    public static final String SECTION = "custom_hero";

    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue OPERATORS_ONLY;
    public static ModConfigSpec.BooleanValue ALLOW_CHOOSE_EXISTING;
    public static ModConfigSpec.IntValue BUDGET;
    public static ModConfigSpec.IntValue MAX_POWERS;
    public static ModConfigSpec.IntValue MAX_STRENGTH;
    public static ModConfigSpec.IntValue MAX_SKIN_BYTES;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_POWERS;
    public static ModConfigSpec.IntValue APPLY_COOLDOWN;
    // the balance of the powers
    public static ModConfigSpec.DoubleValue DAMAGE_SCALE;
    public static ModConfigSpec.DoubleValue COOLDOWN_SCALE;
    public static ModConfigSpec.DoubleValue EXHAUSTION_SCALE;
    public static ModConfigSpec.DoubleValue FLIGHT_SPEED;
    public static ModConfigSpec.DoubleValue FORCE_FIELD_STRENGTH;
    public static ModConfigSpec.IntValue CONTROL_SECONDS;
    public static ModConfigSpec.IntValue TELEPORT_RANGE;
    public static ModConfigSpec.IntValue SUPPRESS_SECONDS;
    public static ModConfigSpec.BooleanValue BLOCK_CHANGES;
    public static ModConfigSpec.BooleanValue WEATHER_CHANGES_WORLD;
    // 3.4: the built-in heroes' abilities and the looks
    public static ModConfigSpec.BooleanValue HERO_MODULES;
    public static ModConfigSpec.BooleanValue WORLD_POWERS;
    public static ModConfigSpec.DoubleValue HERO_STRENGTH_SPREAD;
    public static ModConfigSpec.BooleanValue LAYERS;

    public static void define(ModConfigSpec.Builder builder) {
        builder.comment("The Hero Creator: a Creative-only item with which a player becomes any hero of the mod, or makes a hero",
                "of their own (a name, powers from a catalogue of thirty, keys, colours and a skin).").push(SECTION);
        ENABLED = builder.comment("The Hero Creator works (false: its screen says it is switched off, and custom heroes are judged by",
                        "these limits when they are saved).")
                .define("enabled", true);
        OPERATORS_ONLY = builder.comment("Only operators may use the Hero Creator (false: any player in Creative mode, and operators).")
                .define("operatorsOnly", false);
        ALLOW_CHOOSE_EXISTING = builder.comment("The creator can also turn its user into any hero of the mod, in any look, at once.")
                .define("allowChooseExisting", true);
        BUDGET = builder.comment("Power points a custom hero may spend: each power costs 1 to 3 points for each point of its strength",
                        "(flight 2, durability 1, phasing 3...; strength 1 to 10). The screen shows what is left.")
                .defineInRange("budget", 90, 1, 600);
        MAX_POWERS = builder.comment("How many different powers a custom hero may have (an ability of a hero and its Sneak variant are one).")
                .defineInRange("maxPowers", 10, 1, 30);
        MAX_STRENGTH = builder.comment("The highest strength of a power (the editor's slider goes from 1 to this).")
                .defineInRange("maxStrength", 10, 1, 10);
        MAX_SKIN_BYTES = builder.comment("The biggest custom skin (a 64x64 PNG) the server accepts and sends to everyone, in bytes.")
                .defineInRange("maxSkinBytes", 16384, 1024, 24576);
        DISABLED_POWERS = builder.comment("Powers a custom hero may not have on this server (their ids, e.g. \"time\", \"weather\",",
                        "a hero's ability \"thor.lightning\", or all of a hero's \"thor.*\").")
                .defineListAllowEmpty("disabledPowers", List.of(), () -> "weather", element -> element instanceof String);
        APPLY_COOLDOWN = builder.comment("Ticks a player waits between two heroes made or chosen with the creator.")
                .defineInRange("applyCooldownTicks", 40, 0, 1200);

        DAMAGE_SCALE = builder.comment("Multiplies the damage of every custom power.")
                .defineInRange("damageScale", 1.0, 0.0, 10.0);
        COOLDOWN_SCALE = builder.comment("Multiplies the cooldown of every custom power.")
                .defineInRange("cooldownScale", 1.0, 0.1, 10.0);
        EXHAUSTION_SCALE = builder.comment("Multiplies the hunger every custom power costs (0: free).")
                .defineInRange("exhaustionScale", 1.0, 0.0, 10.0);
        FLIGHT_SPEED = builder.comment("Flight's base speed (Creative flight is 0.05); the power's strength (1-10) scales it (see ModuleStats).")
                .defineInRange("flightSpeed", 0.08, 0.01, 0.5);
        FORCE_FIELD_STRENGTH = builder.comment("The base damage a Force Field takes before it breaks; its strength (1-10) scales it (see ModuleStats).")
                .defineInRange("forceFieldStrength", 35.0, 1.0, 1000.0);
        CONTROL_SECONDS = builder.comment("Base seconds Mind Control, Animal Control and Technopathy hold a creature; the power's strength (1-10) scales it",
                        "(see ModuleStats).")
                .defineInRange("controlSeconds", 20, 1, 600);
        TELEPORT_RANGE = builder.comment("Teleportation's base reach, in blocks; its strength (1-10) scales it (see ModuleStats).")
                .defineInRange("teleportRange", 32, 4, 128);
        SUPPRESS_SECONDS = builder.comment("Seconds Power Absorption takes another hero's powers away (PvP only).")
                .defineInRange("suppressSeconds", 8, 1, 120);
        BLOCK_CHANGES = builder.comment("Custom powers may change blocks: the Sonic Scream shatters glass (doors, levers and frozen water",
                        "are always allowed, and every change still asks the claims, spawn protection and boss arenas).")
                .define("blockChanges", false);
        WEATHER_CHANGES_WORLD = builder.comment("Weather Control's \"cycle\" changes the world's weather (false: it only says it cannot).")
                .define("weatherChangesWorld", true);

        HERO_MODULES = builder.comment("A custom hero may take the abilities of the mod's own heroes (Thor's lightning, Agatha's shield...).")
                .define("heroModules", true);
        WORLD_POWERS = builder.comment("A custom hero may take the heroes' world-scale abilities: the Hex, the Bifrost, Thanos's Time Stone,",
                        "Ultron's upload and army, Professor X's Take Over a Body.")
                .define("worldPowers", true);
        HERO_STRENGTH_SPREAD = builder.comment("How much the strength (1-10) of a hero's ability changes it: its damage and body times",
                        "1 + spread x (strength - 5) / 5, its cooldown divided by that (0.5: strength 1 is x0.6, 10 is x1.5; 0: no change).")
                .defineInRange("heroStrengthSpread", 0.5, 0.0, 1.0);
        LAYERS = builder.comment("A custom hero's look may have layers: a cape, a hat, a visor or mask, glowing eyes, an aura.")
                .define("layers", true);
        builder.pop();
    }

    private CustomHeroConfig() {
    }
}
