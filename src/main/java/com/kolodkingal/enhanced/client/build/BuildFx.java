package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.network.BuildFxPayload;

/**
 * The Build Magic spells this client sees at work (D-B 7.2): the cast gestures, the frame and the ripple that sweeps each
 * spell's box, Mom's scarlet thread, the undo running backwards, Billy's rank-up rings; pruned by time and cleared on
 * logout.
 *
 * <p>A stub of the skeleton (3.1, step S): it draws nothing yet. Lane BC fills it. Only ever loaded on a client, from the
 * handler of {@link BuildFxPayload}.
 */
public final class BuildFx {
    /** A moment of a spell near this client. Lane BC: start, advance or end what is drawn of it. */
    public static void accept(BuildFxPayload payload) {
    }

    /** Forgets every spell seen (at logout). Lane BC: called beside {@link BuildClient#clear}. */
    public static void clear() {
    }

    private BuildFx() {
    }
}
