package com.kolodkingal.enhanced.platform.neoforge.bridge;

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
public record Binding<C, N extends Event>(Class<C> common, Class<N> nativeType, Function<N, ? extends C> adapter) {
    public static <C, N extends Event> void put(Map<Class<?>, Binding<?, ?>> table, Class<C> common,
                                                Class<N> nativeType, Function<N, ? extends C> adapter) {
        if (table.put(common, new Binding<>(common, nativeType, adapter)) != null) {
            throw new IllegalStateException("two bindings for " + common.getName());
        }
    }

    public boolean cancellable() {
        return ICancellableEvent.class.isAssignableFrom(nativeType);
    }

    public boolean modBus() {
        return IModBusEvent.class.isAssignableFrom(nativeType);
    }
}
