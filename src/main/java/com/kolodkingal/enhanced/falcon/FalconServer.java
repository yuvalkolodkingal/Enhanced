package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Falcon (his keys and their Sneak variants, the Exo-Wings' flight and glide, Redwing) (3.8), as a
 * {@link HeroServer} plug-in. The 3.8 skeleton registers it empty, so that the power exists, can be given and saved and
 * does nothing yet; lane L5 fills it.
 */
public final class FalconServer implements HeroServer {
    @Override
    public Power power() {
        return Power.FALCON;
    }
}
