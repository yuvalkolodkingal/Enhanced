package com.kolodkingal.enhanced.thanos;

/**
 * What Thanos is doing, as the boss's state machine and its animation read it (synced to the clients as a byte).
 */
public enum ThanosState {
    /** Falling through the portal and rising from the landing: invulnerable, no AI (60 ticks). */
    ARRIVING,
    /** Walking toward a target, waiting for the gap between moves to end. */
    IDLE,
    /** The wind-up of a move: the tell everyone sees. A stone move is a clench that poise can break. */
    TELL,
    /** The move's effect is running (a beam, a drain, a ring that spreads). */
    ACTIVE,
    /** The short gap after a move. */
    RECOVER,
    /** A new stone has fused: he kneels, takes extra damage and does nothing. */
    SURGE,
    /** His poise broke, or the Gauntlet is locked: a stumble that takes extra damage. */
    STAGGERED,
    /** Defeated: he sits and turns to dust. Invulnerable. */
    DYING
}
