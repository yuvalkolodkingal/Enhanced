package com.kolodkingal.enhanced.ability;

import net.minecraft.world.entity.player.Player;

/**
 * Which key press opens the Bifrost screen (3.4). Since 3.5 the keys of the Bifrost live in
 * {@link com.kolodkingal.enhanced.bifrost.BifrostKeys}; this one-line delegate keeps the 3.4 name for its callers.
 */
public final class BifrostKeys {

    /**
     * @param column 1-5, the key (R G V B N)
     * @param sneak  whether Sneak is held
     */
    public static boolean opensScreen(Player player, int column, boolean sneak) {
        return com.kolodkingal.enhanced.bifrost.BifrostKeys.opensScreen(player, column, sneak);
    }

    private BifrostKeys() {
    }
}
