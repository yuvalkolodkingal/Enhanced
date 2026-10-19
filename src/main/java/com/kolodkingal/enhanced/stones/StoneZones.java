package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;

/**
 * The Time Stone's Slow Time zones (3.6): {@code SlowMotion} reads them beside Quicksilver's and Tommy's. A player
 * whose Time Stone (or Eye of Agamotto, or Doctor Strange's Y) slows time has {@link Live#TIME_SLOWING} (a synced bit,
 * so the clients agree on what is slowed); the zone is centred on him and moves with him, and he is not slowed. The 3.6
 * skeleton writes the seam; lane L2 owns the class (who sets the bit, the factor per zone, the bosses' half).
 */
public final class StoneZones {

    /** Whether this player's Time Stone slows time round him. */
    public static boolean slows(PowerData data) {
        return data.is(Live.TIME_SLOWING);
    }

    /** The radius of a Time Stone's zone ({@code [time_stone] slowRadius}). */
    public static double radius() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.TIME_SLOW_RADIUS.get() : 12;
    }

    private StoneZones() {
    }
}
