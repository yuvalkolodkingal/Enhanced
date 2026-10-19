package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Doctor Strange, the Sorcerer Supreme (3.6), as a {@link
 * com.kolodkingal.enhanced.ability.HeroServer} plug-in. The 3.6 skeleton registers it empty, so that the power exists,
 * can be given and saved and does nothing yet; lane L3 fills it (his spells on R, V, B and N, the Eye on Y as key 6,
 * his body, the Sling Ring's portals). His flight on G is the shared flight of every flier ({@code Power.canFly}).
 */
public final class StrangeServer implements HeroServer {
    @Override
    public Power power() {
        return Power.DOCTOR_STRANGE;
    }
}
