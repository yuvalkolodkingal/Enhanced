package com.kolodkingal.enhanced.client.stones;

import com.kolodkingal.enhanced.network.StoneFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link StoneFxPayload} (3.6): lanes L1, L2 and L5 (each its own kinds) register the drawer of
 * each kind (from the client setup), and an effect that arrives goes to the drawer of its kind; a kind nobody
 * registered is ignored. Written by the 3.6 skeleton (as {@code AsgardFx} in 3.5).
 */
public final class StoneFx {
    private static final Map<Integer, Consumer<StoneFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link StoneFxPayload}). */
    public static void register(int kind, Consumer<StoneFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(StoneFxPayload payload) {
        Consumer<StoneFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private StoneFx() {
    }
}
