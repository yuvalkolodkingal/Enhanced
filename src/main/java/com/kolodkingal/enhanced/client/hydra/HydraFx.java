package com.kolodkingal.enhanced.client.hydra;

import com.kolodkingal.enhanced.network.HydraFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link HydraFxPayload} (3.6): lane L8 registers the drawer of each kind (from the client setup),
 * and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by the 3.6
 * skeleton (as {@code AsgardFx} in 3.5).
 */
public final class HydraFx {
    private static final Map<Integer, Consumer<HydraFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link HydraFxPayload}). */
    public static void register(int kind, Consumer<HydraFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(HydraFxPayload payload) {
        Consumer<HydraFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private HydraFx() {
    }
}
