package com.kolodkingal.enhanced.ability;

/**
 * What Wanda's Chaos Shield needs to know about a projectile to stop it: implemented by the boss and hero projectiles
 * of 3.0 (Ultron's bolt, Thanos's sword, meteor and orb, Apocalypse's sand lance and boulder), so that {@link
 * WandaAbilities} needs no arm of its own for each of them.
 *
 * <p>Only the interface exists in the skeleton; the shield reads it once the Chaos Shield is refactored to take it.
 */
public interface ShieldCost {
    /** How many points of the shield's strength this projectile wears down when the shield stops it. */
    float shieldCost();

    /** Whether it is one of a kind, which the shield turns back to its owner instead of destroying (Thanos's sword). */
    default boolean oneOfAKind() {
        return false;
    }

    /** Turns it back toward where it came from (called only when {@link #oneOfAKind}). */
    default void turnHome() {
    }
}
