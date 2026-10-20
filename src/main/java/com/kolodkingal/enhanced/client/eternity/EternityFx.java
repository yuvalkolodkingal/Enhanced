package com.kolodkingal.enhanced.client.eternity;

import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.network.EternityScreenPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The client's side of {@link EternityFxPayload} and {@link EternityScreenPayload} (3.8): lane L8 registers the drawer
 * of each kind and the wish screen's opener (from the client setup); a kind nobody registered is ignored, and the
 * screen opens nothing until L8 sets its opener. Written by the 3.8 skeleton.
 */
public final class EternityFx {
    private static final Map<Integer, Consumer<EternityFxPayload>> DRAWERS = new ConcurrentHashMap<>();
    private static volatile Consumer<EternityScreenPayload> screen = payload -> {
    };

    /** The drawer of one kind (a constant of {@link EternityFxPayload}). */
    public static void register(int kind, Consumer<EternityFxPayload> drawer) {
        DRAWERS.put(kind, drawer);
    }

    /** What opens the wish screen (lane L8's {@code WishScreen}). */
    public static void screen(Consumer<EternityScreenPayload> opener) {
        screen = opener;
    }

    public static void receive(EternityFxPayload payload) {
        Consumer<EternityFxPayload> drawer = DRAWERS.get(payload.kind());
        if (drawer != null) {
            drawer.accept(payload);
        }
    }

    public static void open(EternityScreenPayload payload) {
        screen.accept(payload);
    }

    private EternityFx() {
    }
}
