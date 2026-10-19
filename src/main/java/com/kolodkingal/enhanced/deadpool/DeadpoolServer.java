package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Deadpool (3.7), as a {@link HeroServer} plug-in. The 3.7 skeleton registers it empty, so that the
 * power exists, can be given and saved and does nothing yet; lane L3 fills it (his katanas and pistols, the Teleport
 * Belt, the Taunt and his lines, Dogpool, the smoke; his healing factor is lane L1's {@code healing/}).
 */
public final class DeadpoolServer implements HeroServer {
    @Override
    public Power power() {
        return Power.DEADPOOL;
    }
}
