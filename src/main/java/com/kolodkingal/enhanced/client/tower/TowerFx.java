package com.kolodkingal.enhanced.client.tower;

import com.kolodkingal.enhanced.network.TowerFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link TowerFxPayload} (3.8): lane L6 (L7 for the gantry) registers the drawer of each kind, and
 * an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by the 3.8
 * skeleton.
 */
public final class TowerFx {
    private static final Map<Integer, Consumer<TowerFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link TowerFxPayload}). */
    public static void register(int kind, Consumer<TowerFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(TowerFxPayload payload) {
        Consumer<TowerFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private TowerFx() {
    }
}
