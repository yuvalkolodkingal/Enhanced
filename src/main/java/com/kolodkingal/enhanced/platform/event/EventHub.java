package com.kolodkingal.enhanced.platform.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The listeners of one common event type, by {@link Priority}, and the way to post the event to them. There is one hub
 * per type ({@link #of}); the generated subscriptions, the loader's bridge and the tests all use the same one.
 *
 * <p>Semantics, as NeoForge's bus: priorities run from {@link Priority#HIGHEST} to {@link Priority#LOWEST}; within a
 * priority, listeners run in the order they were registered; a listener of a {@link Cancellable} event is skipped once
 * the event is cancelled, unless it was registered with {@code receiveCanceled}. A listener's exception goes to whoever
 * posted the event.
 *
 * <p>The loader's bridge learns which priorities are in use through the {@link Activator}: the first listener of a
 * priority of a hub activates it, once, and the bridge then calls {@link #post(Priority, Object)} for that
 * priority from a native listener of the same priority. ({@link #post(Object)} runs every priority at once: tests
 * and loaders without a bus of their own.)
 *
 * @param <E> the common event type
 */
public final class EventHub<E> {
    private static final Map<Class<?>, EventHub<?>> HUBS = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();
    private static final Priority[] PRIORITIES = Priority.values();
    private static Activator activator;

    private final Class<E> type;
    private final boolean cancellable;
    /** The listeners of each priority (by ordinal); replaced, never changed in place, so posting needs no lock. */
    private volatile Listener<E>[][] phases;
    private final boolean[] active = new boolean[PRIORITIES.length];

    @SuppressWarnings("unchecked")
    private EventHub(Class<E> type) {
        this.type = type;
        this.cancellable = Cancellable.class.isAssignableFrom(type);
        this.phases = new Listener[PRIORITIES.length][0];
    }

    /** The hub of a common event type (made on first use). */
    @SuppressWarnings("unchecked")
    public static <E> EventHub<E> of(Class<E> type) {
        return (EventHub<E>) HUBS.computeIfAbsent(type, t -> new EventHub<>(type));
    }

    /** Every hub made so far. */
    public static Collection<EventHub<?>> all() {
        return List.copyOf(HUBS.values());
    }

    /**
     * Installs the loader's bridge: from now on the first listener of each priority of each hub is reported to it, and
     * the priorities already in use are reported now.
     */
    public static void setActivator(Activator newActivator) {
        synchronized (LOCK) {
            activator = Objects.requireNonNull(newActivator);
            for (EventHub<?> hub : HUBS.values()) {
                for (Priority priority : PRIORITIES) {
                    if (hub.phases[priority.ordinal()].length > 0) {
                        hub.activate(priority);
                    }
                }
            }
        }
    }

    public Class<E> type() {
        return type;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    /** A listener of {@link Priority#NORMAL} that does not receive cancelled events. */
    public Registration register(Consumer<? super E> listener) {
        return register(Priority.NORMAL, false, listener, null);
    }

    public Registration register(Priority priority, Consumer<? super E> listener) {
        return register(priority, false, listener, null);
    }

    public Registration register(Priority priority, boolean receiveCanceled, Consumer<? super E> listener) {
        return register(priority, receiveCanceled, listener, null);
    }

    /**
     * Adds a listener after the ones of its priority. {@code name} names it in dumps and order checks (the generated
     * subscriptions pass {@code "<class>#<method>"}); without one, the listener's own {@code toString} does.
     */
    public Registration register(Priority priority, boolean receiveCanceled, Consumer<? super E> listener,
                                 String name) {
        Listener<E> entry = new Listener<>(priority, receiveCanceled, Objects.requireNonNull(listener),
                name != null ? name : listener.toString());
        synchronized (LOCK) {
            Listener<E>[][] next = phases.clone();
            Listener<E>[] old = next[priority.ordinal()];
            Listener<E>[] grown = Arrays.copyOf(old, old.length + 1);
            grown[old.length] = entry;
            next[priority.ordinal()] = grown;
            phases = next;
            activate(priority);
        }
        return () -> unregister(entry);
    }

    private void unregister(Listener<E> entry) {
        synchronized (LOCK) {
            Listener<E>[][] next = phases.clone();
            Listener<E>[] old = next[entry.priority().ordinal()];
            List<Listener<E>> kept = new ArrayList<>(Arrays.asList(old));
            if (!kept.remove(entry)) {
                return;
            }
            next[entry.priority().ordinal()] = kept.toArray(Arrays.copyOf(old, 0));
            phases = next;
        }
    }

    private void activate(Priority priority) {
        if (activator != null && !active[priority.ordinal()]) {
            active[priority.ordinal()] = true;
            activator.activate(this, priority);
        }
    }

    /** Whether any listener of this priority is registered now. */
    public boolean hasListeners(Priority priority) {
        return phases[priority.ordinal()].length > 0;
    }

    /** The listeners of a priority, in the order they run. */
    public List<Listener<E>> listeners(Priority priority) {
        return List.of(phases[priority.ordinal()]);
    }

    /** Posts the event to every listener, priority by priority; returns it (read {@link Cancellable#isCanceled}). */
    public E post(E event) {
        Listener<E>[][] current = phases;
        for (Listener<E>[] phase : current) {
            run(phase, event);
        }
        return event;
    }

    /** Posts the event to the listeners of one priority only (the bridge's native listener of that priority). */
    public E post(Priority priority, E event) {
        run(phases[priority.ordinal()], event);
        return event;
    }

    private void run(Listener<E>[] phase, E event) {
        if (phase.length == 0) {
            return;
        }
        Cancellable cancel = cancellable && event instanceof Cancellable c ? c : null;
        for (Listener<E> listener : phase) {
            if (cancel == null || listener.receiveCanceled() || !cancel.isCanceled()) {
                listener.consumer().accept(event);
            }
        }
    }

    @Override
    public String toString() {
        return "EventHub[" + type.getName() + "]";
    }

    /** One listener of a hub. */
    public record Listener<E>(Priority priority, boolean receiveCanceled, Consumer<? super E> consumer, String name) {
    }

    /** A registered listener; {@link #close} removes it (tests register listeners for one test). */
    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    /** The loader's bridge: told the first time a priority of a hub gets a listener. */
    @FunctionalInterface
    public interface Activator {
        void activate(EventHub<?> hub, Priority priority);
    }
}
