package com.kolodkingal.enhanced.hela;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * The lane of Hela's fight (3.5). The 3.5 skeleton writes the hook of Hela's Seal (lane L1's block {@code hela_seal}
 * calls {@link #onSeal}); lane L4 owns this class and makes it the {@code BossLane} of {@code BossKind.HELA} (refusal,
 * create, the test site). Until then the seal does nothing.
 */
public final class HelaLane {

    /** A player clicks Hela's Seal at {@code pos} (the first click asks, a second within 5 s breaks it). */
    public static boolean onSeal(ServerPlayer player, BlockPos pos) {
        return false;
    }

    private HelaLane() {
    }
}
