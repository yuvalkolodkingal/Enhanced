package com.kolodkingal.enhanced.stones;

import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of the Stone Wheel key (3.6, default J): the stone a Gauntlet in the player's hand uses. Written
 * empty by the 3.6 skeleton ({@code StoneActionPayload} calls it); lane L1 fills it.
 */
public final class StoneWheel {

    /**
     * The player picked the stone of ordinal {@code index} in the Gauntlet in his hand (-1: the next one, a tap of J).
     */
    public static void select(ServerPlayer player, int index) {
    }

    /** The player holds J: his client opens the wheel (the server may check or refuse). */
    public static void open(ServerPlayer player) {
    }

    private StoneWheel() {
    }
}
