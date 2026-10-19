package com.kolodkingal.enhanced.client.deadpool;

import com.kolodkingal.enhanced.network.DeadpoolFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link DeadpoolFxPayload} (3.7): lane L3 registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.7 skeleton.
 */
public final class DeadpoolFx {
    private static final Map<Integer, Consumer<DeadpoolFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link DeadpoolFxPayload}). */
    public static void register(int kind, Consumer<DeadpoolFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(DeadpoolFxPayload payload) {
        Consumer<DeadpoolFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private DeadpoolFx() {
    }
}
