package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.custom.StoneRow;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * A custom hero's stone row on a key (3.6): {@code CustomHeroServer} sends a binding of a {@link StoneRow} here, never
 * to a hero's code. The row works only while he carries the stone and may use it ({@link StoneUse#carried}); the key
 * is the stone's use ({@link StoneUse#use} with {@link StoneUse.Source#ROW}): its main action on the key, its Sneak use
 * on the Sneak binding. A held main action (the Mind Beam, the Purple Beam, the Soul Drain) lasts while the key is
 * down; the Reality Stone's main action is a switch (its bubbles), the Time Stone's too (Slow Time). The cooldown is the
 * stone's ({@link StoneCooldowns}), stretched by the row's strength, and shown on the key.
 */
public final class StoneRowKeys {

    /**
     * A binding of a stone row went down or up.
     *
     * @param column the key, 1-5
     * @param alt    whether the binding is the stone's Sneak use
     */
    public static void press(ServerPlayer player, PowerData data, StoneRow row, int column, boolean down, boolean alt) {
        if (!data.kit().has(row)) {
            return;
        }
        if (!down) {
            // Letting go always gets through: it ends a held beam or drain of this stone.
            StoneUse.use(player, row.kind, StoneUse.Source.ROW, alt, false, column);
            return;
        }
        StoneUse.use(player, row.kind, StoneUse.Source.ROW, alt, true, column);
    }

    private StoneRowKeys() {
    }
}
