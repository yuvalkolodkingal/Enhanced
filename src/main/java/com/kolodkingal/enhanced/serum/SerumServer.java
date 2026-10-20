package com.kolodkingal.enhanced.serum;

import com.kolodkingal.enhanced.falcon.FalconWings;
import com.kolodkingal.enhanced.network.SerumScreenPayload;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Super Soldier Serum's server side (3.8, PLAN 2.1): {@link #open} sends the choice screen, {@link #choose} takes
 * the answer. The 3.8 skeleton opens the screen (so the payload and its screen are wired end to end) and answers every
 * choice "not yet": nothing is used up and nobody changes. Lane L1 writes the pending choices, the server's checks
 * (never trusting the client) and {@code Powers.awaken}, and keeps the signatures.
 */
public final class SerumServer {
    /** The player used a serum in {@code hand}: the screen opens (Falcon locked unless he carries the Exo-Wings). */
    public static void open(ServerPlayer player, InteractionHand hand) {
        if (player.isSpectator()) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new SerumScreenPayload(hand, FalconWings.carries(player)));
    }

    /** The screen's answer ({@code SerumChoicePayload}). Skeleton: "not yet". */
    public static void choose(ServerPlayer player, Power choice) {
        player.displayClientMessage(Component.translatable("message.enhanced.serum.not_yet"), true);
    }

    private SerumServer() {
    }
}
