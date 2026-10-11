package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Jean Grey, the Phoenix, as a {@link HeroServer} plug-in. A stub: the five abilities, the body and
 * the rest are built by the Phoenix lane (DESIGN section 13); until then the power exists, shows its five lines on the
 * HUD and does nothing.
 */
public final class PhoenixServer implements HeroServer {
    @Override
    public Power power() {
        return Power.PHOENIX;
    }
}
