package com.kolodkingal.enhanced.balance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

/**
 * SMP Balance Mode (3.16): one server-wide rule for how strong every power is, so a server stays fair. A
 * {@link Preset} sets a strength for each power and each {@link Setting} (range, size, duration, cooldown, energy cost,
 * damage); an operator may override one power's setting on top of it. It binds every player alike, operators too: it
 * changes the power numbers of the server config as they are read ({@link ConfigSpec.Adjuster}), never the file, so
 * nothing is per player and every power stays usable (caps, not removals). Bosses and the world (the boss fights, the
 * structures, the realms) are never touched.
 *
 * <p>Saved per world in {@code <world>/serverconfig/enhanced-balance.json} (editable; {@code /enhanced balance reload}
 * reads it again), and sent to every player, so the client's own reading of the numbers agrees with the server's.
 */
public final class Balance {
    private Balance() {
    }

    /** What a number of a power is. */
    public enum Setting {
        RANGE(false), SIZE(false), DURATION(false), COOLDOWN(true), COST(true), DAMAGE(false);

        /** Whether a weaker power has more of it (a longer cooldown, a dearer cost). */
        final boolean inverse;

        Setting(boolean inverse) {
            this.inverse = inverse;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static Setting byId(String id) {
            for (Setting s : values()) {
                if (s.id().equals(id)) {
                    return s;
                }
            }
            return null;
        }
    }

    /** The rulesets. */
    public enum Preset {
        /** Every power as the server config has it (the default). */
        FULL,
        /** SMP-fair: the strongest powers capped to their closest counterparts' scale. */
        BALANCED,
        /** Heavily limited: every power at half its strength or less. */
        HARDCORE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static Preset byId(String id) {
            for (Preset p : values()) {
                if (p.id().equals(id)) {
                    return p;
                }
            }
            return null;
        }

        /** The strength of {@code power} under this preset (1: as configured). */
        double strength(String power) {
            return switch (this) {
                case FULL -> 1.0;
                case BALANCED -> tier(power);
                case HARDCORE -> tier(power) * 0.5;
            };
        }
    }

    /**
     * The powers: the server config's sections of the heroes' powers and the items that give powers. Each is a power
     * of {@code /enhanced balance set}. The rest (the boss fights, the structures, the realms, technical settings) is
     * never changed.
     */
    public static final Set<String> POWERS = Collections.unmodifiableSet(new java.util.LinkedHashSet<>(List.of(
            "magneto", "scarlet_witch", "hex_vision", "vision", "quicksilver", "billy", "tommy", "loki", "thor",
            "phoenix", "spider_man", "thanos", "ultron", "apocalypse", "build_magic_wanda", "build_magic_billy",
            "vishanti", "darkhold", "cyclops", "custom_hero", "hex_runes", "professor_x", "agatha", "speedsters",
            "heimdall", "odin", "gungnir", "hela", "stones", "gauntlet", "reality_stone", "mind_stone", "time_stone",
            "power_stone", "space_stone", "soul_stone", "doctor_strange", "eye_of_agamotto", "sling_ring", "hulk",
            "healing_factor", "wolverine", "deadpool", "dogpool", "captain_america", "vibranium_shield", "red_hulk",
            "iron_man", "hulkbuster", "falcon", "love")));

    /** The strongest powers: at 0.6 of their strength when balanced. */
    private static final Set<String> STRONGEST = Set.of("scarlet_witch", "hex_vision", "build_magic_wanda", "phoenix",
            "thanos", "apocalypse", "odin", "hela", "love", "gauntlet", "reality_stone", "mind_stone", "time_stone",
            "power_stone", "space_stone", "soul_stone");
    /** The strong ones: at 0.8 when balanced. */
    private static final Set<String> STRONG = Set.of("magneto", "thor", "loki", "vision", "ultron", "quicksilver",
            "doctor_strange", "eye_of_agamotto", "hulk", "red_hulk", "iron_man", "hulkbuster", "professor_x", "agatha",
            "darkhold", "gungnir", "heimdall");

    /** A power's strength when balanced: the strongest are brought down to the scale of the rest. */
    static double tier(String power) {
        return STRONGEST.contains(power) ? 0.6 : STRONG.contains(power) ? 0.8 : 1.0;
    }

    // ---- what each number is ----

