package com.kolodkingal.enhanced.road;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The end of the Road: the player has won every trial. The ending proper (the choice of Billy or Tommy inside the Road, the
 * Gene used up, and the way back to where he entered) is the last step of 3.2; until it is built this is the stand-in: he is
 * told he has walked the whole Road and is taken back to where he entered, and the next Witches' Road door he opens with the
 * Gene offers the choice ({@link RoadTrials#doorMode}).
 */
public final class RoadEnding {

    /** The player has won the last trial and stepped through its gate. */
    public static void finish(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.enhanced.road.walked_all").withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), false);
        RoadTrials.leave(player, RoadChallenge.End.WON);
        player.displayClientMessage(Component.translatable("message.enhanced.road.walked_all.hint").withStyle(ChatFormatting.GRAY), true);
    }

    private RoadEnding() {
    }
}
