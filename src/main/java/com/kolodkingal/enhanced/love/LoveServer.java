package com.kolodkingal.enhanced.love;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Love, Thor's daughter (her axe, lightning, eye laser and Bifrost; her body) (3.8), as a {@link
 * HeroServer} plug-in. The 3.8 skeleton registers it empty, so that the power exists, can be given and saved and does
 * nothing yet; lane L8 fills it.
 */
public final class LoveServer implements HeroServer {
    @Override
    public Power power() {
        return Power.LOVE;
    }
}
