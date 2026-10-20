package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Love (her HUD, her gestures, her glowing eyes) (3.8), as a {@link HeroClient} plug-in: registered
 * empty by the 3.8 skeleton (the defaults draw and act nothing of his own); lane L8 fills it.
 */
public final class LoveClient implements HeroClient {
    @Override
    public Power power() {
        return Power.LOVE;
    }
}
