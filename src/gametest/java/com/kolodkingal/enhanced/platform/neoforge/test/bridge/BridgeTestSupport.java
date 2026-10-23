package com.kolodkingal.enhanced.platform.neoforge.test.bridge;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.neoforge.bridge.Binding;
import com.kolodkingal.enhanced.platform.neoforge.bridge.NeoForgeBridge;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The BridgeTests' tools (port P0, S-A): does a NeoForge event reach the mod's listeners of its common event, with the
 * right data, in priority order around NeoForge's own listeners, and does a change or a cancellation made on the
 * common event reach the NeoForge event?
 *
 * <p>{@link #bridge} does the part every event shares: it adapts a NeoForge event and compares every getter of the
 * common event with the NeoForge method of the same name; it posts one on NeoForge's bus with a hub listener at HIGH,
 * a NeoForge listener at NORMAL, hub listeners at LOW and LOWEST (receiving cancelled events), and checks they ran in
 * that order, once each; and for a cancellable event it posts another, cancels it from the HIGH listener, and checks
 * that NeoForge saw it cancelled, LOW was skipped and LOWEST was not. A test then checks its event's setters on the
 * returned pair ({@link Bridged#common()} writes through to {@link Bridged#neo()}).
 *
 * <p>Everything is registered, posted and unregistered within one call on the server thread, so no other event of the
 * type reaches the test's listeners. The mod's own listeners see the posted events too: tests choose data they ignore
 * (players without powers, plain mobs in the arena).
 */
final class BridgeTestSupport {
    static final String BATCH = "bridge";
    private static final String PLATFORM = "com.kolodkingal.enhanced.platform.";

    private BridgeTestSupport() {
    }

    /** A common event adapted from a NeoForge event: what the listeners get, and the event NeoForge has. */
    record Bridged<C, N>(C common, N neo) {
    }

    /** The generic checks (getters, order, cancellation; see the class); returns a fresh adapted pair. */
    static <C, N extends Event> Bridged<C, N> bridge(Class<C> common, Supplier<N> factory, String... skipGetters) {
        N neo = factory.get();
        C adapted = adapt(common, neo);
        getters(adapted, neo, Set.of(skipGetters));
        order(common, factory.get());
        if (Cancellable.class.isAssignableFrom(common)) {
            cancellation(common, factory.get());
        }
        N fresh = factory.get();
        return new Bridged<>(adapt(common, fresh), fresh);
    }

    /**
     * The adapter's checks only (getters and setters, no post): for the events a test must not post on the live
     * server's bus (the server's lifecycle and ticks, a level's unloading), which the mod and NeoForge act on.
     */
    static <C, N extends Event> Bridged<C, N> adapted(Class<C> common, N neo, String... skipGetters) {
        C adapted = adapt(common, neo);
        getters(adapted, neo, Set.of(skipGetters));
        return new Bridged<>(adapted, neo);
    }

    /** The bridge's adapter applied to a NeoForge event. */
    @SuppressWarnings("unchecked")
    static <C, N extends Event> C adapt(Class<C> common, N neo) {
        Binding<C, N> binding = (Binding<C, N>) NeoForgeBridge.binding(common)
                .orElseThrow(() -> new GameTestAssertException("no NeoForge event for " + common.getName()));
        check(binding.nativeType().isInstance(neo), "the NeoForge event of " + common.getSimpleName() + " is a "
                + binding.nativeType().getName());
        check(binding.cancellable() == Cancellable.class.isAssignableFrom(common), common.getSimpleName()
                + " is cancellable exactly when NeoForge's is");
        C adapted = binding.adapter().apply(neo);
        check(common.isInstance(adapted), "the adapter gives a " + common.getSimpleName());
        return adapted;
    }

    /** Every getter of the common event equals NeoForge's method of the same name (when NeoForge has one). */
    static void getters(Object common, Object neo, Set<String> skip) {
        int compared = 0;
        for (Method method : common.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() == void.class
                    || Modifier.isStatic(method.getModifiers())
                    || !method.getDeclaringClass().getName().startsWith(PLATFORM)
                    || skip.contains(method.getName())) {
                continue;
            }
            Method other;
            try {
                other = neo.getClass().getMethod(method.getName());
            } catch (NoSuchMethodException e) {
                continue;
            }
            Object mine = call(method, common);
            Object theirs = call(other, neo);
            check(same(mine, theirs), common.getClass().getSuperclass().getSimpleName() + "." + method.getName()
                    + "() is " + mine + ", NeoForge's is " + theirs);
            compared++;
        }
        check(compared > 0 || skip.contains("*none*"), "some getter of " + common.getClass().getName()
                + " was compared");
    }

    private static Object call(Method method, Object target) {
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new GameTestAssertException("cannot call " + method + ": " + e);
        }
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof Enum<?> ea && b instanceof Enum<?> eb) {
            return ea.name().equals(eb.name());
        }
        if (a instanceof Stream<?> sa && b instanceof Stream<?> sb) {
            return sa.toList().equals(sb.toList());
        }
        if (a instanceof Optional<?> oa && b instanceof Optional<?> ob) {
            return oa.equals(ob);
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return na.doubleValue() == nb.doubleValue();
        }
        return Objects.equals(a, b);
    }

    /** What the test's listeners saw of one post. */
    record Run<C>(List<String> order, C seen, boolean neoSawCanceled) {
    }

    /** Posts a NeoForge event with the test's listeners around it ({@code atHigh} runs in the HIGH listener). */
    @SuppressWarnings("unchecked")
    static <C, N extends Event> Run<C> post(Class<C> common, N neo, Consumer<C> atHigh) {
        List<String> order = new ArrayList<>();
        List<C> seen = new ArrayList<>();
        boolean[] neoSawCanceled = new boolean[1];
        boolean cancellable = neo instanceof ICancellableEvent;
        EventHub<C> hub = EventHub.of(common);
        Consumer<N> nativeListener = e -> {
            if (e == neo) {
                order.add("NATIVE");
                neoSawCanceled[0] = cancellable && ((ICancellableEvent) e).isCanceled();
            }
        };
        try (EventHub.Registration high = hub.register(Priority.HIGH, false, c -> {
            order.add("HIGH");
            seen.add(c);
            atHigh.accept(c);
        }, "bridge-test HIGH");
             EventHub.Registration low = hub.register(Priority.LOW, false, c -> order.add("LOW"), "bridge-test LOW");
             EventHub.Registration lowest = hub.register(Priority.LOWEST, true, c -> order.add("LOWEST"),
                     "bridge-test LOWEST")) {
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, cancellable, (Class<N>) neo.getClass(),
                    nativeListener);
            try {
                NeoForge.EVENT_BUS.post(neo);
            } finally {
                NeoForge.EVENT_BUS.unregister(nativeListener);
            }
        }
        check(seen.size() <= 1, "the HIGH listener ran once: " + order);
        return new Run<>(order, seen.isEmpty() ? null : seen.get(0), neoSawCanceled[0]);
    }

    /** Hub HIGH, NeoForge NORMAL, hub LOW, hub LOWEST: once each, in that order. */
    static <C, N extends Event> void order(Class<C> common, N neo) {
        Run<C> run = post(common, neo, c -> {
        });
        check(run.order().equals(List.of("HIGH", "NATIVE", "LOW", "LOWEST")), common.getSimpleName()
                + ": the listeners ran in priority order around NeoForge's: " + run.order());
        check(common.isInstance(run.seen()), "the listeners got a " + common.getSimpleName());
    }

    /** Cancelled at HIGH: NeoForge's listener (receiving cancelled events) sees it, LOW is skipped, LOWEST is not. */
    static <C, N extends Event> void cancellation(Class<C> common, N neo) {
        Run<C> run = post(common, neo, c -> ((Cancellable) c).setCanceled(true));
        check(run.order().equals(List.of("HIGH", "NATIVE", "LOWEST")), common.getSimpleName()
                + ": a cancelled event skips LOW only: " + run.order());
        check(run.neoSawCanceled(), common.getSimpleName() + ": NeoForge's listener sees the cancellation");
        check(((ICancellableEvent) neo).isCanceled(), common.getSimpleName() + ": the NeoForge event is cancelled");
        check(((Cancellable) run.seen()).isCanceled(), "the common event reads it back");
        ((Cancellable) run.seen()).setCanceled(false);
        check(!((ICancellableEvent) neo).isCanceled(), common.getSimpleName() + ": un-cancelling reaches NeoForge");
    }
}
