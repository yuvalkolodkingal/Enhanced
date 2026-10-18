package com.kolodkingal.enhanced.odin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * The coronation on the Throne of Asgard (3.5): a powerless player who sits on the throne holding his own Gungnir
 * becomes Odin. The throne is lane L1's block ({@code odin_throne}); it calls {@link #onSit} when a player sits down.
 * Written empty by the 3.5 skeleton; lane L3 owns and fills it.
 */
public final class Coronation {

    /** A player has sat down on the throne at {@code pos}. */
    public static void onSit(ServerPlayer player, BlockPos pos) {
    }

    private Coronation() {
    }
}
