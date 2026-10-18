package com.kolodkingal.enhanced.asgard.npc;

import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of a talk with an Asgardian (3.5): the answer a player chose on the talk screen ({@code
 * AsgardTalkPayload.Choose}). Written empty by the 3.5 skeleton so the payload has a handler; lane L5 owns and fills it
 * (the server checks the distance and that the option was offered, then runs its action).
 */
public final class AsgardTalk {

    /** The player chose {@code option} in his talk with the Asgardian {@code entityId}. */
    public static void choose(ServerPlayer player, int entityId, String option) {
    }

    private AsgardTalk() {
    }
}
