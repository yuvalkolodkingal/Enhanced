package com.kolodkingal.enhanced.vormir;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Who pushed whom, for the Soul Stone's sacrifice at the Vormir Cliff (3.6, the plan's 6.1). The shared push helpers
 * ({@code StonePowers.shove} and every power built on it: beams, splashes, rings, columns, the Hulk's pushes) and the
 * telekinetic grip's release call {@link #credit}; a damage and a knockback are read from the game's events. The 3.6
 * skeleton writes the hook empty; lane L5 owns the class and writes what it records (the last player who pushed a
 * creature, and when, and the creatures that leave the summit's edge).
 */
public final class VormirTracker {

    /**
     * {@code pusher} has just pushed, thrown or dropped {@code victim} (a power that pushes without damage, a
     * telekinetic grip letting go). A pusher that is not a player, or none, gives no credit.
     */
    public static void credit(LivingEntity victim, @Nullable Entity pusher) {
    }

    private VormirTracker() {
    }
}
