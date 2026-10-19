package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Wolverine (3.7), as a {@link HeroServer} plug-in. The 3.7 skeleton registers it empty, so that the
 * power exists, can be given and saved and does nothing yet; lane L2 fills it (his five keys and their Sneak variants,
 * his adamantium body, the claws, the Rage, the senses, the climb; his healing factor is lane L1's {@code healing/}).
 */
public final class WolverineServer implements HeroServer {
    @Override
    public Power power() {
        return Power.WOLVERINE;
    }
}
