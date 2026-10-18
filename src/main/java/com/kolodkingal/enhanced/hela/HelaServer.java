package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Hela, the Goddess of Death (3.5), the hero, as a {@link HeroServer} plug-in. The 3.5 skeleton
 * registers it empty, so that the power exists, can be given and saved and does nothing yet; lane L4 fills it (her
 * abilities, the necroswords and her army).
 */
public final class HelaServer implements HeroServer {
    @Override
    public Power power() {
        return Power.HELA;
    }
}
