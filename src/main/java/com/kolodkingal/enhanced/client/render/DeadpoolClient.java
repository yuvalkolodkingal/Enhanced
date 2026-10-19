package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Deadpool (3.7), as a {@link HeroClient} plug-in: registered empty by the 3.7 skeleton (the
 * defaults draw and act nothing of his own); lane L3 fills it (the swords and pistols on his back and hips, the ammo
 * HUD, the speech bubbles of his lines, Dogpool's renderer, his gestures and stances).
 */
public final class DeadpoolClient implements HeroClient {
    @Override
    public Power power() {
        return Power.DEADPOOL;
    }
}
