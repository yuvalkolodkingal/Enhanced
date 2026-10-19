package com.kolodkingal.enhanced.client.wolverine;

import com.kolodkingal.enhanced.network.WolverineFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link WolverineFxPayload} (3.7): lane L2 registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.7 skeleton.
 */
public final class WolverineFx {
    private static final Map<Integer, Consumer<WolverineFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link WolverineFxPayload}). */
    public static void register(int kind, Consumer<WolverineFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(WolverineFxPayload payload) {
        Consumer<WolverineFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private WolverineFx() {
    }
}
