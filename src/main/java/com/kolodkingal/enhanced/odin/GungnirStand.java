package com.kolodkingal.enhanced.odin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Gungnir's stand in the vault (3.5): the block is lane L1's ({@code gungnir_stand}), what it does lane L3's. Written
 * by the 3.5 skeleton as the hook the block calls; until lane L3 fills it, the stand gives nothing.
 */
public final class GungnirStand {

    /** A player right-clicks the stand at {@code pos} (both sides; the server decides). */
    public static InteractionResult use(Player player, BlockPos pos) {
        return InteractionResult.PASS;
    }

    private GungnirStand() {
    }
}
