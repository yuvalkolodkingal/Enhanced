package com.kolodkingal.enhanced.tower;

import net.minecraft.server.level.ServerPlayer;

/**
 * The run-time refit of the Avengers Towers generated before 3.8 (PLAN 9.6, the {@code HydraGarrison} pattern): the
 * armoury's points replaced only where the 3.6.1 template's own blocks still stand, once per tower (SavedData {@code
 * tower_refits}). The 3.8 skeleton's refit refits nothing; lane L6 fills it (and calls {@link #tick} from its own
 * player tick) and keeps the signature.
 */
public final class TowerRefit {
    /** The refit's look round a player (every {@code refitIntervalTicks}). Skeleton: nothing. */
    public static void tick(ServerPlayer player) {
    }

    private TowerRefit() {
    }
}
