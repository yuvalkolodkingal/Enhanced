package com.kolodkingal.enhanced.platform.event;

import com.kolodkingal.enhanced.platform.event.generated.GeneratedSubscriptions;

/**
 * Posting common events, and registering the generated {@link Subscribe} listeners.
 *
 * <p>Migrating a test: {@code NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player))} becomes
 * {@code Events.post(new BlockEvent.BreakEvent(level, pos, state, player))} with the common {@code BlockEvent}; a
 * test's own listener, {@code NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BreakEvent.class,
 * claim)}, becomes
 * {@code EventHub.of(BlockEvent.BreakEvent.class).register(Priority.NORMAL, false, claim)}, and its {@code unregister}
 * the returned registration's {@code close()}.
 */
public final class Events {
    private static final String PACKAGE = Events.class.getPackageName();
    /** The common event type of each class posted: the class itself, or the first of its supertypes in this package. */
    private static final ClassValue<Class<?>> COMMON_TYPE = new ClassValue<>() {
        @Override
        protected Class<?> computeValue(Class<?> type) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                if (!c.isAnonymousClass() && c.getPackageName().startsWith(PACKAGE)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(type.getName() + " is not a common event");
        }
    };
    private static boolean registered;

    private Events() {
    }

    /** Posts a common event to every listener of its type ({@link EventHub#post(Object)}) and returns it. */
    @SuppressWarnings("unchecked")
    public static <E> E post(E event) {
        return ((EventHub<E>) EventHub.of(COMMON_TYPE.get(event.getClass()))).post(event);
    }

    /** The common event type a posted object belongs to (a bridge's subclass belongs to the type it extends). */
    public static Class<?> commonType(Class<?> type) {
        return COMMON_TYPE.get(type);
    }

    /**
     * Registers every {@link Subscribe} listener the processor found, once, for this physical side: {@link Side#CLIENT}
     * on a game client (with its integrated server), {@link Side#SERVER} on a dedicated server.
     */
    public static synchronized void registerGenerated(Side physicalSide) {
        if (registered) {
            return;
        }
        registered = true;
        GeneratedSubscriptions.register(physicalSide);
    }
}
