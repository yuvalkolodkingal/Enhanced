package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Hela, the Goddess of Death (3.5), the hero, as a {@link HeroClient} plug-in: registered empty by
 * the 3.5 skeleton (the defaults draw and act nothing of his own); lane L4 fills it (HUD, gestures, stances, first
 * person, effects).
 */
public final class HelaClient implements HeroClient {
    @Override
    public Power power() {
        return Power.HELA;
    }
}
