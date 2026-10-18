package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Heimdall, the Guardian of the Bifrost (3.5), as a {@link HeroServer} plug-in. The 3.5 skeleton
 * registers it empty, so that the power exists, can be given and saved and does nothing yet; lane L2 fills it (his
 * abilities, his Hofund, the Guardian's Watch and his sight).
 */
public final class HeimdallServer implements HeroServer {
    @Override
    public Power power() {
        return Power.HEIMDALL;
    }
}
