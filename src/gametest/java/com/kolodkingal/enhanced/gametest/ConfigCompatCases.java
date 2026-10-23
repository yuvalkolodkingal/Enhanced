package com.kolodkingal.enhanced.gametest;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The config files of the compatibility gate (port P0, lane L7): what players may have in
 * {@code enhanced-server.toml} and {@code enhanced-client.toml}. Each case is a text made from the default file (or a
 * real file an earlier build wrote, in {@code config_compat/}); the expected result of loading it (the file after the
 * load, whether a backup was made, every value) was recorded once with NeoForge's own config backend (NightConfig,
 * before the mod had its own) in {@code config_compat/cases.txt}, and {@link ConfigCompatTests} checks the mod's
 * backend gives the same. Plain text only: the same class drove the recording.
 */
public final class ConfigCompatCases {
    /** Which config a case is a file of. */
    public enum Kind {
        SERVER, CLIENT
    }

    /**
     * One file: {@code edit} makes it from the default text of its kind, or {@code resource} names a file of
     * {@code config_compat/}; neither means there is no file at all.
     */
    public record Case(String name, Kind kind, @Nullable UnaryOperator<String> edit, @Nullable String resource) {
        /** The text of the file ({@code null}: no file), from the default text of its kind. */
        @Nullable
        public String input(String defaultText, @Nullable String resourceText) {
            if (resource != null) {
                return resourceText;
            }
            return edit == null ? null : edit.apply(defaultText);
        }
    }

    private ConfigCompatCases() {
    }

    public static List<Case> all() {
        List<Case> cases = new ArrayList<>();
        // ---- the server's file ----
        server(cases, "default", text -> text);
        cases.add(new Case("missing", Kind.SERVER, null, null));
        server(cases, "values_in_range", text -> {
            text = set(text, "magneto", "flightSpeed", "0.2");
            text = set(text, "magneto", "telekinesisRange", "64");
            text = set(text, "magneto", "pullTakesBuiltMetal", "false");
            text = set(text, "build_magic", "fullContainers", "\"SPILL\"");
            text = set(text, "spider_man", "webAssistOpenLand", "\"zip\"");
            text = set(text, "apocalypse_boss", "ritualDimensions",
                    "[\"minecraft:the_nether\", \"minecraft:overworld\"]");
            text = set(text, "build_magic", "deniedDimensions", "[\"minecraft:the_end\"]");
            return set(text, "darkhold", "poisonTarget", "\"HOLDER\"");
        });
        server(cases, "out_of_range", text -> {
            text = set(text, "magneto", "flightSpeed", "5.0");
            text = set(text, "magneto", "telekinesisRange", "1000");
            text = set(text, "magneto", "shrapnelPierce", "0");
            return set(text, "magneto", "throwSpeed", "-1.0");
        });
        server(cases, "wrong_types", text -> {
            text = set(text, "magneto", "flightSpeed", "\"fast\"");
            text = set(text, "magneto", "telekinesisRange", "true");
            text = set(text, "magneto", "pullTakesBuiltMetal", "1");
            text = set(text, "build_magic", "fullContainers", "\"NOPE\"");
            text = set(text, "apocalypse_boss", "ritualDimensions", "\"minecraft:overworld\"");
            text = set(text, "build_magic", "deniedDimensions", "[1, \"minecraft:the_end\"]");
            return set(text, "spider_man", "webAssistOpenLand", "\"fly\"");
        });
        server(cases, "loose_types", text -> {
            text = set(text, "magneto", "shrapnelDamage", "4");
            text = set(text, "magneto", "telekinesisRange", "32.0");
            text = set(text, "build_magic", "fullContainers", "\"spill\"");
            return set(text, "darkhold", "poisonTarget", "\"holder\"");
        });
        server(cases, "string_boolean", text -> set(text, "magneto", "pullTakesBuiltMetal", "\"false\""));
        server(cases, "number_syntax", text -> {
            text = set(text, "magneto", "telekinesisRange", "0x40");
            text = set(text, "magneto", "shrapnelPierce", "1_0");
            text = set(text, "magneto", "shrapnelDamage", "1e1");
            text = set(text, "magneto", "disarmRange", "+48");
            text = set(text, "magneto", "disarmCooldown", "0o17");
            text = set(text, "magneto", "shrapnelFireInterval", "0b11");
            return set(text, "magneto", "throwSpeed", "2.6E0");
        });
        server(cases, "inf_nan", text -> {
            text = set(text, "magneto", "shrapnelDamage", "inf");
            text = set(text, "magneto", "shieldReflectChance", "-inf");
            return set(text, "magneto", "throwSpeed", "nan");
        });
        server(cases, "unknown_keys", text -> {
            text = insertAfter(text, "magneto", "flightSpeed", "\tnotAKey = 5");
            return text + "\n[old_section]\n\tgone = true\n";
        });
        server(cases, "missing_keys", text -> {
            text = remove(text, "magneto", "telekinesisRange");
            text = remove(text, "magneto", "throwSpeed");
            return removeSection(text, "vision");
        });
        server(cases, "comments_edited", text -> {
            text = text.replace("#Magneto's flying speed (Creative flight is 0.05).", "#How fast he flies.");
            return text.replace("# Range: 4 ~ 128", "# Range: whatever");
        });
        server(cases, "trailing_comment", text -> set(text, "magneto", "flightSpeed", "0.2 #fast"));
        server(cases, "crlf", text -> text.replace("\n", "\r\n"));
        server(cases, "spaces", text -> text.replace("\t", "    ").replace(" = ", "="));
        server(cases, "quoted_keys", text -> {
            text = text.replace("\tflightSpeed = 0.075", "\t\"flightSpeed\" = 0.2");
            return text.replace("\ttelekinesisRange = 32", "\t'telekinesisRange' = 40");
        });
        server(cases, "reordered", text -> {
            String magneto = section(text, "magneto");
            return removeSection(text, "magneto") + "\n" + magneto;
        });
        server(cases, "strings", text -> {
            text = set(text, "spider_man", "webAssistOpenLand", "\"\"\"\nzip\"\"\"");
            text = set(text, "build_magic", "fullContainers", "'SPILL'");
            return set(text, "apocalypse_boss", "ritualDimensions",
                    "[\"minecraft:\\u006Fverworld\", 'minecraft:the_end', \"a\\tb\\\"c\\\\d\",]");
        });
        server(cases, "multiline_array", text -> set(text, "apocalypse_boss", "ritualDimensions",
                "[\n\t\t\"minecraft:overworld\", # the overworld\n\t\t\"minecraft:the_nether\"\n\t]"));
        server(cases, "tables_inside", text -> {
            text = insertAfter(text, "magneto", "flightSpeed", "\tinline = {a = 1, b = \"x\"}");
            return text + "\n[[extra]]\n\ta = 1\n\n[[extra]]\n\ta = 2\n";
        });
        server(cases, "empty_file", text -> "");
        server(cases, "syntax_error", text -> set(text, "magneto", "flightSpeed", "= 0.2"));
        server(cases, "duplicate_key", text -> insertAfter(text, "magneto", "flightSpeed", "\tflightSpeed = 0.1"));
        cases.add(new Case("old_a", Kind.SERVER, null, "old-a.toml"));
        cases.add(new Case("old_b", Kind.SERVER, null, "old-b.toml"));
        // ---- the client's file ----
        client(cases, "client_default", text -> text);
        cases.add(new Case("client_missing", Kind.CLIENT, null, null));
        client(cases, "client_values", text -> {
            text = set(text, "animation", "animations", "false");
            text = set(text, "build_magic", "ghostDetail", "1000");
            return set(text, "music", "quicksilverSong", "\"sweet_dreams\"");
        });
        client(cases, "client_bad", text -> {
            text = set(text, "build_magic", "ghostDetail", "-5");
            text = set(text, "music", "quicksilverSong", "\"rock\"");
            return set(text, "hulk", "hulkModel", "\"yes\"");
        });
        return cases;
    }

