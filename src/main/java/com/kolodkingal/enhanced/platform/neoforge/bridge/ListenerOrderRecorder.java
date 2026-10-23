package com.kolodkingal.enhanced.platform.neoforge.bridge;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.event.EventHub;
import net.neoforged.bus.api.EventListener;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records the order in which NeoForge calls the mod's listeners, per event and priority, into
 * {@code docs/port/listener-order.txt}: the order the processor gives the generated {@code @Subscribe} listeners, and
 * that {@code ListenerOrderTests} checks. A listener that already moved to a hub is listed in its hub's order, in the
 * place of the bridge's native listener that runs it.
 *
 * <p>NeoForge registers {@code @EventBusSubscriber} classes in the order the mod's class files are found (the file
 * system's order in a development run, the jar's in a release) and their methods in the order the JVM lists them, so
 * the order recorded is the one of the run that recorded it. The server's section comes from the GameTest server, the
 * client's from a client (with its integrated server, so it holds both sides' listeners); a listener only the client
 * has is placed after the one it follows there.
 *
 * <p>File format: {@code [server]} or {@code [client]} starts a section; {@code event <NeoForge type> -> <common type>}
 * (or {@code -} when the event has no common type) starts an event; each listener line is {@code <PRIORITY> <rc|-> <class>#<method>}
 * ({@code rc}: it receives cancelled events). Lines starting with {@code #} are comments.
 *
 * <p>How to record (development runs only, or with {@code -Denhanced.debug=true}): {@code /enhanced debug
 * listener_order} writes the section of the side it runs on to the file named by {@code -Denhanced.listenerOrder}
 * (set by the build for every run); {@code ./gradlew -PrecordListenerOrder runGameTestServer} (or {@code runClient})
 * writes it when loading completes, and a client then exits.
 */
public final class ListenerOrderRecorder {
    static final String MOD_PACKAGE = "com.kolodkingal.enhanced.";
    static final String TOOLING = "com.kolodkingal.enhanced.platform.";
    public static final String FILE_PROPERTY = "enhanced.listenerOrder";
    static final String RECORD_PROPERTY = "enhanced.recordListenerOrder";
    private static final Pattern SUBSCRIBE_EVENT = Pattern.compile("@SubscribeEvent: (?:class |interface )?(\\S+) (\\w+)\\(");
    private static final Pattern KEY = Pattern.compile("[\\w.$]+#[\\w$]+");

    private ListenerOrderRecorder() {
    }

    /** Whether the debug command exists: development runs, or {@code -Denhanced.debug=true}. */
    public static boolean enabled() {
        return !FMLEnvironment.production || Boolean.getBoolean("enhanced.debug");
    }

    static void install(IEventBus modBus) {
        if (enabled() && Boolean.getBoolean(RECORD_PROPERTY)) {
            modBus.addListener(FMLLoadCompleteEvent.class, event -> {
                Result result = write(file());
                Enhanced.LOGGER.info("Recorded {} listeners of {} events ({}) to {}", result.listeners(),
                        result.events(), result.section(), result.file());
                if (FMLEnvironment.dist.isClient()) {
                    Runtime.getRuntime().halt(0);
                }
            });
        }
    }

    /** The recording's file: {@code -Denhanced.listenerOrder}, or {@code listener-order.txt} in the game folder. */
    public static Path file() {
        String path = System.getProperty(FILE_PROPERTY);
        return path != null && !path.isBlank() ? Path.of(path) : Path.of("listener-order.txt");
    }

    /** What a recording wrote. */
    public record Result(Path file, String section, int events, int listeners) {
    }

    /** Records this side's section and writes it into the file (keeping the other side's section). */
    public static Result write(Path file) {
        String section = FMLEnvironment.dist.isClient() ? "client" : "server";
        List<String> lines = new ArrayList<>();
        int[] counts = new int[2];
        Map<Class<?>, Class<?>> common = NeoForgeBridge.commonByNative();
        IEventBus modBus = net.neoforged.fml.ModList.get().getModContainerById(Enhanced.MODID).orElseThrow()
                .getEventBus();
        for (IEventBus bus : List.of(NeoForge.EVENT_BUS, modBus)) {
            dump(bus, common, lines, counts);
        }
        try {
            List<String> old = Files.isRegularFile(file) ? Files.readAllLines(file, StandardCharsets.UTF_8)
                    : List.of();
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, merge(old, section, lines), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot write " + file, e);
        }
        return new Result(file, section, counts[0], counts[1]);
    }

    /** The file's header and the other sections as they were, with this section replaced. */
    static List<String> merge(List<String> old, String section, List<String> body) {
        List<String> header = new ArrayList<>();
        Map<String, List<String>> sections = new java.util.TreeMap<>(Comparator.comparing(s -> s.equals("server") ? 0
                : 1));
        String current = null;
        for (String line : old) {
            if (line.startsWith("[") && line.endsWith("]")) {
                current = line.substring(1, line.length() - 1);
                sections.put(current, new ArrayList<>());
            } else if (current == null) {
                header.add(line);
            } else {
                sections.get(current).add(line);
            }
        }
        if (header.isEmpty()) {
            header.add("# The order in which NeoForge 21.1 called Enhanced's listeners (3.8.2), per event and priority.");
            header.add("# Written by ListenerOrderRecorder (/enhanced debug listener_order, or -PrecordListenerOrder);");
            header.add("# read by the @Subscribe processor (the generated order) and ListenerOrderTests. Do not edit.");
            header.add("");
        }
        sections.put(section, body);
        List<String> out = new ArrayList<>(header);
        sections.forEach((name, lines) -> {
            out.add("[" + name + "]");
            out.addAll(lines);
        });
        return out;
    }

    private static void dump(IEventBus bus, Map<Class<?>, Class<?>> common, List<String> out, int[] counts) {
        Map<Class<?>, Object> lists = listenerLists(bus);
        List<Class<?>> events = new ArrayList<>(lists.keySet());
        events.sort(Comparator.comparing(Class::getName));
        for (Class<?> event : events) {
            List<String> lines = new ArrayList<>();
            List<List<EventListener>> priorities = read(lists.get(event), "priorities");
            for (EventPriority priority : EventPriority.values()) {
                for (EventListener listener : priorities.get(priority.ordinal())) {
                    describe(listener, priority, lines);
                }
            }
            if (!lines.isEmpty()) {
                Class<?> type = common.get(event);
                out.add("event " + event.getName() + " -> " + (type != null ? type.getName() : "-"));
                out.addAll(lines);
                counts[0]++;
                counts[1] += lines.size();
            }
        }
    }

    private static void describe(EventListener listener, EventPriority priority, List<String> lines) {
        String text = listener.toString();
        Matcher m = SUBSCRIBE_EVENT.matcher(text);
        if (m.find()) {
            String owner = m.group(1);
            if (owner.startsWith(MOD_PACKAGE) && !owner.startsWith(TOOLING)) {
                Object info = read(listener, "subInfo");
                boolean rc = info instanceof SubscribeEvent s && s.receiveCanceled();
                lines.add("    " + priority + (rc ? " rc " : " - ") + owner + "#" + m.group(2));
            }
            return;
        }
        Object consumer = consumerOf(listener);
        if (consumer instanceof NeoForgeBridge.BridgeListener<?, ?> bridge) {
            for (EventHub.Listener<?> l : bridge.hub.listeners(bridge.priority)) {
                if (!l.name().startsWith(TOOLING) && KEY.matcher(l.name()).matches()) {
                    lines.add("    " + priority + (l.receiveCanceled() ? " rc " : " - ") + l.name());
                }
            }
        } else if (consumer != null && consumer.getClass().getName().startsWith(MOD_PACKAGE)
                && !consumer.getClass().getName().startsWith(TOOLING)) {
            String name = consumer.getClass().getName();
            int lambda = name.indexOf("$$Lambda");
            lines.add("    " + priority + " - " + (lambda > 0 ? name.substring(0, lambda) + "#lambda" : name));
        }
    }

    private static Object consumerOf(EventListener listener) {
        for (Class<?> c = listener.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getName().equals("net.neoforged.bus.ConsumerEventHandler")) {
                Consumer<?> consumer = read(listener, c, "consumer");
                return consumer;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Object> listenerLists(IEventBus bus) {
        Object lockHelper = read(bus, "listenerLists");
        return (Map<Class<?>, Object>) read(lockHelper, "backingMap");
    }

    private static <T> T read(Object target, String field) {
        return read(target, target.getClass(), field);
    }

    /**
     * Reads a private field of NeoForge's event bus (a debug tool: the bus has no API for its listeners). The bus's
     * module is not open to the mod, so when reflection is refused the field is read through {@code Unsafe}.
     */
    @SuppressWarnings({"unchecked", "removal", "deprecation"})
    private static <T> T read(Object target, Class<?> declaring, String name) {
        try {
            Field field = null;
            for (Class<?> c = declaring; c != null && field == null; c = c.getSuperclass()) {
                try {
                    field = c.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                    // look further up
                }
            }
            if (field == null) {
                throw new NoSuchFieldException(declaring.getName() + "." + name);
            }
            try {
                field.setAccessible(true);
                return (T) field.get(target);
            } catch (RuntimeException | IllegalAccessException refused) {
                Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
                return (T) unsafe.getObject(target, unsafe.objectFieldOffset(field));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read NeoForge's listeners (" + name + ")", e);
        }
    }
}
