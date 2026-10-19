package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Doctor Strange (3.6), as a {@link HeroClient} plug-in: registered empty by the 3.6 skeleton (the
 * defaults draw and act nothing of his own); lane L3 fills it (HUD, gestures, stances, the Cloak, first person, the
 * spells' effects).
 */
public final class StrangeClient implements HeroClient {
    @Override
    public Power power() {
        return Power.DOCTOR_STRANGE;
    }
}
