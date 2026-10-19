package com.kolodkingal.enhanced.finders;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/**
 * The lab's Cerebro (3.7, lane L5): Professor X's N stays as it is everywhere, and at the Cerebro console of the X-Men
 * lower lab it is amplified (every dimension, 30 s, the stone sites). Two seams: {@code XavierAbilities.cerebro} asks
 * {@link #amplify} first, and the console block (lane L4) hands a right-click to {@link #useConsole}. Stubs of the 3.7
 * skeleton until lane L5 fills them: nothing is amplified, the console does nothing.
 */
public final class CerebroRoom {

    /**
     * Whether {@code player}, pressing Cerebro's key (his cooldown ready), is at a console and the room has amplified
     * it (then the plain Cerebro does not run: the room started its own search and cooldown).
     */
    public static boolean amplify(ServerPlayer player) {
        return false;
    }

    /** A right-click on the Cerebro console at {@code pos} (lane L4's block calls it). */
    public static InteractionResult useConsole(ServerPlayer player, BlockPos pos) {
        return InteractionResult.PASS;
    }

    private CerebroRoom() {
    }
}
