package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.network.AsgardFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link AsgardFxPayload} (3.5): each lane registers the drawer of its kinds (from its client setup),
 * and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by the 3.5
 * skeleton; shared (each lane registers only its own range).
 */
public final class AsgardFx {
    private static final Map<Integer, Consumer<AsgardFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (in the lane's range of {@link AsgardFxPayload}). */
    public static void register(int kind, Consumer<AsgardFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(AsgardFxPayload payload) {
        Consumer<AsgardFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private AsgardFx() {
    }
}
