package com.kolodkingal.enhanced.client.redhulk;

import com.kolodkingal.enhanced.network.RedHulkFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link RedHulkFxPayload} (3.8): lane L2 registers the drawer of each kind (from the client
 * setup), and an effect that arrives goes to the drawer of its kind; a kind nobody registered is ignored. Written by
 * the 3.8 skeleton.
 */
public final class RedHulkFx {
    private static final Map<Integer, Consumer<RedHulkFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link RedHulkFxPayload}). */
    public static void register(int kind, Consumer<RedHulkFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(RedHulkFxPayload payload) {
        Consumer<RedHulkFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private RedHulkFx() {
    }
}
