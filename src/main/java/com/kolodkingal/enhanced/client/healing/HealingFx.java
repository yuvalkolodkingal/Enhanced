package com.kolodkingal.enhanced.client.healing;

import com.kolodkingal.enhanced.network.HealingFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link HealingFxPayload} (3.7): lane L1 registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.7 skeleton (as {@code HulkFx} in 3.6).
 */
public final class HealingFx {
    private static final Map<Integer, Consumer<HealingFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link HealingFxPayload}). */
    public static void register(int kind, Consumer<HealingFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(HealingFxPayload payload) {
        Consumer<HealingFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private HealingFx() {
    }
}
