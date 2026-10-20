package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.network.FalconFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link FalconFxPayload} (3.8): lane L5 registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.8 skeleton.
 */
public final class FalconFx {
    private static final Map<Integer, Consumer<FalconFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link FalconFxPayload}). */
    public static void register(int kind, Consumer<FalconFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(FalconFxPayload payload) {
        Consumer<FalconFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private FalconFx() {
    }
}
