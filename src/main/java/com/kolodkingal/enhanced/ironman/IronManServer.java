package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Iron Man (the suits, their energy, flight and moves, the suit-up; his keys work only in a suit)
 * (3.8), as a {@link HeroServer} plug-in. The 3.8 skeleton registers it empty, so that the power exists, can be given
 * and saved and does nothing yet; lane L3 (L4 the nanotech suits and the Hulkbuster) fills it.
 */
public final class IronManServer implements HeroServer {
    @Override
    public Power power() {
        return Power.IRON_MAN;
    }
}
