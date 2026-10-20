package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Iron Man (his HUD and helmet frame, his gestures and flight, the suit-up in first
 * person) (3.8), as a {@link HeroClient} plug-in: registered empty by the 3.8 skeleton (the
 * defaults draw and act nothing of his own); lane L3 (L4, L7) fills it.
 */
public final class IronManClient implements HeroClient {
    @Override
    public Power power() {
        return Power.IRON_MAN;
    }
}
