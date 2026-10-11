package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Jean Grey, the Phoenix, as a {@link HeroClient} plug-in. A stub: the HUD lines, the eyes, the
 * stances and gestures, the first-person hands and the effects are built by the Phoenix lane (DESIGN section 13); until
 * then the power exists and shows its five lines on the HUD (the name of each ability and its cooldown).
 */
public final class PhoenixClient implements HeroClient {
    @Override
    public Power power() {
        return Power.PHOENIX;
    }
}