    /** How a value read from a config is written in the record: its type and value ({@code Integer:32}). */
    public static String describe(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof List<?> list) {
            List<String> elements = new ArrayList<>();
            list.forEach(element -> elements.add(describe(element)));
            return "List[" + String.join(", ", elements) + "]";
        }
        if (value instanceof Enum<?> constant) {
            return "Enum:" + constant.name();
        }
        return value.getClass().getSimpleName() + ":" + value;
    }

    /** The record of every value: {@code path=describe(value)} lines, sorted. */
    public static String dump(List<String> lines) {
        List<String> sorted = new ArrayList<>(lines);
        sorted.sort(null);
        return String.join("\n", sorted);
    }

    public static String sha256(String text) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void server(List<Case> cases, String name, UnaryOperator<String> edit) {
        cases.add(new Case(name, Kind.SERVER, edit, null));
    }

    private static void client(List<Case> cases, String name, UnaryOperator<String> edit) {
        cases.add(new Case(name, Kind.CLIENT, edit, null));
    }

    // ---- text edits (the default files have LF line ends) ----

    private static int sectionStart(List<String> lines, String section) {
        int at = lines.indexOf("[" + section + "]");
        if (at < 0) {
            throw new IllegalArgumentException("no section " + section);
        }
        return at;
    }

    private static int keyLine(List<String> lines, String section, String key) {
        for (int i = sectionStart(lines, section) + 1; i < lines.size() && !lines.get(i).startsWith("["); i++) {
            if (lines.get(i).strip().startsWith(key + " = ")) {
                return i;
            }
        }
        throw new IllegalArgumentException("no key " + section + "." + key);
    }

    private static List<String> lines(String text) {
        return new ArrayList<>(List.of(text.split("\n", -1)));
    }

    /** The value of {@code section.key} written as {@code raw}. */
    public static String set(String text, String section, String key, String raw) {
        List<String> lines = lines(text);
        int at = keyLine(lines, section, key);
        String line = lines.get(at);
        lines.set(at, line.substring(0, line.indexOf(" = ") + 3) + raw);
        return String.join("\n", lines);
    }

    /** {@code line} added after the line of {@code section.key}. */
    public static String insertAfter(String text, String section, String key, String line) {
        List<String> lines = lines(text);
        lines.add(keyLine(lines, section, key) + 1, line);
        return String.join("\n", lines);
    }

    /** The line of {@code section.key} taken out (its comment lines stay). */
    public static String remove(String text, String section, String key) {
        List<String> lines = lines(text);
        lines.remove(keyLine(lines, section, key));
        return String.join("\n", lines);
    }

    /** The text of a section: from its header to the blank line before the next one (both kept out). */
    public static String section(String text, String section) {
        List<String> lines = lines(text);
        int start = sectionStart(lines, section);
        int end = start + 1;
        while (end < lines.size() && !lines.get(end).isEmpty()) {
            end++;
        }
        return String.join("\n", lines.subList(start, end)) + "\n";
    }

    /** A section taken out with the blank line after it. */
    public static String removeSection(String text, String section) {
        List<String> lines = lines(text);
        int start = sectionStart(lines, section);
        int end = start + 1;
        while (end < lines.size() && !lines.get(end).isEmpty()) {
            end++;
        }
        if (end < lines.size()) {
            end++;
        }
        lines.subList(start, end).clear();
        return String.join("\n", lines);
    }
}
