package com.kolodkingal.enhanced.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * A custom hero as JSON: the file the Hero Creator exports ({@code enhanced/heroes/<name>.json} in the game folder, and
 * the clipboard) and imports again, on any server. Also the form a hero is saved in with the player.
 *
 * <pre>{
 *   "format": "enhanced:custom_hero", "version": 1,
 *   "name": "Storm Girl", "color": "#3FA9F5", "glow": "#E0F4FF",
 *   "look": {"base": "skin", "slim": true}, "skin": "&lt;base64 of a 64x64 PNG&gt;",
 *   "powers": [{"id": "flight", "strength": 5, "option": "jet"}, {"id": "energy_blast", "strength": 8, "option": "beam", "color": "#FF4040"}],
 *   "keys": ["energy_blast", "flight", "", "", ""]
 * }</pre>
 *
 * <p>Reading is lenient, so a file from another version or written by hand still loads as far as it can: unknown fields
 * are ignored, a missing field takes its default, a power entry that is not an object is skipped, and each thing that
 * was not understood is noted ({@link Read#notes}). Whether the powers exist, the strengths, the budget and the skin are
 * the server's to judge ({@link CustomHeroRules}), never this reader's.
 */
public final class CustomHeroJson {
    /** The value of {@code format} this file kind has. */
    public static final String FORMAT = "enhanced:custom_hero";
    /** A text longer than this is not read at all (a 24 KiB skin is 32 KiB of base64). */
    public static final int MAX_CHARS = 256 * 1024;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * What a read gave.
     *
     * @param hero  the definition, or null when the text was not a hero at all
     * @param notes lang keys of what was not understood (shown to the player), in order
     */
    public record Read(@Nullable CustomHero hero, List<String> notes) {
        public boolean ok() {
            return hero != null;
        }
    }

    public static String write(CustomHero hero) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", hero.version() <= 0 ? CustomHero.VERSION : hero.version());
        root.addProperty("name", hero.name());
        root.addProperty("color", hex(hero.color()));
        root.addProperty("glow", hex(hero.glow()));
        JsonObject look = new JsonObject();
        look.addProperty("base", hero.look());
        look.addProperty("slim", hero.slim());
        root.add("look", look);
        if (hero.skin().length > 0) {
            root.addProperty("skin", Base64.getEncoder().encodeToString(hero.skin()));
        }
        JsonArray powers = new JsonArray();
        for (CustomHero.Pick pick : hero.powers()) {
            JsonObject p = new JsonObject();
            p.addProperty("id", pick.id());
            p.addProperty("strength", pick.strength());
            p.addProperty("option", pick.option());
            if (pick.color() >= 0) {
                p.addProperty("color", hex(pick.color()));
            }
            powers.add(p);
        }
        root.add("powers", powers);
        JsonArray keys = new JsonArray();
        for (String key : hero.keys()) {
            keys.add(key);
        }
        root.add("keys", keys);
        return GSON.toJson(root);
    }

    public static Read read(@Nullable String text) {
        List<String> notes = new ArrayList<>();
        if (text == null || text.isBlank()) {
            notes.add("message.enhanced.hero_creator.import.empty");
            return new Read(null, notes);
        }
        if (text.length() > MAX_CHARS) {
            notes.add("message.enhanced.hero_creator.import.too_big");
            return new Read(null, notes);
        }
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(text.strip());
            if (!element.isJsonObject()) {
                notes.add("message.enhanced.hero_creator.import.not_a_hero");
                return new Read(null, notes);
            }
            root = element.getAsJsonObject();
        } catch (RuntimeException e) {
            notes.add("message.enhanced.hero_creator.import.not_json");
            return new Read(null, notes);
        }
        String format = string(root, "format", FORMAT);
        if (!FORMAT.equals(format) || !(root.has("powers") || root.has("name"))) {
            notes.add("message.enhanced.hero_creator.import.not_a_hero");
            return new Read(null, notes);
        }
        int version = integer(root, "version", CustomHero.VERSION);
        if (version > CustomHero.VERSION) {
            notes.add("message.enhanced.hero_creator.import.newer");
        }
        String name = string(root, "name", "");
        int color = color(root.get("color"), CustomHero.EMPTY.color());
        int glow = color(root.get("glow"), CustomHero.EMPTY.glow());
        String look = CustomHero.LOOK_OWN;
        boolean slim = false;
        if (root.get("look") instanceof JsonObject l) {
            look = string(l, "base", CustomHero.LOOK_OWN);
            slim = l.get("slim") instanceof JsonPrimitive s && s.isBoolean() && s.getAsBoolean();
        } else if (root.get("look") instanceof JsonPrimitive l && l.isString()) {
            look = l.getAsString();
        }
        byte[] skin = new byte[0];
        String encoded = string(root, "skin", "");
        if (!encoded.isEmpty()) {
            try {
                skin = Base64.getMimeDecoder().decode(encoded);
            } catch (IllegalArgumentException e) {
                notes.add("message.enhanced.hero_creator.import.bad_skin");
            }
        }
        List<CustomHero.Pick> picks = new ArrayList<>();
        if (root.get("powers") instanceof JsonArray array) {
            boolean skipped = false;
            for (JsonElement e : array) {
                if (e instanceof JsonObject p && p.get("id") instanceof JsonPrimitive id && id.isString()) {
                    picks.add(new CustomHero.Pick(id.getAsString().toLowerCase(Locale.ROOT), strength(p),
                            string(p, "option", "").toLowerCase(Locale.ROOT), p.has("color") ? color(p.get("color"), -1) : -1));
                } else if (e instanceof JsonPrimitive id && id.isString()) {
                    picks.add(new CustomHero.Pick(id.getAsString().toLowerCase(Locale.ROOT), DEFAULT_STRENGTH, "", -1));
                } else {
                    skipped = true;
                }
            }
            if (skipped) {
                notes.add("message.enhanced.hero_creator.import.bad_power");
            }
        }
        List<String> keys = new ArrayList<>();
        if (root.get("keys") instanceof JsonArray array) {
            for (JsonElement e : array) {
                keys.add(e instanceof JsonPrimitive p && p.isString() ? p.getAsString().toLowerCase(Locale.ROOT) : "");
            }
        }
        return new Read(new CustomHero(version, name, color, glow, look, slim, skin, picks, keys), notes);
    }

    /** {@code #RRGGBB}. */
    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /** A colour written as {@code #RRGGBB}, {@code RRGGBB} or a number; {@code fallback} if it is none of them. */
    public static int color(@Nullable JsonElement e, int fallback) {
        if (!(e instanceof JsonPrimitive p)) {
            return fallback;
        }
        if (p.isNumber()) {
            return p.getAsInt() & 0xFFFFFF;
        }
        String s = p.getAsString().trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /** The strength a power gets when its entry names none. */
    public static final int DEFAULT_STRENGTH = 5;

    /**
     * A power entry's strength: {@code strength}, or a {@code level} of 1 to 3 written by hand read as 3, 6 or 10;
     * {@link #DEFAULT_STRENGTH} if it has neither.
     */
    private static int strength(JsonObject p) {
        if (p.has("strength")) {
            return integer(p, "strength", DEFAULT_STRENGTH);
        }
        if (p.has("level")) {
            int level = integer(p, "level", 2);
            return level <= 1 ? 3 : level == 2 ? 6 : 10;
        }
        return DEFAULT_STRENGTH;
    }

    private static String string(JsonObject o, String key, String fallback) {
        return o.get(key) instanceof JsonPrimitive p && p.isString() ? p.getAsString() : fallback;
    }

    private static int integer(JsonObject o, String key, int fallback) {
        try {
            return o.get(key) instanceof JsonPrimitive p && p.isNumber() ? p.getAsInt() : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** A file name for a hero's name: letters, digits and dashes (any script), at most 32, "hero" if nothing is left. */
    public static String fileName(String name) {
        StringBuilder out = new StringBuilder();
        for (int cp : name.strip().codePoints().toArray()) {
            if (Character.isLetterOrDigit(cp)) {
                out.appendCodePoint(Character.toLowerCase(cp));
            } else if ((cp == ' ' || cp == '-' || cp == '_') && out.length() > 0 && out.charAt(out.length() - 1) != '_') {
                out.append('_');
            }
            if (out.length() >= 32) {
                break;
            }
        }
        String s = out.toString().replaceAll("_+$", "");
        return s.isEmpty() ? "hero" : s;
    }

    private CustomHeroJson() {
    }
}
