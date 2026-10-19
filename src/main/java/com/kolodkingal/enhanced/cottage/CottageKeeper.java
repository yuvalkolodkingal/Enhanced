package com.kolodkingal.enhanced.cottage;

import net.minecraft.server.level.ServerPlayer;

/**
 * The keeper of Agatha's Cottages (3.7, lane L6; the plan's 9.4): every 100 ticks per player, the cottages near him are
 * found by their structure starts and recorded (SavedData {@code cottages}), each resident kept once per cottage
 * ({@code AsgardPopulation.keep} with the cottage's homes), the cellar's Road door registered, Señor Scratchy kept at
 * his hutch; the NPC Agatha is away while a player is Agatha. A stub of the 3.7 skeleton until lane L6 fills it: it
 * keeps nobody.
 */
public final class CottageKeeper {

    /** Every player tick (lane L6 calls it from its own events): the cottages near {@code player}. */
    public static void tick(ServerPlayer player) {
    }

    private CottageKeeper() {
    }
}
