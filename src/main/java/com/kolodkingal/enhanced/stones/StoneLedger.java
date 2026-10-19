package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * How many stones there are (3.6, the plan's 1.4): the four site stones (Reality, Time, Power, Soul) are counted by
 * {@code [stones] uniqueness} (each player once, one in the world, or unlimited); the Mind and Space Stones are as rare
 * as their vessels and are not. The 3.6 skeleton writes this shell (the questions every lane asks); lane L1 writes the
 * ledger behind it (the per-player bits of {@link StoneProgress}, the world's SavedData {@code stone_ledger}, lost
 * stones returning).
 */
public final class StoneLedger {
    /** The stones a site gives and the ledger counts. */
    public static final Set<StoneKind> SITE_STONES = Set.of(StoneKind.REALITY, StoneKind.TIME, StoneKind.POWER,
            StoneKind.SOUL);

    /** Whether the ledger counts this stone (a site stone). */
    public static boolean isSiteStone(StoneKind kind) {
        return SITE_STONES.contains(kind);
    }

    /** Whether the player may take this site stone now (from a pedestal, the Orb's chest, the sacrifice). Lane L1. */
    public static boolean mayTake(ServerPlayer player, StoneKind kind) {
        return true;
    }

    /** The player has taken this site stone (the ledger marks it). Lane L1. */
    public static void taken(ServerPlayer player, StoneKind kind) {
    }

    /**
     * A stack is destroyed for good (a sacrificed player's items, a lost stone): every site stone in it (or in a
     * Gauntlet in it) returns to its site. Lane L1; the sacrifice (lane L5) calls it.
     */
    public static void destroyed(ItemStack stack) {
    }

    private StoneLedger() {
    }
}
