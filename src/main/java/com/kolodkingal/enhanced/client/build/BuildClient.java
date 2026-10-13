package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;

/**
 * The builder's own Build Magic on the client (D-B 11.10): build mode, the chosen spell and style, the selection, the
 * ghost, the numbers, the hold timer, the server's last verdict and its marks, and the pocket's copy for the ghost. It
 * sends recipes and actions; the server decides.
 *
 * <p>A stub of the skeleton (3.1, step S): it keeps nothing yet. Lane BC fills it. Only ever loaded on a client, from the
 * handlers of the two payloads it receives.
 */
public final class BuildClient {
    /** The server's verdict on a cast, a preflight, an undo or a redo. Lane BC: keep it, show its marks and counts. */
    public static void onVerdict(BuildVerdictPayload payload) {
    }

    /** One part of the copy in the pocket. Lane BC: gather the parts ({@link BuildPocketPayload#join}), mesh the ghost. */
    public static void onPocketPart(BuildPocketPayload payload) {
    }

    private BuildClient() {
    }
}
