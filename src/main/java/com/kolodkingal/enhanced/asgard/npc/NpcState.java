package com.kolodkingal.enhanced.asgard.npc;

/**
 * What an Asgardian is doing (3.5), synced to the clients, who pose him by it ({@code AsgardianPoses}). Saved by name:
 * append only.
 */
public enum NpcState {
    /** Strolling, standing guard, looking about. */
    IDLE,
    /** Talking: to a player on the talk screen, or to another Asgardian (a pair). */
    TALKING,
    /** Sparring with another warrior in the training ring (or with a player: Sif's training). */
    SPARRING,
    /** Sitting on a bench (or on the throne). */
    SITTING,
    /** Eating standing (Volstagg before 3.7.2; now he sits at the table: {@link #DINING}). */
    FEASTING,
    /** Asleep: Odin's Odinsleep in his chamber. */
    SLEEPING,
    /** Bowing to a player (the coronation, a god's arrival). */
    BOWING,
    /** Knocked down by Hela's army: kneeling, untouchable, rising after a while. */
    DOWN,
    /** Fighting Hela's army (or what his master commands). */
    FIGHTING,
    /** Sitting at a table, eating (3.7.2: Volstagg at the Hall of Feasts' high table, and his friends at the feast). */
    DINING;

    /** Whether the Asgardian may not walk, turn by himself or be talked to in this state. */
    public boolean still() {
        return this == SLEEPING || this == DOWN || this == BOWING;
    }

    public static NpcState byOrdinal(int ordinal) {
        NpcState[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : IDLE;
    }

    public static NpcState byName(String name) {
        for (NpcState state : values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return IDLE;
    }
}
