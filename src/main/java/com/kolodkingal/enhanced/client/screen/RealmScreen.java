package com.kolodkingal.enhanced.client.screen;

import com.kolodkingal.enhanced.network.RealmScreenPayload;

/**
 * The Space Stone's Realm Picker (3.6): four realms to choose from (the Overworld, the Nether, the End, Asgard), drawn
 * as windows on a starfield. A plain class until lane L2 makes it its Screen (keeping the static {@link #open}).
 */
public final class RealmScreen {

    /** The server asks for the screen with the realms it allows now. */
    public static void open(RealmScreenPayload payload) {
    }

    private RealmScreen() {
    }
}
