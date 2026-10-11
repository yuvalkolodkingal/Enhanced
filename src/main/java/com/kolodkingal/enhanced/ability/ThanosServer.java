package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.thanos.ThanosSummon;

/**
 * The server side of Thanos, as a {@link HeroServer} plug-in. The five abilities and the body are built by the Thanos
 * lane's hero stage (DESIGN section 15.4); until then the power exists, shows its five lines on the HUD and does
 * nothing. What the plug-in already does is the boss's housekeeping: when the server stops, the summoning forgets who
 * was charging.
 */
public final class ThanosServer implements HeroServer {
    @Override
    public Power power() {
        return Power.THANOS;
    }

    @Override
    public void onServerStopped() {
        ThanosSummon.clear();
    }
}
