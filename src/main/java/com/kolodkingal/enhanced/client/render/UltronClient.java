package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;

/**
 * The client side of Ultron, as a {@link HeroClient} plug-in. A stub: the HUD lines, the eyes, the stances and
 * gestures, the first-person hands and the effects are built by the Ultron lane (DESIGN section 16); until then the
 * power exists and shows its five lines on the HUD (the name of each ability and its cooldown).
 */
public final class UltronClient implements HeroClient {
    @Override
    public Power power() {
        return Power.ULTRON;
    }
}
