package com.kolodkingal.enhanced.client.wolverine;

import com.kolodkingal.enhanced.network.ScentPayload;

import java.util.function.Consumer;

/**
 * The client's side of {@link ScentPayload} (3.7): what Wolverine smells. Lane L2 sets the {@link #handler} (from the
 * client setup) that keeps the glowing creatures and draws the trails; until then a report is ignored. Written by the
 * 3.7 skeleton.
 */
public final class Scents {
    private static volatile Consumer<ScentPayload> handler = payload -> {
    };

    /** Lane L2: who takes the reports. */
    public static void handler(Consumer<ScentPayload> consumer) {
        handler = consumer;
    }

    public static void receive(ScentPayload payload) {
        handler.accept(payload);
    }

    private Scents() {
    }
}
