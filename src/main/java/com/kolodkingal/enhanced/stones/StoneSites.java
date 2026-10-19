package com.kolodkingal.enhanced.stones;

import net.minecraft.server.level.ServerLevel;

/**
 * Where the site stones are put at run time (3.6). Written empty by the 3.6 skeleton; lane L1 fills it.
 */
public final class StoneSites {

    /**
     * Asgard's vault is open (Hela's first defeat, {@code odin/Vault.open}, and at server start for worlds whose vault
     * opened in 3.5): the stone pedestal with the Reality Stone is put at {@code CityPlan.REALITY_PEDESTAL} if it is
     * not there.
     */
    public static void placeAsgardPedestal(ServerLevel asgard) {
    }

    private StoneSites() {
    }
}