    /** Numbers that are none of the settings, or whose stronger side is unclear: never changed. */
    private static final Pattern SKIP = Pattern.compile("^enabled|seed|version|chance|factor|multiplier|fraction"
            + "|practice|rank|percent|share|threshold|health|armor|toughness|speed|knockback|angle|amplifier|level"
            + "|regen|heal|scale|budget|undo|millis|pertick|boost|copies|places|bonus|grace|ritual|check|confirm"
            + "|awaken|dropheight|dropinheight|minheight|meter|refill|pause|hardness|targets|pierce");
    /** A minimum (minFall, ...): a lower one is no weaker, so never changed. */
    private static final Pattern MINIMUM = Pattern.compile("^min[A-Z]");
    private static final Object[][] RULES = {
            {Setting.COOLDOWN, Pattern.compile("cooldown|interval|recharge|delay|windup|warmup|chargeticks|chargetime"
                    + "|every$|retry|regrip|rebuild|crash")},
            {Setting.COST, Pattern.compile("exhaustion|cost|price|fuel")},
            {Setting.DAMAGE, Pattern.compile("damage")},
            {Setting.RANGE, Pattern.compile("range|radius|distance|reach")},
            {Setting.SIZE, Pattern.compile("size|width|height|maxblocks|maxside|blockcap|volume|selectmax|brushmax"
                    + "|repeatmax|depth|length")},
            {Setting.DURATION, Pattern.compile("duration|ticks$|seconds$|lifetime|time$")},
    };

    /** The setting a number of the server config is, by its name, or null if none (it is never changed). */
    @Nullable
    public static Setting classify(String key) {
        if (MINIMUM.matcher(key).find()) {
            return null;
        }
        String k = key.toLowerCase(Locale.ROOT);
        if (SKIP.matcher(k).find()) {
            return null;
        }
        for (Object[] rule : RULES) {
            if (((Pattern) rule[1]).matcher(k).find()) {
                return (Setting) rule[0];
            }
        }
        return null;
    }

    // ---- the counterpart caps ----

    /** A cap of one number by its counterpart's scale: its setting, and the cap for a preset. */
    private record Cap(Setting setting, ToDoubleFunction<Preset> limit, boolean zeroIsUnlimited) {
        Cap(Setting setting, ToDoubleFunction<Preset> limit) {
            this(setting, limit, false);
        }
    }

    /**
     * Balanced and hardcore: Wanda's Build Magic on the scale of Billy's, his as Wiccan at his highest rank when
     * balanced, his as an Apprentice in hardcore (her 0, no ceiling, is capped too).
     */
    private static final Map<String, Cap> CAPS = Map.of(
            "build_magic_wanda.reach", new Cap(Setting.RANGE, p -> billyRanked(p, "reach", 8)),
            "build_magic_wanda.shapeMaxRadius", new Cap(Setting.RANGE, p -> Math.max(8, billyRanked(p, "maxSide", 8) / 2)),
            "build_magic_wanda.maxBlocks", new Cap(Setting.SIZE, Balance::billyMeter, true),
            "build_magic_wanda.instantMaxBlocks", new Cap(Setting.SIZE, Balance::billyMeter),
            "build_magic_wanda.magicSelectMax", new Cap(Setting.SIZE, p -> Math.max(1000, billyMeter(p))),
            "build_magic_wanda.repeatMax", new Cap(Setting.SIZE, p -> billyRanked(p, "repeatMax", 4), true));

    private static double billy(String key) {
        return ((Number) EnhancedConfig.SPEC.value(List.of("build_magic_billy", key)).get()).doubleValue();
    }

    /** Billy's {@code key} at the rank the preset matches Wanda to (it grows by {@code perRank} each rank). */
    private static double billyRanked(Preset preset, String key, int perRank) {
        double base = billy(key);
        return preset == Preset.HARDCORE ? base : base + perRank * (billy("maxRank") - 1);
    }

    /** Billy's magic meter at that rank (it doubles to rank 4, and is 64 times as much at rank 10). */
    private static double billyMeter(Preset preset) {
        double meter = billy("meter");
        if (preset == Preset.HARDCORE) {
            return meter;
        }
        int rank = (int) billy("maxRank");
        return meter * (rank >= 10 ? 64 : 1 << Math.min(rank - 1, 3));
    }

    // ---- the state ----

    private static volatile Preset preset = Preset.FULL;
    /** power.setting -> strength, on top of the preset. */
    private static volatile Map<String, Double> overrides = Map.of();
    /** Where this world's balance is kept (null: no server here, the client of a server). */
    @Nullable
    private static Path file;

    public static Preset preset() {
        return preset;
    }

    public static Map<String, Double> overrides() {
        return overrides;
    }

    /** The strength of a power's setting now: its override, or the preset's. */
    public static double strength(String power, Setting setting) {
        Double o = overrides.get(power + "." + setting.id());
        return o != null ? o : preset.strength(power);
    }

