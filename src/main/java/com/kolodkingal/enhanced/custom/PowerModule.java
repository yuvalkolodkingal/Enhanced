package com.kolodkingal.enhanced.custom;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The Hero Creator's catalogue: the thirty most common superpowers of comics and films (the research and its sources are
 * in the lane's notes), each a module a custom hero can pick. A module has a cost in power points for each point of its
 * strength (1 to 10, the slider of the editor: its numbers are {@link ModuleStats}), a list of sub-options (the first is the default) and, for each option, how its key acts: not at all (a passive
 * power, which needs no key), a press, a key held down, or a switch. The order is the catalogue's order on the screen and
 * is never saved (a hero names its powers by {@link #id}), so modules may be inserted anywhere.
 */
public enum PowerModule {
    SUPER_STRENGTH("super_strength", 2, Group.BODY, "minecraft:anvil", opt("punch", Act.PRESS), opt("slam", Act.PRESS), opt("leap", Act.PRESS)),
    DURABILITY("durability", 1, Group.BODY, "minecraft:iron_chestplate", opt("armor", Act.NONE), opt("health", Act.NONE)),
    SUPER_SPEED("super_speed", 2, Group.MOVE, "minecraft:sugar", opt("run", Act.TOGGLE), opt("blitz", Act.PRESS)),
    FLIGHT("flight", 2, Group.MOVE, "minecraft:feather", opt("hover", Act.TOGGLE), opt("jet", Act.TOGGLE), opt("wings", Act.TOGGLE)),
    HEALING("healing", 2, Group.BODY, "minecraft:golden_apple", opt("steady", Act.NONE), opt("burst", Act.PRESS)),
    ENERGY_BLAST("energy_blast", 2, Group.ENERGY, "minecraft:nether_star", opt("beam", Act.HOLD), opt("bolts", Act.PRESS), opt("burst", Act.PRESS)),
    SUPER_SENSES("super_senses", 1, Group.MIND, "minecraft:spyglass", opt("night", Act.NONE), opt("danger", Act.NONE), opt("xray", Act.TOGGLE)),
    TELEPATHY("telepathy", 1, Group.MIND, "minecraft:ender_eye", opt("sense", Act.TOGGLE), opt("read", Act.PRESS)),
    TELEPORT("teleport", 2, Group.MOVE, "minecraft:ender_pearl", opt("blink", Act.PRESS), opt("behind", Act.PRESS)),
    TELEKINESIS("telekinesis", 2, Group.MIND, "minecraft:amethyst_shard", opt("creatures", Act.PRESS), opt("everything", Act.PRESS)),
    WALL_CRAWL("wall_crawl", 1, Group.MOVE, "minecraft:spider_eye", opt("steady", Act.TOGGLE), opt("fast", Act.TOGGLE)),
    FORCE_FIELD("force_field", 2, Group.ENERGY, "minecraft:shield", opt("bubble", Act.HOLD), opt("reflect", Act.HOLD)),
    PHASING("phasing", 3, Group.BODY, "minecraft:phantom_membrane", opt("phase", Act.TOGGLE), opt("ghost", Act.TOGGLE)),
    ANIMAL_CONTROL("animal_control", 1, Group.MIND, "minecraft:lead", opt("pack", Act.PRESS), opt("calm", Act.PRESS)),
    MIND_CONTROL("mind_control", 3, Group.MIND, "minecraft:name_tag", opt("ally", Act.PRESS), opt("calm", Act.PRESS)),
    MAGIC("magic", 2, Group.ENERGY, "minecraft:enchanted_book", opt("bolts", Act.PRESS), opt("ward", Act.PRESS), opt("curse", Act.PRESS)),
    ICE("ice", 2, Group.ELEMENT, "minecraft:snowball", opt("freeze_ray", Act.PRESS), opt("ice_path", Act.TOGGLE), opt("blizzard", Act.PRESS)),
    WEATHER("weather", 2, Group.ELEMENT, "minecraft:sunflower", opt("storm", Act.PRESS), opt("cycle", Act.PRESS)),
    LIGHTNING("lightning", 2, Group.ELEMENT, "minecraft:lightning_rod", opt("strike", Act.PRESS), opt("chain", Act.PRESS)),
    SHAPESHIFT("shapeshift", 1, Group.BODY, "minecraft:slime_ball", opt("grow", Act.TOGGLE), opt("shrink", Act.TOGGLE), opt("creature", Act.PRESS)),
    INVISIBILITY("invisibility", 2, Group.BODY, "minecraft:glass", opt("full", Act.TOGGLE), opt("shimmer", Act.TOGGLE)),
    ABSORPTION("absorption", 2, Group.ENERGY, "minecraft:sponge", opt("energy", Act.PRESS), opt("power", Act.PRESS)),
    FIRE("fire", 2, Group.ELEMENT, "minecraft:fire_charge", opt("fireball", Act.PRESS), opt("flamethrower", Act.HOLD), opt("aura", Act.TOGGLE)),
    DUPLICATION("duplication", 2, Group.MIND, "minecraft:armor_stand", opt("clones", Act.PRESS), opt("decoys", Act.PRESS)),
    WATER("water", 1, Group.ELEMENT, "minecraft:water_bucket", opt("jet", Act.HOLD), opt("wave", Act.PRESS)),
    TIME("time", 3, Group.MIND, "minecraft:clock", opt("slow", Act.TOGGLE), opt("stop", Act.PRESS), opt("rewind", Act.PRESS)),
    TECHNOPATHY("technopathy", 1, Group.MIND, "minecraft:redstone", opt("hack", Act.PRESS), opt("command", Act.PRESS)),
    SONIC_SCREAM("sonic_scream", 2, Group.ENERGY, "minecraft:goat_horn", opt("cone", Act.PRESS), opt("ring", Act.PRESS)),
    EARTH("earth", 2, Group.ELEMENT, "minecraft:cobblestone", opt("tremor", Act.PRESS), opt("spikes", Act.PRESS)),
    WIND("wind", 1, Group.ELEMENT, "minecraft:wind_charge", opt("gust", Act.PRESS), opt("updraft", Act.PRESS));

    /** The strongest any power can be (a server may allow less: {@code custom_hero.maxStrength}). */
    public static final int MAX_STRENGTH = 10;

    /** How a module's key acts in one of its options. */
    public enum Act {
        /** A passive power: always on, it takes no key. */
        NONE,
        /** Each press does it once (then its cooldown runs). */
        PRESS,
        /** It lasts while the key is held down. */
        HOLD,
        /** A press switches it on, the next one off. */
        TOGGLE
    }

    /** The catalogue's sections (the screen groups and colours the buttons by them). */
    public enum Group {
        BODY(0xE0574A), MOVE(0x52C26B), ENERGY(0xF2C744), ELEMENT(0x4AA8E0), MIND(0xB57BEA);

        /** The colour of the section's buttons. */
        public final int color;

        Group(int color) {
            this.color = color;
        }
    }

    /** One sub-option: its id (saved) and how the key acts in it. */
    public record Option(String id, Act act) {
    }

    /** The id saved in a hero's definition and in its JSON file. */
    public final String id;
    /** Power points for each point of its strength. */
    public final int costPerPoint;
    public final Group group;
    /** The item drawn on its button (an item id). */
    public final String icon;
    private final List<Option> options;

    PowerModule(String id, int costPerPoint, Group group, String icon, Option... options) {
        this.id = id;
        this.costPerPoint = costPerPoint;
        this.group = group;
        this.icon = icon;
        this.options = List.of(options);
    }

    private static Option opt(String id, Act act) {
        return new Option(id, act);
    }

    public List<Option> options() {
        return options;
    }

    /** The default sub-option. */
    public Option defaultOption() {
        return options.get(0);
    }

    /** The option of this id, or null. */
    @Nullable
    public Option option(String id) {
        for (Option option : options) {
            if (option.id().equals(id)) {
                return option;
            }
        }
        return null;
    }

    /** How the key acts in the option of this id (the default option's when the id is unknown). */
    public Act act(String option) {
        Option found = option(option);
        return (found == null ? defaultOption() : found).act();
    }

    /** The power points this module costs at {@code strength} (1 to 10). */
    public int cost(int strength) {
        return costPerPoint * Math.max(1, strength);
    }

    /** The lang key of the module's name ({@code power_module.enhanced.flight}); {@code .desc} is its one-line description. */
    public String nameKey() {
        return "power_module.enhanced." + id;
    }

    /** The lang key of one of its options ({@code power_module.enhanced.flight.jet}). */
    public String optionKey(String option) {
        return nameKey() + "." + option;
    }

    /** The module of this id, or null for a power this version does not know. */
    @Nullable
    public static PowerModule byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (PowerModule module : values()) {
            if (module.id.equals(wanted)) {
                return module;
            }
        }
        return null;
    }
}
