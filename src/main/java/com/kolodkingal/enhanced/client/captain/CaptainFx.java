package com.kolodkingal.enhanced.client.captain;

import com.kolodkingal.enhanced.network.CaptainFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link CaptainFxPayload} (3.8): lane L1 registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.8 skeleton.
 */
public final class CaptainFx {
    private static final Map<Integer, Consumer<CaptainFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link CaptainFxPayload}). */
    public static void register(int kind, Consumer<CaptainFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(CaptainFxPayload payload) {
        Consumer<CaptainFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private CaptainFx() {
    }
}