    /** Sets the whole state (the server's file, or the server's message on a client). */
    public static void apply(Preset p, Map<String, Double> o) {
        preset = p;
        overrides = Collections.unmodifiableMap(new TreeMap<>(o));
        ConfigSpec.setAdjuster(p == Preset.FULL && o.isEmpty() ? null : Balance::adjust);
    }

    /** Back to the default (the server stopped, or the client left its server). */
    public static void reset() {
        file = null;
        apply(Preset.FULL, Map.of());
    }

    // ---- the adjuster ----

    /** A number of the server config as the balance makes it. */
    static Object adjust(ConfigSpec.ConfigValue<?> value, Object raw) {
        if (!(raw instanceof Number number) || value.owner() != EnhancedConfig.SPEC) {
            return raw;
        }
        List<String> path = value.getPath();
        String power = path.get(0);
        if (!POWERS.contains(power)) {
            return raw;
        }
        String key = path.get(path.size() - 1);
        Setting setting = classify(key);
        if (setting == null) {
            return raw;
        }
        double v = number.doubleValue();
        double f = strength(power, setting);
        double out = setting.inverse ? (f <= 0 ? v : v / f) : v * f;
        String id = power + "." + key;
        Cap cap = CAPS.get(id);
        if (cap != null && cap.setting == setting && preset != Preset.FULL
                && !overrides.containsKey(power + "." + setting.id())) {
            double limit = cap.limit.applyAsDouble(preset);
            // (her 0 is "no ceiling" for the blocks and the repeats, but "none at all" for the Instant level)
            out = out <= 0 ? (cap.zeroIsUnlimited ? limit : out) : Math.min(out, limit);
        }
        out = clamp(value, out);
        if (raw instanceof Integer i) {
            int r = (int) Math.round(out);
            return i >= 1 ? Math.max(1, r) : r;
        }
        if (raw instanceof Long l) {
            long r = Math.round(out);
            return l >= 1 ? Math.max(1L, r) : r;
        }
        if (raw instanceof Float) {
            return (float) out;
        }
        return out;
    }

    /** Into the range the config allows for this value. */
    private static double clamp(ConfigSpec.ConfigValue<?> value, double v) {
        try {
            ConfigSpec.Range<?> range = value.getSpec().getRange();
            if (range != null && range.getMin() instanceof Number min && range.getMax() instanceof Number max) {
                return Math.max(min.doubleValue(), Math.min(max.doubleValue(), v));
            }
        } catch (RuntimeException ignored) {
            // no spec: as it is
        }
        return v;
    }

    // ---- the world's file ----

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Reads the world's balance (the server is starting); none yet: the full preset. */
    public static String load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("enhanced-balance.json");
        return reload();
    }

    /** Reads the file again; null when all went well, else what was wrong (and the state stays as it was). */
    @Nullable
    public static String reload() {
        if (file == null || !Files.exists(file)) {
            apply(Preset.FULL, Map.of());
            return null;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            Preset p = Preset.byId(json.has("preset") ? json.get("preset").getAsString() : "full");
            if (p == null) {
                return "unknown preset " + json.get("preset");
            }
            Map<String, Double> o = new LinkedHashMap<>();
            if (json.has("overrides")) {
                for (Map.Entry<String, com.google.gson.JsonElement> e : json.getAsJsonObject("overrides").entrySet()) {
                    String bad = checkOverride(e.getKey());
                    if (bad != null) {
                        return bad;
                    }
                    double s = e.getValue().getAsDouble();
                    if (!(s > 0) || s > MAX_STRENGTH) {
                        return e.getKey() + ": the strength must be above 0 and at most " + MAX_STRENGTH;
                    }
                    o.put(e.getKey(), s);
                }
            }
            apply(p, o);
            return null;
        } catch (IOException | RuntimeException e) {
            Enhanced.LOGGER.error("Could not read {}", file, e);
            return e.getMessage();
        }
    }

    /** The highest strength of an override (1: as configured; above 1 makes a power stronger). */
    public static final double MAX_STRENGTH = 4.0;

    /** Null when {@code power.setting} names a power and a setting, else what is wrong. */
    @Nullable
    static String checkOverride(String key) {
        int dot = key.lastIndexOf('.');
        if (dot < 0 || !POWERS.contains(key.substring(0, dot)) || Setting.byId(key.substring(dot + 1)) == null) {
            return "not a power.setting: " + key;
        }
        return null;
    }

    /** Writes the state to the world's file. */
    static void save() {
        if (file == null) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("preset", preset.id());
        JsonObject o = new JsonObject();
        overrides.forEach(o::addProperty);
        json.add("overrides", o);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not write {}", file, e);
        }
    }
}
