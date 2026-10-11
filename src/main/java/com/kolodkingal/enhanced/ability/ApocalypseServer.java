package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Apocalypse, as a {@link HeroServer} plug-in. A stub: the five abilities, the body and the rest are
 * built by the Apocalypse lane (DESIGN section 17); until then the power exists, shows its five lines on the HUD and
 * does nothing.
 */
public final class ApocalypseServer implements HeroServer {
    @Override
    public Power power() {
        return Power.APOCALYPSE;
    }
}
