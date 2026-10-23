package com.kolodkingal.enhanced.platform.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.gametest.framework.TestReporter;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The parity report (port P0, PLAN2 3.5): when a GameTest run ends, the ids ({@code Class.method}) of the tests that
 * passed and failed go to {@code <mc>-<loader>.json} (in {@code -Denhanced.testReport=<folder>}, which the build sets to
 * {@code build/gametest}), so the runs of the loaders and Minecraft versions can be compared. It logs every failure
 * as the game's own reporter does.
 */
public final class ParityReporter implements TestReporter {
    public static final String PROPERTY = "enhanced.testReport";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final TestReporter log = new LogTestReporter();
    private final Map<TestFunction, String> ids = new IdentityHashMap<>();
    private final TreeSet<String> passed = new TreeSet<>();
    private final TreeSet<String> failed = new TreeSet<>();
    private final Path file;

    ParityReporter(Path folder, String loader) {
        this.file = folder.resolve(SharedConstants.getCurrentVersion().getName() + "-" + loader + ".json");
    }

    /** The report folder the system property names, or {@code gametest} in the game's folder. */
    static Path folder() {
        String folder = System.getProperty(PROPERTY);
        return Path.of(folder == null || folder.isBlank() ? "gametest" : folder);
    }

    void add(TestFunction function, String id) {
        ids.put(function, id);
    }

    private String id(GameTestInfo info) {
        String id = ids.get(info.getTestFunction());
        return id != null ? id : info.getTestName();
    }

    @Override
    public void onTestFailed(GameTestInfo info) {
        log.onTestFailed(info);
        if (info.isRequired()) {
            failed.add(id(info));
        } else {
            failed.add(id(info) + " (optional)");
        }
    }

    @Override
    public void onTestSuccess(GameTestInfo info) {
        log.onTestSuccess(info);
        passed.add(id(info));
    }

    @Override
    public void finish() {
        log.finish();
        JsonObject json = new JsonObject();
        json.addProperty("minecraft", SharedConstants.getCurrentVersion().getName());
        json.addProperty("loader", TestHooks.get().loaderName());
        json.addProperty("filter", TestFilter.fromSystem().toString());
        json.addProperty("passed_count", passed.size());
        json.addProperty("failed_count", failed.size());
        json.add("failed", array(failed));
        json.add("passed", array(passed));
        write(file, json);
        LOGGER.info("Enhanced parity report: {} passed, {} failed -> {}", passed.size(), failed.size(),
                file.toAbsolutePath());
    }

    /** Writes the index of every test the run registered (its id, name, batch, template and timeout). */
    static void writeIndex(Path folder, List<TestIndex.Entry> entries) {
        JsonArray tests = new JsonArray();
        for (TestIndex.Entry entry : entries) {
            JsonObject test = new JsonObject();
            test.addProperty("id", entry.id());
            test.addProperty("name", entry.name());
            test.addProperty("batch", EnhancedTests.batch(entry.test()));
            test.addProperty("template", entry.test().template());
            test.addProperty("timeoutTicks", entry.test().timeoutTicks());
            tests.add(test);
        }
        JsonObject json = new JsonObject();
        json.addProperty("count", entries.size());
        json.add("tests", tests);
        write(folder.resolve("enhanced-tests.json"), json);
    }

    private static JsonArray array(Iterable<String> ids) {
        JsonArray array = new JsonArray();
        ids.forEach(array::add);
        return array;
    }

    private static void write(Path file, JsonObject json) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Cannot write {}", file, e);
        }
    }
}
