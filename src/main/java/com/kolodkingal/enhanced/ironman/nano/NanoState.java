package com.kolodkingal.enhanced.ironman.nano;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What each nanotech or Hulkbuster wearer carries from tick to tick on the server (3.8, lane L4): the moves under way
 * (the second repulsor blast, the beams, the shield, the blades' combo, the cannon's charge, the Refocuser's charge).
 * Forgotten when he leaves, dies or the server stops; nothing of it is saved (a move never survives a logout). The
 * synced side is {@code IronManState} and the {@code Live} bits.
 */
public final class NanoState {
    private static final Map<UUID, NanoState> STATES = new HashMap<>();

    // ---- repulsors ----
    /** The Mark 85's second (left-palm) blast: when it fires (0: none pending). */
    long secondBlastAt;

    // ---- beams (the Mark 50's Unibeam, the Mark 85's chest laser, the Hulkbuster's heavy Unibeam) ----
    /** When the beam began (0: none burns), and which. */
    long beamSince;
    int beamKind;
    long beamSent = Long.MIN_VALUE / 2;
    long beamHitAt = Long.MIN_VALUE / 2;

    // ---- the shields ----
    long shieldSince;

    // ---- blades ----
    /** The Mark 50's nano blade: until when it is out (0: in). */
    long nanoBladeUntil;
    /** The Mark 85's slash combo: the last step struck (-1: none), when, and when the next may. */
    int combo = -1;
    long lastSlash = Long.MIN_VALUE / 2;
    long nextSlash;

    // ---- the Mark 50's cannon ----
    /** When the charge began (0: none). */
    long cannonSince;

    // ---- the Lightning Refocuser ----
    /** Until when lightning that struck him charges the Refocuser (0: not charged). */
    long chargedUntil;

    /** The ones a chest laser or a heavy Unibeam hit in its current interval. */
    final IntSet beamHit = new IntOpenHashSet();

    public static NanoState of(UUID id) {
        return STATES.computeIfAbsent(id, k -> new NanoState());
    }

    public static NanoState peek(UUID id) {
        return STATES.get(id);
    }

    static void forget(UUID id) {
        STATES.remove(id);
    }

    static void clear() {
        STATES.clear();
    }

    /** Whether the Refocuser is charged by lightning now. */
    public boolean charged(long now) {
        return chargedUntil > now;
    }

    public long chargedUntil() {
        return chargedUntil;
    }
}
