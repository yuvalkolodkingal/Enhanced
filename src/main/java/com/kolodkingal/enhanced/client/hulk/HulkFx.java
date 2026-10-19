package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.network.HulkFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link HulkFxPayload} (3.6): lane L7 registers the drawer of each kind (from the client setup),
 * and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by the 3.6
 * skeleton (as {@code AsgardFx} in 3.5).
 */
public final class HulkFx {
    private static final Map<Integer, Consumer<HulkFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link HulkFxPayload}). */
    public static void register(int kind, Consumer<HulkFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(HulkFxPayload payload) {
        Consumer<HulkFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private HulkFx() {
    }
}
