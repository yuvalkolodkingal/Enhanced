package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.parkour.ParkourChallenge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The trials of the Road, in the order they are walked: the first is trial 1, and winning each unlocks the next. The
 * list is built here, in one place; a new trial is one line in the static block below.
 */
public final class RoadChallenges {
    private static final List<RoadChallenge> ALL = new ArrayList<>();
    /** The order number each trial was registered with (the list is kept sorted by it). */
    private static final List<Integer> ORDER = new ArrayList<>();

    static {
        // Trial 1: the Parkour. (Trial 2, copying a structure, goes between; trial 3, the ice-boat race, has the order number 3.)
        register(new ParkourChallenge());
        register(3, new IceBoatChallenge());
    }

    /** Adds a trial at the end of the Road. */
    public static synchronized void register(RoadChallenge challenge) {
        register(ORDER.isEmpty() ? 1 : ORDER.get(ORDER.size() - 1) + 1, challenge);
    }

    /**
     * Adds a trial with an explicit order number: it comes after every trial with a lower number and before every one with a higher
     * number, whatever the order they are registered in (so trials made apart from one another can say where they belong).
     */
    public static synchronized void register(int order, RoadChallenge challenge) {
        if (byId(challenge.id()) != null) {
            throw new IllegalArgumentException("Two trials called " + challenge.id());
        }
        int at = ORDER.size();
        while (at > 0 && ORDER.get(at - 1) > order) {
            at--;
        }
        ALL.add(at, challenge);
        ORDER.add(at, order);
    }

    /** The trials in order (a copy). */
    public static synchronized List<RoadChallenge> all() {
        return List.copyOf(ALL);
    }

    public static synchronized int count() {
        return ALL.size();
    }

    /** The trial with this number (0 is the first), or null. */
    @Nullable
    public static synchronized RoadChallenge get(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : null;
    }

    @Nullable
    public static synchronized RoadChallenge byId(String id) {
        for (RoadChallenge challenge : ALL) {
            if (challenge.id().equals(id)) {
                return challenge;
            }
        }
        return null;
    }

    public static synchronized int indexOf(RoadChallenge challenge) {
        return ALL.indexOf(challenge);
    }

    /** Takes a trial out of the Road again (a test that added a trial of its own, when it ends). */
    public static synchronized void unregister(RoadChallenge challenge) {
        int at = ALL.indexOf(challenge);
        if (at >= 0) {
            ALL.remove(at);
            ORDER.remove(at);
        }
    }

    private RoadChallenges() {
    }
}
