package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.custom.StoneRow;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * A custom hero's stone row on a key (3.6): {@code CustomHeroServer} sends a binding of a {@link StoneRow} here, never
 * to a hero's code. Written empty by the 3.6 skeleton; lane L2 fills it: the row works only while he carries the stone
 * and may use it ({@link StoneUse#carried}), through {@link StoneUse#use} with {@link StoneUse.Source#ROW}.
 */
public final class StoneRowKeys {

    /**
     * A binding of a stone row went down or up.
     *
     * @param column the key, 1-5
     * @param alt    whether the binding is the stone's Sneak use
     */
    public static void press(ServerPlayer player, PowerData data, StoneRow row, int column, boolean down, boolean alt) {
    }

    private StoneRowKeys() {
    }
}
