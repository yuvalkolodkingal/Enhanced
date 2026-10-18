package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Odin, the Allfather (3.5), as a {@link HeroServer} plug-in. The 3.5 skeleton registers it empty,
 * so that the power exists, can be given and saved and does nothing yet; lane L3 fills it (his abilities, Gungnir, the
 * ravens and the coronation).
 */
public final class OdinServer implements HeroServer {
    @Override
    public Power power() {
        return Power.ODIN;
    }
}
