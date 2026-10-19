package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of the Hulk (3.6), as a {@link HeroClient} plug-in: registered empty by the 3.6 skeleton (the
 * defaults draw and act nothing of his own); lane L7 fills it (the rage and heart-rate HUD, gestures, stances, his
 * model, first person, effects).
 */
public final class HulkClient implements HeroClient {
    @Override
    public Power power() {
        return Power.HULK;
    }
}
