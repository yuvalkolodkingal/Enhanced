package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import com.kolodkingal.enhanced.road.parkour.ParkourChallenge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The trials of the Road, in the order they are walked: the first is trial 1, and winning each unlocks the next. The
 * list is built here, in one place; a new trial is one line in the static block below. A trial has an order number: the trials
 * are walked from the lowest to the highest, so that each trial's line can say where it belongs whatever the order the lines
 * are in (parkour 10, copy the structure 20, the boat race 30).
 */
public final class RoadChallenges {
    private record Entry(int order, RoadChallenge challenge) {
    }

    private static final List<Entry> ALL = new ArrayList<>();

    static {
        register(new ParkourChallenge(), 10);
        register(new StructureCopyChallenge(), 20);
        // Trial 3, the ice-boat race, is added here with order 30.
    }

    /** Adds a trial at the end of the Road. */
    public static synchronized void register(RoadChallenge challenge) {
        register(challenge, ALL.isEmpty() ? 10 : ALL.get(ALL.size() - 1).order + 10);
    }

    /**
     * Adds a trial with an order number: it is walked after the trials with a lower number and before those with the same or a
     * higher one.
     */
    public static synchronized void register(RoadChallenge challenge, int order) {
        if (byId(challenge.id()) != null) {
            throw new IllegalArgumentException("Two trials called " + challenge.id());
        }
        int at = 0;
        while (at < ALL.size() && ALL.get(at).order < order) {
            at++;
        }
        ALL.add(at, new Entry(order, challenge));
    }

    /** The trials in order (a copy). */
    public static synchronized List<RoadChallenge> all() {
        List<RoadChallenge> all = new ArrayList<>();
        for (Entry entry : ALL) {
            all.add(entry.challenge);
        }
        return List.copyOf(all);
    }

    public static synchronized int count() {
        return ALL.size();
    }

    /** The trial with this number (0 is the first), or null. */
    @Nullable
    public static synchronized RoadChallenge get(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index).challenge : null;
    }

    @Nullable
    public static synchronized RoadChallenge byId(String id) {
        for (Entry entry : ALL) {
            if (entry.challenge.id().equals(id)) {
                return entry.challenge;
            }
        }
        return null;
    }

    public static synchronized int indexOf(RoadChallenge challenge) {
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).challenge == challenge) {
                return i;
            }
        }
        return -1;
    }

    /** Takes a trial out of the Road again (a test that added a trial of its own, when it ends). */
    public static synchronized void unregister(RoadChallenge challenge) {
        ALL.removeIf(entry -> entry.challenge == challenge);
    }

    /**
     * For tests: from now on the Road has exactly these trials, in this order, until the returned action is run (it puts back the
     * trials the Road had, with their order numbers). A test of the framework walks a Road of its own making this way, whatever
     * trials the mod has.
     */
    public static synchronized Runnable swap(List<RoadChallenge> trials) {
        List<Entry> before = new ArrayList<>(ALL);
        ALL.clear();
        int order = 10;
        for (RoadChallenge trial : trials) {
            ALL.add(new Entry(order, trial));
            order += 10;
        }
        return () -> {
            synchronized (RoadChallenges.class) {
                ALL.clear();
                ALL.addAll(before);
            }
        };
    }

    private RoadChallenges() {
    }
}
