package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Captain America (his HUD, his gestures and stances, the shield on his back and in
 * first person) (3.8), as a {@link HeroClient} plug-in: registered empty by the 3.8 skeleton (the
 * defaults draw and act nothing of his own); lane L1 fills it.
 */
public final class CaptainClient implements HeroClient {
    @Override
    public Power power() {
        return Power.CAPTAIN_AMERICA;
    }
}
