package com.kolodkingal.enhanced.ultron;

/**
 * What the Ultron body is doing, as it is synced to the clients ({@code UltronEntity.DATA_STATE}, with the game time the
 * state began in {@code DATA_STATE_TICK}): the model poses a tell, the effects draw it, and every client animates it the
 * same way because they all know the same tick. The ids are what travels (a byte); they never change.
 */
public enum UltronState {
    /** Hovering, fingers curled, a slow drift. */
    IDLE(0),
    /** Both arms forward, the fingertips glowing: the Repulsor Volley's tell and its bursts. */
    VOLLEY(1),
    /** Arms spread, ten aim lines converging on a point at the target's feet; then the beams. */
    BEAMS(2),
    /** Rising on his thrusters, then the dive and the landing. */
    DIVE(3),
    /** A fist raised in a vortex, then driven into the ground: the Gravity Pull. */
    PULL(4),
    /** Both arms thrown wide and up: the Call of the Sentries. */
    CALL(5),
    /** The right arm drawn back, then forward. */
    PUNCH(6),
    /** Grounded after a dive, venting steam, eyes dim to blue: he takes more damage. */
    STAGGER(7),
    /** Both arms in a V overhead, eyes ablaze: he is lifting Sokovia. */
    LIFT_CHANNEL(8),
    /** The body is destroyed and he is uploading into a Sentry: plates shudder, eyes flicker. */
    TRANSFER(9),
    /** The last body falls: the plates fall open, the eyes fade. */
    DEATH(10),
    /** Being assembled by the ritual: eyes dark, nothing can hurt him. */
    AWAKENING(11);

    private final int id;

    UltronState(int id) {
        this.id = id;
    }

    /** The byte that travels to the clients. */
    public byte id() {
        return (byte) id;
    }

    public static UltronState byId(int id) {
        for (UltronState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return IDLE;
    }

    /** Whether the state is one of the six attacks (the brain is busy). */
    public boolean isAttack() {
        return this == VOLLEY || this == BEAMS || this == DIVE || this == PULL || this == CALL || this == PUNCH;
    }
}
