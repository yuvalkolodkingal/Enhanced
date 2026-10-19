package com.kolodkingal.enhanced.strange;

import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of the Sling Ring's screen (3.6): {@code SlingRingActionPayload} calls it. Written empty by the 3.6
 * skeleton; lane L3 fills it (the places of {@code PlaceBook.Kind.SLING_RING}, the portal pair to one, the refusals).
 */
public final class SlingRingActions {

    /**
     * A button: {@code action} is one of {@code SlingRingActionPayload}'s; {@code index} and {@code name} name a place.
     */
    public static void handle(ServerPlayer player, int action, int index, String name) {
    }

    private SlingRingActions() {
    }
}
