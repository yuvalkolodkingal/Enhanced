package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.config.core.ConfigFile;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.config.core.Toml;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The config compatibility gate (port P0, lane L7, PLAN2 3.3): the files players have load with the mod's own backend
 * exactly as they did with NeoForge's. Every case of {@link ConfigCompatCases} is loaded with {@link ConfigFile} and
 * compared with what NeoForge's backend did with it ({@code config_compat/cases.txt}, recorded with NightConfig before
 * the swap): the file after loading (byte for byte), whether a backup was made, whether the file was remade (it was not
 * TOML), and every value the game reads.
 */
public final class ConfigCompatTests {
    static final String BATCH = "config_core";

    private ConfigCompatTests() {
    }

    /** One line of the record. */
    record Recorded(String name, String kind, String input, String output, boolean backup, boolean recreated,
                    String values, List<String> differing) {
    }

    static Map<String, Recorded> recorded() {
        Map<String, Recorded> cases = new LinkedHashMap<>();
        Recorded current = null;
        for (String line : resource("cases.txt").split("\n")) {
            if (line.startsWith("case ")) {
                Map<String, String> fields = new LinkedHashMap<>();
                String[] parts = line.split(" ");
                for (int i = 3; i < parts.length; i++) {
                    String[] kv = parts[i].split("=", 2);
                    fields.put(kv[0], kv[1]);
                }
                current = new Recorded(parts[1], parts[2], fields.get("input"), fields.get("output"),
                        Boolean.parseBoolean(fields.get("backup")), Boolean.parseBoolean(fields.get("recreated")),
                        fields.get("values"), new ArrayList<>());
                cases.put(current.name(), current);
            } else if (line.startsWith("  ") && current != null) {
                current.differing().add(line.substring(2));
            }
        }
        return cases;
    }

