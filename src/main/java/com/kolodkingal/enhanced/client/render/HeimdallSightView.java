package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.network.HeimdallSightPayload;

/**
 * What Heimdall's All-Seeing Sight shows on his client (3.5): the far players' markers and the list of the other realms.
 * Written by the 3.5 skeleton as the hook the payload calls; lane L2 owns and fills it.
 */
public final class HeimdallSightView {

    /** The server's latest list of far players. */
    public static void receive(HeimdallSightPayload payload) {
    }

    private HeimdallSightView() {
    }
}
