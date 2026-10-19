package com.kolodkingal.enhanced.strange;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Doctor Strange's Mirror Dimension (3.6, simplified): a sphere round him in which nothing of the real world breaks.
 * The 3.6 skeleton writes the question {@code BlockRights} asks; lane L3 owns the class (the spell, the sphere, its
 * rules and its look).
 */
public final class MirrorDimension {

    /**
     * Whether a Mirror Dimension keeps the block at {@code pos} as it is against {@code player} (anyone but the Strange
     * whose sphere it is). False while there is none.
     */
    public static boolean protects(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return false;
    }

    private MirrorDimension() {
    }
}