    static String resource(String name) {
        try (InputStream in = ConfigCompatTests.class.getResourceAsStream("/config_compat/" + name)) {
            if (in == null) {
                throw new IllegalStateException("no test resource config_compat/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static ConfigSpec spec(ConfigCompatCases.Kind kind) {
        return kind == ConfigCompatCases.Kind.SERVER ? EnhancedConfig.SPEC : EnhancedClientConfig.SPEC;
    }

    static String fileName(ConfigCompatCases.Kind kind) {
        return kind == ConfigCompatCases.Kind.SERVER ? "enhanced-server.toml" : "enhanced-client.toml";
    }

    /** Every value of {@code spec} as the game reads it from {@code table}, in the record's form. */
    static List<String> values(ConfigSpec spec, Toml.Table table) {
        List<String> lines = new ArrayList<>();
        for (ConfigSpec.ConfigValue<?> value : spec.values()) {
            Object read;
            try {
                read = value.read(table);
            } catch (RuntimeException e) {
                read = "!" + e.getClass().getSimpleName();
            }
            lines.add(String.join(".", value.getPath()) + "=" + ConfigCompatCases.describe(read));
        }
        return lines;
    }

    static Path tempDir() {
        try {
            return Files.createTempDirectory("enhanced-config-test");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void delete(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void write(Path file, String text) {
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The default files are NeoForge's, byte for byte, and read back as they were written. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theDefaultFilesAreTheOnesNeoForgeWrote(GameTestHelper helper) {
        Map<String, Recorded> recorded = recorded();
        for (ConfigCompatCases.Kind kind : ConfigCompatCases.Kind.values()) {
            ConfigSpec spec = spec(kind);
            String text = Toml.write(spec.defaults(), "\n");
            String expected = recorded.get(kind == ConfigCompatCases.Kind.SERVER ? "default" : "client_default")
                    .output();
            check(ConfigCompatCases.sha256(text).equals(expected), kind + ": the default file is NeoForge's ("
                    + ConfigCompatCases.sha256(text) + ", recorded " + expected + ")");
            try {
                Toml.Table table = Toml.parse(text);
                check(spec.isCorrect(table), kind + ": the default file is correct");
                check(Toml.write(table, "\n").equals(text), kind + ": the default file is written back unchanged");
            } catch (Toml.ParseException e) {
                throw new IllegalStateException(kind + ": the default file reads: " + e.getMessage());
            }
        }
        // (1,681 server and 22 client options in 3.8.3)
        check(EnhancedConfig.SPEC.values().size() >= 1681 && EnhancedClientConfig.SPEC.values().size() >= 22,
                "every option is a value: " + EnhancedConfig.SPEC.values().size() + " + "
                        + EnhancedClientConfig.SPEC.values().size());
        helper.succeed();
    }

    /**
     * Every recorded file loads as it did with NeoForge: the same file afterwards, the same backup, the same values.
     * (One known difference: a boolean written as a string, "false", reads as the boolean; NeoForge handed out the
     * string, which the code then failed to read.)
     */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void everyRecordedFileLoadsAsItDidWithNeoForge(GameTestHelper helper) {
        Map<String, Recorded> recorded = recorded();
        Map<ConfigCompatCases.Kind, String> defaults = new LinkedHashMap<>();
        for (ConfigCompatCases.Kind kind : ConfigCompatCases.Kind.values()) {
            defaults.put(kind, Toml.write(spec(kind).defaults(), "\n"));
        }
        Path dir = tempDir();
        List<String> failures = new ArrayList<>();
        int checked = 0;
        try {
            for (ConfigCompatCases.Case c : ConfigCompatCases.all()) {
                Recorded expected = recorded.get(c.name());
                check(expected != null, c.name() + " is in the record");
                ConfigSpec spec = spec(c.kind());
                String fileName = fileName(c.kind());
                Path caseDir = dir.resolve(c.name());
                Files.createDirectories(caseDir);
                Path file = caseDir.resolve(fileName);
                String input = c.input(defaults.get(c.kind()), c.resource() == null ? null : resource(c.resource()));
                if (input != null) {
                    write(file, input);
                    if (!ConfigCompatCases.sha256(input).equals(expected.input())) {
                        failures.add(c.name() + ": the input differs from the recorded one");
                        continue;
                    }
                }
                boolean parses = input != null && parses(input);
                Toml.Table table = ConfigFile.read(file, spec, fileName);
                String output = read(file);
                boolean backup = Files.exists(caseDir.resolve(fileName.replace(".toml", "-1.toml.bak")));
                List<String> values = values(spec, table);
                if (c.name().equals("string_boolean")) {
                    values.replaceAll(line -> line.equals("magneto.pullTakesBuiltMetal=Boolean:false")
                            ? "magneto.pullTakesBuiltMetal=String:false" : line);
                    check(spec.value(List.of("magneto", "pullTakesBuiltMetal")).read(table).equals(Boolean.FALSE),
                            "a boolean written as a string reads as the boolean");
                }
                String dump = ConfigCompatCases.dump(values);
                if (!ConfigCompatCases.sha256(output).equals(expected.output())) {
                    failures.add(c.name() + ": the file afterwards differs (" + caseDir + ")");
                }
                if (backup != expected.backup()) {
                    failures.add(c.name() + ": backup " + backup + ", recorded " + expected.backup());
                }
                if (input != null && parses == expected.recreated()) {
                    failures.add(c.name() + ": parses " + parses + ", recorded remade " + expected.recreated());
                }
                if (!ConfigCompatCases.sha256(dump).equals(expected.values())) {
                    List<String> differing = new ArrayList<>(values);
                    differing.removeAll(values(spec, spec.defaults()));
                    failures.add(c.name() + ": the values differ: " + differing + ", recorded "
                            + expected.differing());
                }
                checked++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        check(failures.isEmpty(), failures.toString());
        check(checked == recorded.size(), "every recorded case was loaded: " + checked + " of " + recorded.size());
        delete(dir);
        helper.succeed();
    }

    private static boolean parses(String text) {
        try {
            Toml.parse(text);
            return true;
        } catch (Toml.ParseException e) {
            return false;
        }
    }

    /** A file a player edited keeps its order, its values and its comments when the game writes it again. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCorrectFileIsWrittenBackAsItWas(GameTestHelper helper) {
        ConfigSpec spec = EnhancedConfig.SPEC;
        String text = Toml.write(spec.defaults(), "\n");
        // a section moved to the end, values changed: still correct, so written back (on a save) unchanged
        String edited = ConfigCompatCases.set(text, "magneto", "flightSpeed", "0.2");
        edited = ConfigCompatCases.removeSection(edited, "thor") + "\n" + ConfigCompatCases.section(text, "thor");
        try {
            Toml.Table table = Toml.parse(edited);
            check(spec.isCorrect(table), "a moved section and a changed value are correct");
            check(Toml.write(table, "\n").equals(edited), "it is written back in its own order");
            List<String> sections = new ArrayList<>(table.values().keySet());
            check(sections.get(sections.size() - 1).equals("thor"), "thor stays last: " + sections);
            check(table.comment("thor") == null || spec.getLevelComment(List.of("thor")) != null,
                    "a section keeps its comment");
        } catch (Toml.ParseException e) {
            throw new IllegalStateException(e.getMessage());
        }
        helper.succeed();
    }

    /** A wrong value is corrected (out of its range clamped, anything else its default) and the log says so. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aWrongValueBecomesItsDefaultWithAWarning(GameTestHelper helper) {
        ConfigSpec spec = EnhancedConfig.SPEC;
        String text = Toml.write(spec.defaults(), "\n");
        text = ConfigCompatCases.set(text, "magneto", "flightSpeed", "\"fast\"");
        text = ConfigCompatCases.set(text, "magneto", "telekinesisRange", "1000");
        text = ConfigCompatCases.insertAfter(text, "magneto", "flightSpeed", "\tnoSuchOption = 1");
        try {
            Toml.Table table = Toml.parse(text);
            List<String> warnings = new ArrayList<>();
            int corrected = spec.correct(table, (action, path, was, now) -> warnings.add(action + " "
                    + String.join(".", path) + " " + was + " -> " + now), null);
            check(corrected == 3, "three corrections: " + warnings);
            check(warnings.contains("REPLACE magneto.flightSpeed fast -> 0.075"), "the string became the default: "
                    + warnings);
            check(warnings.contains("REPLACE magneto.telekinesisRange 1000 -> 128"), "1000 was clamped to 128: "
                    + warnings);
            check(warnings.contains("REMOVE magneto.noSuchOption 1 -> null"), "the unknown option went: " + warnings);
            check(EnhancedConfig.MAGNETO_FLIGHT_SPEED.read(table) == 0.075, "the flight speed reads its default");
        } catch (Toml.ParseException e) {
            throw new IllegalStateException(e.getMessage());
        }
        helper.succeed();
    }
}
