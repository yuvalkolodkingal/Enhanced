package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Odin, the Allfather (3.5), as a {@link HeroClient} plug-in: registered empty by the 3.5 skeleton
 * (the defaults draw and act nothing of his own); lane L3 fills it (HUD, gestures, stances, first person, effects).
 */
public final class OdinClient implements HeroClient {
    @Override
    public Power power() {
        return Power.ODIN;
    }
}
