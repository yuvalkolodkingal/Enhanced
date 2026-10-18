package com.kolodkingal.enhanced.asgard.npc;

import net.minecraft.server.level.ServerLevel;

/**
 * The population keeper of Asgard (3.5): every slot of {@code CityPlan.homes()} has exactly one Asgardian. Written
 * empty by the 3.5 skeleton (the test hook the plan names); lane L5 owns and fills it.
 */
public final class AsgardPopulation {

    /** Runs the keeper once now, as it runs every 100 ticks (for the tests). */
    public static void tickNow(ServerLevel level) {
    }

    private AsgardPopulation() {
    }
}
