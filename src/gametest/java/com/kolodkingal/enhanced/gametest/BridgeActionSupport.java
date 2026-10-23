package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The real-action BridgeTests' tool (port P0, L8; PLAN2 3.5): listeners on a common event's hub, for the duration of a
 * test, that count the events about the test's own things. Loader-neutral: whatever makes the game post the event
 * (NeoForge's bus through the bridge, Fabric's callbacks and hook mixins), the test sees it only if the real vanilla
 * action reached the hub.
 *
 * <p>A {@link Probe} listens at HIGH (where the test may act on the event: change it, cancel it), at LOW and at
 * LOWEST (receiving cancelled events), and records the order. Events not about the test's things (other tests of the
 * batch run at the same time) are ignored.
 */
final class BridgeActionSupport {
    static final String BATCH = "bridge_action";

    private BridgeActionSupport() {
    }

    /** What the test's listeners saw of one event type. */
    static final class Probe<E> {
        private final Class<E> type;
        private final List<E> seen = new ArrayList<>();
        private final List<String> order = new ArrayList<>();

        private Probe(Class<E> type) {
            this.type = type;
        }

        /** The events the HIGH listener saw, oldest first. */
        List<E> seen() {
            return seen;
        }

        /** HIGH, LOW, LOWEST of every event, in the order they ran. */
        List<String> order() {
            return order;
        }

        int count() {
            return seen.size();
        }

        /** The one event the action posted, after checking it came once, through every priority in order. */
        E once() {
            check(seen.size() == 1, type.getSimpleName() + " was posted once for the action: " + seen.size() + " times "
                    + order);
            E event = seen.get(0);
            boolean canceled = event instanceof Cancellable c && c.isCanceled();
            List<String> expected = canceled ? List.of("HIGH", "LOWEST") : List.of("HIGH", "LOW", "LOWEST");
            check(order.equals(expected), type.getSimpleName() + " ran " + order + ", not " + expected);
            return event;
        }

        /** That no event of the type was about the test's things. */
        void none(String why) {
            check(seen.isEmpty(), type.getSimpleName() + " was not posted " + why + ": " + order);
        }
    }

    /** A probe that ignores nothing about the event but what {@code mine} refuses. */
    static <E> Probe<E> probe(TestKit.Fixture fixture, Class<E> type, Predicate<? super E> mine) {
        return probe(fixture, type, mine, e -> {
        });
    }

    /**
     * A probe on {@code type} until the test ends; {@code atHigh} runs in the HIGH listener for each event about the
     * test's things ({@code mine}).
     */
    static <E> Probe<E> probe(TestKit.Fixture fixture, Class<E> type, Predicate<? super E> mine,
                              Consumer<? super E> atHigh) {
        Probe<E> probe = new Probe<>(type);
        EventHub<E> hub = EventHub.of(type);
        EventHub.Registration high = hub.register(Priority.HIGH, false, e -> {
            if (mine.test(e)) {
                probe.order.add("HIGH");
                probe.seen.add(e);
                atHigh.accept(e);
            }
        }, "bridge-action HIGH");
        EventHub.Registration low = hub.register(Priority.LOW, false, e -> {
            if (mine.test(e)) {
                probe.order.add("LOW");
            }
        }, "bridge-action LOW");
        EventHub.Registration lowest = hub.register(Priority.LOWEST, true, e -> {
            if (mine.test(e)) {
                probe.order.add("LOWEST");
            }
        }, "bridge-action LOWEST");
        fixture.onClose(() -> {
            high.close();
            low.close();
            lowest.close();
        });
        return probe;
    }

    /** Cancels a cancellable event (for {@code atHigh}). */
    static void cancel(Object event) {
        ((Cancellable) event).setCanceled(true);
    }
}
