package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.network.IronFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link IronFxPayload} (3.8): lane L3 (L4, L7) registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.8 skeleton.
 */
public final class IronFx {
    private static final Map<Integer, Consumer<IronFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link IronFxPayload}). */
    public static void register(int kind, Consumer<IronFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(IronFxPayload payload) {
        Consumer<IronFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private IronFx() {
    }
}
