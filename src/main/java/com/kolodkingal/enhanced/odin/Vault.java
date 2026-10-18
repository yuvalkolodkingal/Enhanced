package com.kolodkingal.enhanced.odin;

import net.minecraft.server.level.ServerLevel;

/**
 * Odin's vault under the throne room (3.5): its door ({@code CityPlan.VAULT_DOOR}) opens when Odin wakes. Written empty
 * by the 3.5 skeleton (lane L4's victory calls it); lane L3 owns and fills it.
 */
public final class Vault {

    /** Opens the vault's door in Asgard's level. */
    public static void open(ServerLevel level) {
    }

    private Vault() {
    }
}
