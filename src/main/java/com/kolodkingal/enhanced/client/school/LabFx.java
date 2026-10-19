package com.kolodkingal.enhanced.client.school;

import com.kolodkingal.enhanced.network.LabFxPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link LabFxPayload} (3.7): lane L4 registers the drawer of each kind (L3 the pod's, L5 the
 * console's; from the client setup), and an effect that arrives goes to the drawer of its kind; a kind nobody
 * registered is ignored. Written by the 3.7 skeleton.
 */
public final class LabFx {
    private static final Map<Integer, Consumer<LabFxPayload>> DRAWERS = new ConcurrentHashMap<>();

    /** The drawer of one kind (a constant of {@link LabFxPayload}). */
    public static void register(int kind, Consumer<LabFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    public static void receive(LabFxPayload payload) {
        Consumer<LabFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    private LabFx() {
    }
}
