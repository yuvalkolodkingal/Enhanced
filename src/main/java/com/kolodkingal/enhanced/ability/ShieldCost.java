package com.kolodkingal.enhanced.ability;

/**
 * What Wanda's Chaos Shield (and any hero's shield built on {@link WandaAbilities#neutralize}) needs to know about a
 * projectile to stop it: implemented by the boss and hero projectiles of 3.0 (Ultron's bolt, Thanos's sword, meteor and
 * orb, Apocalypse's sand lance and boulder), so that {@link WandaAbilities} needs no arm of its own for each of them.
 * {@code WandaAbilities.projectilePower} reads {@link #shieldCost} before anything else it knows, and {@code
 * neutralize} turns a {@link #oneOfAKind} projectile home instead of destroying it.
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

    /**
     * Whether it is already on its way home (called only when {@link #oneOfAKind}): the shield lets it by, or it would
     * be turned round again and again.
     */
    default boolean returning() {
        return false;
    }
}
