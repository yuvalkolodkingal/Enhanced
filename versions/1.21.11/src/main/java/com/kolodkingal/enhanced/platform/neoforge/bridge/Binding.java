package com.kolodkingal.enhanced.platform.neoforge.bridge;
//#norenames

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.event.IModBusEvent;

import java.util.Map;
import java.util.function.Function;

/**
 * How one NeoForge event becomes its common event: the two types and the adapter. A cancellable NeoForge event is
 * listened to with {@code receiveCanceled} (the hub skips the listeners that do not want it); a mod-bus event is
 * listened to on the mod's bus.
 *
 * @param <C> the common event type
 * @param <N> the NeoForge event type
 */
public record Binding<C, N extends Event>(Class<C> common, Class<N> nativeType, Function<N, ?> adapter,
                                          java.util.List<Class<? extends N>> listened) {
    public static <C, N extends Event> void put(Map<Class<?>, Binding<?, ?>> table, Class<C> common,
                                                Class<N> nativeType, Function<N, ? extends C> adapter) {
        putAll(table, common, nativeType, java.util.List.of(nativeType), adapter);
    }

    /**
     * (1.21.11) A common event made from any of several NeoForge events of one (abstract) type: the concrete ones are
     * listened to. The adapter may give {@link Many} common events for one NeoForge event.
     */
    public static <C, N extends Event> void putAll(Map<Class<?>, Binding<?, ?>> table, Class<C> common, Class<N> nativeType,
                                                   java.util.List<Class<? extends N>> listened, Function<N, ?> adapter) {
        if (table.put(common, new Binding<>(common, nativeType, adapter, listened)) != null) {
            throw new IllegalStateException("two bindings for " + common.getName());
        }
    }

    /** (1.21.11) What an adapter gives for a NeoForge event that is several common ones, posted in order. */
    public record Many(java.util.List<?> events) {
    }

    public boolean cancellable() {
        return ICancellableEvent.class.isAssignableFrom(nativeType);
    }

    public boolean modBus() {
        return IModBusEvent.class.isAssignableFrom(nativeType);
    }
}
