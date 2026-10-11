package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Ultron, as a {@link HeroServer} plug-in. A stub: the five abilities, the body and the rest are
 * built by the Ultron lane (DESIGN section 16); until then the power exists, shows its five lines on the HUD and does
 * nothing.
 */
public final class UltronServer implements HeroServer {
    @Override
    public Power power() {
        return Power.ULTRON;
    }
}
