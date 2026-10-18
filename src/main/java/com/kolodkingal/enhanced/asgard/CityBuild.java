package com.kolodkingal.enhanced.asgard;

import net.minecraft.server.level.ServerLevel;

/**
 * What the city's own code changes in the built city (3.5). The skeleton writes the calls the other lanes make, so they
 * compile before lane L1 (who owns this class and fills it with the placement of the city in each chunk) lands.
 */
public final class CityBuild {

    /**
     * Turns the murals of the throne room to Hela's conquests ({@code conquest} true, when she arrives) or back to the
     * peaceful paintings (called by lane L4's fight). The skeleton records the state in {@link AsgardState}; lane L1
     * swaps the mural blocks.
     */
    public static void setMurals(ServerLevel level, boolean conquest) {
        AsgardState.get(level.getServer()).setMuralsConquest(conquest);
    }

    private CityBuild() {
    }
}
