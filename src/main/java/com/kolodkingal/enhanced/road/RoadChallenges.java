package com.kolodkingal.enhanced.road;

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

    static {
        // Trial 1: the Parkour. (Trial 2, copying a structure, and trial 3, the ice-boat race, are added here.)
        register(new ParkourChallenge());
    }

    /** Adds a trial at the end of the Road. */
    public static synchronized void register(RoadChallenge challenge) {
        if (byId(challenge.id()) != null) {
            throw new IllegalArgumentException("Two trials called " + challenge.id());
        }
        ALL.add(challenge);
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

    /** Puts the list back as it was (tests that add a trial of their own). */
    public static synchronized void restore(List<RoadChallenge> saved) {
        ALL.clear();
        ALL.addAll(saved);
    }

    private RoadChallenges() {
    }
}
