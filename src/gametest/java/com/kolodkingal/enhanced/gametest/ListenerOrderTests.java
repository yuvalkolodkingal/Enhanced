package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.generated.GeneratedSubscriptions;
import com.kolodkingal.enhanced.platform.neoforge.bridge.ListenerOrderRecorder;
import com.kolodkingal.enhanced.platform.neoforge.bridge.NeoForgeBridge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The common events keep NeoForge's order (port P0, S-A): within each event and priority, the generated
 * {@code @Subscribe} listeners are registered, and run, in the order NeoForge called the same methods in 3.8.2, as
 * recorded in {@code docs/port/listener-order.txt} ({@code ListenerOrderRecorder}). A listener the recording does not
 * know fails the test: record again ({@code /enhanced debug listener_order} or {@code -PrecordListenerOrder}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ListenerOrderTests {
    private static final String BATCH = "listener_order";
    private static final String TOOLING = "com.kolodkingal.enhanced.platform.";

    private ListenerOrderTests() {
    }

    /** One generated listener: {@code type|PRIORITY|receiveCanceled|SIDE|class#method}. */
    private record Entry(String type, String priority, boolean receiveCanceled, String side, String key) {
        static Entry parse(String line) {
            String[] p = line.split("\\|");
            return new Entry(p[0], p[1], Boolean.parseBoolean(p[2]), p[3], p[4]);
        }

        String group() {
            return type + "|" + priority;
        }
    }

    private static List<Entry> generated() {
        List<Entry> entries = new ArrayList<>();
        for (String line : GeneratedSubscriptions.INDEX) {
            Entry entry = Entry.parse(line);
            if (!entry.key().startsWith(TOOLING)) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Map<String, List<Entry>> byGroup(List<Entry> entries) {
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        for (Entry entry : entries) {
            groups.computeIfAbsent(entry.group(), k -> new ArrayList<>()).add(entry);
        }
        return groups;
    }

    /** The recording, merged as the processor merges it: per group, the keys and whether each receives cancelled. */
    private static Map<String, List<String>> recorded(Map<String, Boolean> receiveCanceled) {
        Path file = ListenerOrderRecorder.file();
        if (!Files.isRegularFile(file)) {
            file = Path.of("..", "docs", "port", "listener-order.txt");
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GameTestAssertException("no listener-order recording at " + file.toAbsolutePath());
        }
        Map<String, Map<String, List<String>>> sections = new LinkedHashMap<>();
        Map<String, List<String>> section = null;
        String common = null;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[")) {
                section = sections.computeIfAbsent(line.substring(1, line.length() - 1), k -> new LinkedHashMap<>());
            } else if (line.startsWith("event ")) {
                String[] parts = line.split("\\s+");
                common = parts[3].equals("-") ? null : parts[3];
            } else if (common != null && section != null) {
                String[] parts = line.split("\\s+");
                String group = common + "|" + parts[0];
                section.computeIfAbsent(group, k -> new ArrayList<>()).add(parts[2]);
                receiveCanceled.put(group + "|" + parts[2], parts[1].equals("rc"));
            }
        }
        check(sections.containsKey("server"), "the recording has the server's section");
        Map<String, List<String>> merged = new LinkedHashMap<>();
        sections.getOrDefault("server", Map.of()).forEach((g, keys) -> merged.put(g, new ArrayList<>(keys)));
        sections.getOrDefault("client", Map.of()).forEach((g, keys) -> {
            List<String> into = merged.computeIfAbsent(g, k -> new ArrayList<>());
            int at = 0;
            for (String key : keys) {
                int known = into.indexOf(key);
                if (known >= 0) {
                    at = known + 1;
                } else {
                    into.add(at++, key);
                }
            }
        });
        return merged;
    }

    /** Every generated listener was recorded, with its receiveCanceled, and the generated order is the recorded one. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theGeneratedOrderIsNeoForgesRecordedOrder(GameTestHelper helper) {
        Map<String, Boolean> receiveCanceled = new LinkedHashMap<>();
        Map<String, List<String>> recorded = recorded(receiveCanceled);
        Map<String, List<Entry>> groups = byGroup(generated());
        int listeners = 0;
        for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
            List<String> order = recorded.getOrDefault(group.getKey(), List.of());
            List<String> keys = group.getValue().stream().map(Entry::key).toList();
            for (Entry entry : group.getValue()) {
                check(order.contains(entry.key()), entry.key() + " (" + group.getKey() + ") is in the recording: "
                        + "record the order again");
                Boolean rc = receiveCanceled.get(group.getKey() + "|" + entry.key());
                check(rc != null && rc == entry.receiveCanceled(), entry.key() + " receives cancelled events as "
                        + "recorded");
                listeners++;
            }
            List<String> expected = order.stream().filter(keys::contains).toList();
            check(expected.equals(keys), group.getKey() + ": generated " + keys + ", recorded " + expected);
        }
        Enhanced.LOGGER.info("Listener order: {} generated listeners in {} groups follow the recording", listeners,
                groups.size());
        helper.succeed();
    }

    /** The hubs of this side hold the generated listeners in the generated order, with their priorities. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theHubsRunTheGeneratedOrder(GameTestHelper helper) {
        for (Map.Entry<String, List<Entry>> group : byGroup(generated()).entrySet()) {
            List<Entry> entries = group.getValue().stream().filter(e -> !e.side().equals("CLIENT")).toList();
            if (entries.isEmpty()) {
                continue;
            }
            Entry first = entries.get(0);
            EventHub<?> hub = EventHub.of(type(first.type()));
            List<String> keys = entries.stream().map(Entry::key).toList();
            List<String> live = new ArrayList<>();
            for (EventHub.Listener<?> listener : hub.listeners(Priority.valueOf(first.priority()))) {
                if (keys.contains(listener.name())) {
                    live.add(listener.name());
                    Entry entry = entries.get(keys.indexOf(listener.name()));
                    check(listener.receiveCanceled() == entry.receiveCanceled(), listener.name()
                            + " receives cancelled events as generated");
                }
            }
            check(live.equals(keys), group.getKey() + ": the hub runs " + live + ", generated " + keys);
        }
        helper.succeed();
    }

    /** Every event a generated listener of this side listens to comes from a NeoForge event. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyGeneratedListenerHasANeoForgeEvent(GameTestHelper helper) {
        for (String line : GeneratedSubscriptions.INDEX) {
            Entry entry = Entry.parse(line);
            if (!entry.side().equals("CLIENT")) {
                check(NeoForgeBridge.binding(type(entry.type())).isPresent(), entry.type() + " has a NeoForge event ("
                        + entry.key() + ")");
            }
        }
        helper.succeed();
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name, false, ListenerOrderTests.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new GameTestAssertException("no event type " + name);
        }
    }
}
