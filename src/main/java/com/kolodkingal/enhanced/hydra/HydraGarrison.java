package com.kolodkingal.enhanced.hydra;

import net.minecraft.server.level.ServerPlayer;

/**
 * The garrisons of the Hydra outposts (3.6, the plan's 15.4): every 40 ticks per player, an outpost near him has its
 * posts filled with soldiers (old outposts of 3.3 too: nothing is stored in the template). The 3.6 skeleton writes this
 * stub, which spawns nothing; lane L8 owns the class and writes it (with {@code HydraOutposts}, the outposts'
 * SavedData).
 */
public final class HydraGarrison {

    /** One player's tick (the shared player tick calls it): the outposts near him are filled when due. Lane L8. */
    public static void tick(ServerPlayer player) {
    }

    private HydraGarrison() {
    }
}
