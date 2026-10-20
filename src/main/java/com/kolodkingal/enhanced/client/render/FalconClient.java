package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Falcon (his HUD, his gestures and flight, the wings in first person) (3.8), as a {@link
 * HeroClient} plug-in: registered empty by the 3.8 skeleton (the defaults draw and act nothing of his own); lane L5
 * fills it.
 */
public final class FalconClient implements HeroClient {
    @Override
    public Power power() {
        return Power.FALCON;
    }
}
