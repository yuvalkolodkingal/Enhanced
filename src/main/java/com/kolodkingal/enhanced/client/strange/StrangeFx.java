package com.kolodkingal.enhanced.client.strange;

import com.kolodkingal.enhanced.network.StrangeFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link StrangeFxPayload} (3.6): lane L3 registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.6 skeleton (as {@code AsgardFx} in 3.5).
 */
public final class StrangeFx {
    private static final Map<Integer, Consumer<StrangeFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link StrangeFxPayload}). */
    public static void register(int kind, Consumer<StrangeFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(StrangeFxPayload payload) {
        Consumer<StrangeFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private StrangeFx() {
    }
}
