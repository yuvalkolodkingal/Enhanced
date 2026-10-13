package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.network.MagnetPullPayload;

/**
 * What every client draws of Magneto's Magnetic Pull (D-M 8.3): the colourless pulse of his field growing to the pull's
 * radius, and each thing taken flying to his hand as its item along a line of force, with a soft clink when it arrives.
 * At most 128 streams alive; cleared on logout.
 *
 * <p>A stub of the skeleton (3.1, step S): it draws nothing yet. Lane MAG fills {@code accept} and {@code clear}, adds a
 * {@code render} called from {@code WorldEffectsRenderer.onRenderLevel}, and calls {@code clear} beside {@code
 * WorldEffectsRenderer.clearHexBolts()} at logout. Only ever loaded on a client, from the handler of {@link
 * MagnetPullPayload}.
 */
public final class MagnetStreams {
    /** One tick of a pull. Lane MAG: start the pulse ({@code first}) and the streams. */
    public static void accept(MagnetPullPayload payload) {
    }

    /** Forgets every pulse and stream (at logout). Lane MAG: called from {@code ClientEvents.onLogout}. */
    public static void clear() {
    }

    private MagnetStreams() {
    }
}
