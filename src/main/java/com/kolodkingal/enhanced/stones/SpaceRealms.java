package com.kolodkingal.enhanced.stones;

import net.minecraft.server.level.ServerPlayer;

/**
 * The Space Stone's Realm Picker (3.6): the Overworld, the Nether, the End and Asgard. Written empty by the 3.6
 * skeleton ({@code StoneActionPayload} calls it); lane L2 fills it, every teleport inside {@link SpaceStone#moving}.
 */
public final class SpaceRealms {

    /** The player chose the realm of index {@code index} on the screen ({@code RealmScreenPayload}'s list). */
    public static void choose(ServerPlayer player, int index) {
    }

    private SpaceRealms() {
    }
}
