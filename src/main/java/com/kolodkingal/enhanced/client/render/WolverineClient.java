package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Wolverine (3.7), as a {@link HeroClient} plug-in: registered empty by the 3.7 skeleton (the
 * defaults draw and act nothing of his own); lane L2 fills it (his claws (the layer and first person), the healing
 * icon's place in his HUD, the Rage's red edge, the scent trails and glows, his gestures and stances).
 */
public final class WolverineClient implements HeroClient {
    @Override
    public Power power() {
        return Power.WOLVERINE;
    }
}
