package com.kolodkingal.enhanced.platform.neoforge.bridge;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Events;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.neoforge.bridge.client.ClientBindings;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The NeoForge side of the common events: NeoForge's events become the mod's {@link EventHub} events.
 *
 * <p>When the mod is constructed (right after NeoForge registered its {@code @EventBusSubscriber} classes) the bridge
 * installs the bindings ({@link GameBindings}, and {@link ClientBindings} on a client), becomes the hubs'
 * {@link EventHub.Activator} and registers the generated {@code @Subscribe} listeners. From then on, the first listener
 * of each priority of a hub registers one native listener of that priority on NeoForge's bus (the mod's bus for a
 * mod-bus event), with {@code receiveCanceled} for a cancellable event; it adapts the NeoForge event and posts it to
 * that priority's listeners, in the hub's order. So the mod's listeners keep their priority among other mods'
 * listeners, a cancellation made by anyone is seen by everyone, and every change a listener makes is made on the
 * NeoForge event itself.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class NeoForgeBridge {
    private static final Map<Class<?>, Binding<?, ?>> BINDINGS = new HashMap<>();
    private static IEventBus modBus;

    private NeoForgeBridge() {
    }

    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
        install();
    }

    /** Installs the bridge once (the mod's construction; idempotent). */
    public static synchronized void install() {
        if (modBus != null) {
            return;
        }
        modBus = ModList.get().getModContainerById(Enhanced.MODID).orElseThrow().getEventBus();
        GameBindings.install(BINDINGS);
        boolean client = FMLEnvironment.dist.isClient();
        if (client) {
            ClientBindings.install(BINDINGS);
        }
        EventHub.setActivator(NeoForgeBridge::activate);
        Events.registerGenerated(client ? Side.CLIENT : Side.SERVER);
        ListenerOrderRecorder.install(modBus);
    }

    /** The binding of a common event type, if NeoForge has its event. */
    public static Optional<Binding<?, ?>> binding(Class<?> common) {
        return Optional.ofNullable(BINDINGS.get(common));
    }

    /** Every binding installed on this side. */
    public static Collection<Binding<?, ?>> bindings() {
        return List.copyOf(BINDINGS.values());
    }

    /** The NeoForge event type each common event type comes from. */
    static Map<Class<?>, Class<?>> commonByNative() {
        Map<Class<?>, Class<?>> map = new HashMap<>();
        BINDINGS.values().forEach(b -> map.put(b.nativeType(), b.common()));
        return map;
    }

    private static void activate(EventHub<?> hub, Priority priority) {
        Binding<?, ?> binding = BINDINGS.get(hub.type());
        if (binding == null) {
            Enhanced.LOGGER.debug("No NeoForge event for {}: its listeners run only when it is posted", hub.type());
            return;
        }
        listen(binding, hub, priority);
    }

    @SuppressWarnings("unchecked")
    private static <C, N extends Event> void listen(Binding<C, N> binding, EventHub<?> hub, Priority priority) {
        IEventBus bus = binding.modBus() ? modBus : NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.values()[priority.ordinal()], binding.cancellable(), binding.nativeType(),
                new BridgeListener<>((EventHub<C>) hub, priority, binding.adapter()));
    }

    /** The native listener of one priority of one hub (named, so the listener-order recorder can see through it). */
    static final class BridgeListener<C, N extends Event> implements Consumer<N> {
        final EventHub<C> hub;
        final Priority priority;
        private final Function<N, ? extends C> adapter;

        BridgeListener(EventHub<C> hub, Priority priority, Function<N, ? extends C> adapter) {
            this.hub = hub;
            this.priority = priority;
            this.adapter = adapter;
        }

        @Override
        public void accept(N event) {
            if (hub.hasListeners(priority)) {
                hub.post(priority, adapter.apply(event));
            }
        }

        @Override
        public String toString() {
            return "Enhanced bridge: " + hub.type().getName() + " " + priority;
        }
    }
}
