package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;

/** Routes an ability key press/release from a client to the right hero. */
public final class AbilityHandler {

    /**
     * @param ability 1-5
     * @param down    true when the key was pressed, false when released (hold-to-use abilities need both)
     */
    public static void onKey(ServerPlayer player, int ability, boolean down) {
        if (ability < 1 || ability > PowerData.ABILITIES || !player.isAlive()) {
            return;
        }
        PowerData data = Powers.get(player);
        switch (data.power()) {
            case MAGNETO -> MagnetoAbilities.onKey(player, data, ability, down);
            case SCARLET_WITCH -> WandaAbilities.onKey(player, data, ability, down);
            case PHOTON -> PhotonAbilities.onKey(player, data, ability, down);
            case NONE -> {
            }
        }
        Powers.syncIfDirty(player);
    }

    private AbilityHandler() {
    }
}
