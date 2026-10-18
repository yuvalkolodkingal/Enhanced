package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.network.AsgardTalkPayload;

/**
 * The talk screen of the Asgardians (3.5): the speaker's face and name, his line typed out, up to three answers. Written by
 * the 3.5 skeleton as the hook the payload calls; lane L5 owns it and makes the screen.
 */
public final class AsgardTalkScreen {

    /** The server opened a talk. */
    public static void open(AsgardTalkPayload.Open payload) {
    }

    private AsgardTalkScreen() {
    }
}
