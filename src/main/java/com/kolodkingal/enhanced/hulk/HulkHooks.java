package com.kolodkingal.enhanced.hulk;

import net.minecraft.server.level.ServerPlayer;

/**
 * Where other lanes' code calls the Hulk's (3.6). Written empty by the 3.6 skeleton; lane L7 fills the bodies, keeping
 * the signatures.
 */
public final class HulkHooks {

    /**
     * The Hulk has just snapped with a full Infinity Gauntlet (lane L1 calls it after the Snap): the cost is his own,
     * his right arm burned ({@code HulkState.armBurnedUntil}, Weakness II), instead of the 2-health cost of a Bearer.
     */
    public static void onSnap(ServerPlayer hulk) {
    }

    private HulkHooks() {
    }
}
