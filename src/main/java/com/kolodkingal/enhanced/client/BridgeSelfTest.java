package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.client.CalculateDetachedCameraDistanceEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.ComputeFovModifierEvent;
import com.kolodkingal.enhanced.platform.event.client.CustomizeGuiOverlayEvent;
import com.kolodkingal.enhanced.platform.event.client.FMLClientSetupEvent;
import com.kolodkingal.enhanced.platform.event.client.InputEvent;
import com.kolodkingal.enhanced.platform.event.client.MovementInputUpdateEvent;
import com.kolodkingal.enhanced.platform.event.client.RegisterClientCommandsEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderFrameEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderGuiEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderGuiLayerEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderHandEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderHighlightEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderNameTagEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderPlayerEvent;
import com.kolodkingal.enhanced.platform.event.client.ScreenEvent;
import com.kolodkingal.enhanced.platform.event.client.SelectMusicEvent;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A development self-test of the client's event bridges (port P0): the dedicated GameTest server cannot load client
 * classes, so the BridgeTests cover only the server's events. Started with {@code -Denhanced.bridgeSelfTest=true}, it
 * listens to every common client event at HIGH, NORMAL and LOWEST while the game is played for real (the loader's own
 * events, through the loader's bridge) and checks that each one arrives, once per priority and in priority order, and
 * that its getters answer. {@code /enhanced_bridges} (a client command) shows the result; it is also written to
 * {@code bridge-selftest.txt} in the game's folder each time an event arrives for the first time. Without the
 * property nothing is registered.
 */
public final class BridgeSelfTest {
    /** Every common client event a loader bridges. */
    static final List<Class<?>> TYPES = List.of(
            ClientTickEvent.Pre.class, ClientTickEvent.Post.class,
            ClientPlayerNetworkEvent.LoggingIn.class, ClientPlayerNetworkEvent.LoggingOut.class,
            ClientPlayerNetworkEvent.Clone.class,
            RenderLevelStageEvent.class,
            ViewportEvent.ComputeCameraAngles.class, ViewportEvent.ComputeFogColor.class, ViewportEvent.RenderFog.class,
            RenderPlayerEvent.Pre.class, RenderPlayerEvent.Post.class,
            RenderArmEvent.class, RenderHandEvent.class,
            InputEvent.Key.class, InputEvent.MouseButton.Pre.class, InputEvent.MouseButton.Post.class,
            InputEvent.MouseScrollingEvent.class, InputEvent.InteractionKeyMappingTriggered.class,
            RenderGuiLayerEvent.Pre.class, RenderGuiLayerEvent.Post.class,
            RenderGuiEvent.Pre.class, RenderGuiEvent.Post.class,
            RenderFrameEvent.Pre.class, RenderFrameEvent.Post.class,
            RenderHighlightEvent.Block.class,
            CustomizeGuiOverlayEvent.BossEventProgress.class,
            ScreenEvent.Opening.class,
            SelectMusicEvent.class,
            MovementInputUpdateEvent.class,
            ComputeFovModifierEvent.class,
            CalculateDetachedCameraDistanceEvent.class,
            RenderNameTagEvent.class,
            RegisterClientCommandsEvent.class,
            FMLClientSetupEvent.class);
    private static final Priority[] PROBED = {Priority.HIGH, Priority.NORMAL, Priority.LOWEST};
    private static final Map<Class<?>, Probe> PROBES = new LinkedHashMap<>();

    private BridgeSelfTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("enhanced.bridgeSelfTest");
    }

    /** Registers the probes and the command, if the self-test was asked for (the client's setup calls it). */
    public static void install(ClientRegistrar registrar) {
        if (!enabled()) {
            return;
        }
        for (Class<?> type : TYPES) {
            probe(type);
        }
        registrar.clientCommands(BridgeSelfTest::register);
        Enhanced.LOGGER.info("Bridge self-test: listening to {} client events", TYPES.size());
    }

    private static <E> void probe(Class<E> type) {
        Probe probe = new Probe(type);
        PROBES.put(type, probe);
        EventHub<E> hub = EventHub.of(type);
        for (Priority priority : PROBED) {
            hub.register(priority, true, event -> probe.seen(priority, event), "BridgeSelfTest " + priority);
        }
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("enhanced_bridges").executes(ctx -> {
            List<String> lines = report();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                lines.forEach(line -> mc.player.displayClientMessage(Component.literal(line), false));
            }
            return 1;
        }));
    }

    /** The result: a summary line, then one line per event. */
    static synchronized List<String> report() {
        List<String> lines = new ArrayList<>();
        int seen = 0;
        int bad = 0;
        List<String> missing = new ArrayList<>();
        for (Probe probe : PROBES.values()) {
            if (probe.rounds > 0) {
                seen++;
            } else {
                missing.add(probe.name());
            }
            if (!probe.problems.isEmpty()) {
                bad++;
            }
        }
        lines.add("Bridge self-test: " + seen + "/" + PROBES.size() + " client events seen, " + bad
                + " with problems" + (missing.isEmpty() ? "" : "; not seen yet: " + String.join(", ", missing)));
        for (Probe probe : PROBES.values()) {
            lines.add("  " + probe.name() + ": " + probe.rounds + " rounds, out of order " + probe.disorder
                    + (probe.problems.isEmpty() ? "" : ", problems: " + String.join("; ", probe.problems)));
        }
        return lines;
    }

    private static synchronized void write() {
        try {
            Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("bridge-selftest.txt");
            Files.write(file, report());
        } catch (IOException | RuntimeException e) {
            Enhanced.LOGGER.warn("Bridge self-test: cannot write its report", e);
        }
    }

    /** What arrived of one event type. */
    private static final class Probe {
        private final Class<?> type;
        /** Times the event reached all three priorities in order. */
        private int rounds;
        /** Times a priority came out of order (NORMAL not after HIGH, LOWEST not after NORMAL). */
        private int disorder;
        private Priority last = Priority.LOWEST;
        private boolean checked;
        private final List<String> problems = new ArrayList<>();

        Probe(Class<?> type) {
            this.type = type;
        }

        String name() {
            String outer = type.getEnclosingClass() == null ? "" : type.getEnclosingClass().getSimpleName() + ".";
            return outer + type.getSimpleName();
        }

        void seen(Priority priority, Object event) {
            boolean first;
            synchronized (BridgeSelfTest.class) {
                Priority expectedBefore = switch (priority) {
                    case HIGH -> Priority.LOWEST;
                    case NORMAL -> Priority.HIGH;
                    default -> Priority.NORMAL;
                };
                if (last != expectedBefore) {
                    disorder++;
                }
                last = priority;
                first = false;
                if (priority == Priority.LOWEST) {
                    rounds++;
                    first = rounds == 1;
                }
                if (!type.isInstance(event)) {
                    problems.add("got a " + event.getClass().getName());
                }
                if (!checked) {
                    checked = true;
                    getters(event);
                }
            }
            if (first) {
                Enhanced.LOGGER.info("Bridge self-test: {} arrived", name());
                write();
            }
        }

        /** Every public getter of the event answers without throwing. */
        private void getters(Object event) {
            for (Method method : type.getMethods()) {
                String n = method.getName();
                if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())
                        || method.getDeclaringClass() == Object.class
                        || !(n.startsWith("get") || n.startsWith("is") || n.startsWith("can"))) {
                    continue;
                }
                try {
                    method.invoke(event);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    problems.add(n + "() threw " + e);
                }
            }
        }
    }
}
