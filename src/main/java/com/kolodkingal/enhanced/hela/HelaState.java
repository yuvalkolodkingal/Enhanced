package com.kolodkingal.enhanced.hela;

/**
 * What the boss Hela is doing (3.5), synced to the clients with the game time it began: every pose of {@code
 * HelaBossPoses} and every tell is drawn from the state and the ticks since. The lengths are the server's: the fight
 * moves on when they run out.
 */
public enum HelaState {
    /** She forms on the dais out of the smoke of the seal; the headdress grows. Nothing hurts her. */
    ARRIVING(120, true),
    /** Between her attacks: she walks toward her target, the blades turning in her hands. */
    IDLE(0, false),
    /** The Necrosword Volley: the blades appear behind her (the tell), then fly. */
    VOLLEY(20, false),
    /** The Blade Wall: a low sweep of her hand, the cracks run along the floor, the spikes erupt. */
    WALL(24, false),
    /** The Shadow Step: she dissolves into smoke... */
    STEP_OUT(10, false),
    /** ... and comes out of it behind the farthest fighter. */
    STEP_IN(8, false),
    /** The blade combo: three blows. */
    MELEE(30, false),
    /** The Blade Rain: an arm raised, palm down; the blades hang over the circle, then fall. */
    RAIN(26, false),
    /** The Blade Storm: she spins, and a ring of blades sweeps out from her. */
    STORM(40, false),
    /** The Army of the Dead: she rises on the dais, arms lifting, and the dead climb out of the floor. Nothing hurts her. */
    RISE(60, true),
    /** The Strength of Asgard: she kneels on the dais and draws on Asgard. */
    CHANNEL(80, false),
    /** The channel was broken: she reels, and takes more damage. */
    STUNNED(60, false),
    /** A thrown Mjolnir caught in her hand and crushed. */
    CATCH(24, false),
    /** She falls: staggers, the headdress cracks, she dissolves into black smoke. Nothing hurts her. */
    DYING(40, true),
    /** She leaves the fight: she sinks back into the seal. Nothing hurts her. */
    LEAVING(40, true);

    private static final HelaState[] VALUES = values();

    /** How long the state lasts (ticks; 0: until something else happens). */
    public final int ticks;
    /** Whether nothing hurts her in it. */
    public final boolean untouchable;

    HelaState(int ticks, boolean untouchable) {
        this.ticks = ticks;
        this.untouchable = untouchable;
    }

    public byte id() {
        return (byte) ordinal();
    }

    public static HelaState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IDLE;
    }

    /** Whether it is one of her attacks (the brain waits for it to end before choosing the next). */
    public boolean attacking() {
        return switch (this) {
            case VOLLEY, WALL, STEP_OUT, STEP_IN, MELEE, RAIN, STORM, CATCH -> true;
            default -> false;
        };
    }
}
