package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Apocalypse, as a {@link HeroServer} plug-in. The boss is built (DESIGN section 17); the five
 * abilities, the body and the rest of the hero are built by the next stage of the Apocalypse lane: until then the power
 * exists, shows its five lines on the HUD and does nothing.
 */
public final class ApocalypseServer implements HeroServer {
    @Override
    public Power power() {
        return Power.APOCALYPSE;
    }

    /** The sand that was rolling when the server stopped is forgotten. */
    @Override
    public void onServerStopped() {
        SandHazards.clear();
    }
}
