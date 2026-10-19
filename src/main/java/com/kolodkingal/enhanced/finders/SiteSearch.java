package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * The search for the Infinity Stones' sites (3.7, lane L5): incremental, budgeted ({@code [stone_locator]
 * startChecksPerTick} structure starts loaded per tick, server-wide), cached, skipping spent sites ({@link
 * SpentSites}). The Stone Locator and the amplified Cerebro ask it. A stub of the 3.7 skeleton until lane L5 fills it:
 * it finds nothing.
 */
public final class SiteSearch {

    /**
     * The nearest site of {@code kind} found for {@code player} so far (the search goes on in the background), if any.
     */
    public static Optional<GlobalPos> nearest(ServerPlayer player, StoneKind kind) {
        return Optional.empty();
    }

    private SiteSearch() {
    }
}
