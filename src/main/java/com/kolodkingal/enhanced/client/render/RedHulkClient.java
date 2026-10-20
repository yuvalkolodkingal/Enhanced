package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Red Hulk (his HUD and anger bar, his gestures, his heat and steam) (3.8), as a {@link HeroClient}
 * plug-in: registered empty by the 3.8 skeleton (the defaults draw and act nothing of his own); lane L2 fills it.
 */
public final class RedHulkClient implements HeroClient {
    @Override
    public Power power() {
        return Power.RED_HULK;
    }
}
