package com.kolodkingal.enhanced.build;

/**
 * Every Build Magic spell at work or waiting on the server (D-B 11.7): the builders' queues, the budget all spells share
 * in each server tick, the turns they take, and what happens to them when the server stops.
 *
 * <p>A stub of the skeleton (3.1, step S): no spell ever runs yet. Lane BS fills it.
 */
public final class BuildScheduler {
    /** Drops every spell, working or waiting, and lets go of their chunks: for the server's stop and for tests. */
    public static void clear() {
    }

    private BuildScheduler() {
    }
}
