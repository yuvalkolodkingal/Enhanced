package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Captain America (his five keys and their Sneak variants, his super-soldier body, the Vibranium
 * Shield's throw and rules) (3.8), as a {@link HeroServer} plug-in. The 3.8 skeleton registers it empty, so that the
 * power exists, can be given and saved and does nothing yet; lane L1 fills it.
 */
public final class CaptainServer implements HeroServer {
    @Override
    public Power power() {
        return Power.CAPTAIN_AMERICA;
    }
}
