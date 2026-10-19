package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;

/**
 * The server side of the Hulk (3.6), as a {@link com.kolodkingal.enhanced.ability.HeroServer} plug-in. The 3.6 skeleton
 * registers it empty, so that the power exists, can be given and saved and does nothing yet; lane L7 fills it (his five
 * keys, Bruce Banner and the two Hulks, his body and his rage, the blocks he breaks).
 */
public final class HulkServer implements HeroServer {
    @Override
    public Power power() {
        return Power.HULK;
    }
}
