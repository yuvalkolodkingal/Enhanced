package com.kolodkingal.enhanced.vormir;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * One Stonekeeper per cliff (3.6, the plan's 4.4 and 6.3): every {@link #PERIOD} ticks, at each cliff with a player on
 * its summit, a keeper is put at {@code STONEKEEPER_HOME} if there is none (the template's was killed with {@code
 * /kill}, or the template came without one).
 */
public final class VormirKeeper {
    /** How often (ticks) the summits are looked at. */
    public static final int PERIOD = 40;

    /** The level's look at its summits (real servers: the GameTests call {@link #ensure} themselves). */
    static void tick(ServerLevel level) {
        if (level.getGameTime() % PERIOD != 0) {
            return;
        }
        Set<VormirSite> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            VormirSite site = VormirTracker.siteOf(player);
            if (site != null && site.dimension() == level.dimension() && site.onSummit(player.blockPosition())
                    && seen.add(site)) {
                ensure(level, site);
            }
        }
    }

    /**
     * The cliff's keeper: the one there (a second one, which should never be, goes), or a new one at his place; null
     * while the creatures of his place are not loaded yet (he may be there).
     */
    @Nullable
    public static StonekeeperEntity ensure(ServerLevel level, VormirSite site) {
        if (!level.areEntitiesLoaded(ChunkPos.asLong(site.keeperHome()))) {
            return null;
        }
        StonekeeperEntity found = null;
        for (StonekeeperEntity keeper : level.getEntitiesOfClass(StonekeeperEntity.class, AABB.of(site.bounds()))) {
            if (!keeper.isAlive()) {
                continue;
            }
            if (found == null) {
                found = keeper;
            } else {
                keeper.discard();
            }
        }
        return found != null ? found : StonekeeperEntity.spawn(level, site.keeperHome());
    }

    private VormirKeeper() {
    }
}
