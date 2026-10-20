package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of Red Hulk (his five keys and their Sneak variants, his body and scale, Anger and its burst,
 * Ross's form) (3.8), as a {@link HeroServer} plug-in. The 3.8 skeleton registers it empty, so that the
 * power exists, can be given and saved and does nothing yet; lane L2 fills it.
 */
public final class RedHulkServer implements HeroServer {
    @Override
    public Power power() {
        return Power.RED_HULK;
    }
}
