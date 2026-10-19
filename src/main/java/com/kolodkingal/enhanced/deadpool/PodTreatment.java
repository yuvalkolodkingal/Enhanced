package com.kolodkingal.enhanced.deadpool;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/**
 * The Weapon X Pod's treatment (3.7, lane L3): a powerless player inside the pod right-clicks it, the hatch closes, 100
 * ticks of "treatment", and out comes Deadpool; leaving cancels; a hero is refused. The pod block (lane L4) hands the
 * right-click here. A stub of the 3.7 skeleton until lane L3 fills it: the pod refuses everyone.
 */
public final class PodTreatment {

    /** A right-click on the pod at {@code pos} by {@code player}. */
    public static InteractionResult use(ServerPlayer player, BlockPos pos) {
        player.displayClientMessage(Component.translatable("message.enhanced.deadpool.pod_idle"), true);
        return InteractionResult.CONSUME;
    }

    private PodTreatment() {
    }
}
