package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.eternity.EternityRealm;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Helpers for the tests of Eternity's realm (3.8, written by the skeleton for lane L8): the realm's level in the GameTest
 * server, which (built from the flat preset) has only the vanilla dimensions. The first test that needs it makes it from
 * the data pack's own dimension file, as {@link AsgardTestSupport#ensureAsgard} does for Asgard; it stays until the
 * server stops.
 */
final class EternityTestSupport {
    static ServerLevel ensureRealm(MinecraftServer server) {
        ServerLevel existing = server.getLevel(EternityRealm.LEVEL);
        if (existing != null) {
            return existing;
        }
        return AsgardTestSupport.makeLevel(server, EternityRealm.LEVEL, "/data/enhanced/dimension/eternity.json");
    }

    private EternityTestSupport() {
    }
}
