package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Thanos, as a {@link HeroClient} plug-in. A stub: the HUD lines, the eyes, the stances and
 * gestures, the first-person hands and the effects are built by the Thanos lane (DESIGN section 15); until then the
 * power exists and shows its five lines on the HUD (the name of each ability and its cooldown).
 */
public final class ThanosClient implements HeroClient {
    @Override
    public Power power() {
        return Power.THANOS;
    }
}
