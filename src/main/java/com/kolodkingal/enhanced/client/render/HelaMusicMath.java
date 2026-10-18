package com.kolodkingal.enhanced.client.render;

/**
 * The numbers of the music of Hela's fight (3.5), with no client class (the game tests check them): which of the three
 * stems plays in which phase, and how loud it is at a distance from her. {@code HelaMusic} plays them.
 */
public final class HelaMusicMath {
    /** All of it within this many blocks of her, none from {@link #REACH}. */
    public static final double FULL = 48.0;
    public static final double REACH = 96.0;

    /** Whether layer {@code n} (1-3) plays in {@code phase}: each phase adds its stem to the ones before. */
    public static boolean layerWanted(int n, int phase) {
        return n <= phase;
    }

    /** How loud the music is at {@code distance} blocks from her (0 to 1). */
    public static float volumeAt(double distance) {
        if (distance <= FULL) {
            return 1.0F;
        }
        return distance >= REACH ? 0.0F : (float) (1.0 - (distance - FULL) / (REACH - FULL));
    }

    private HelaMusicMath() {
    }
}
