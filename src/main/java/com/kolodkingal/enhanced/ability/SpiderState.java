package com.kolodkingal.enhanced.ability;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What the server remembers about one Spider-Man between ticks (nothing of it is saved; the power, the look and the three
 * toggles are in {@code PowerData}). The times are game times. See {@link SpiderServer}.
 */
final class SpiderState {
    /** A zip or a swing: a web from his hand to an anchor, that the server watches while he moves along it. */
    static final class Line {
        final Vec3 anchor;
        final Direction face;
        /** Where his feet end up (a zip); for a swing, the anchor. */
        final Vec3 stand;
        /** The length of the rope (a swing), 0 for a zip. */
        final double rope;
        final long start;
        /** The creature a zip is aimed at (it is kicked on arrival), or null. */
        @Nullable
        final UUID kick;
        Vec3 last;
        /** The nearest he has been to {@link #stand} (a zip). */
        double nearest = Double.MAX_VALUE;
        boolean moved;
        /** The ticks in a row his position has hardly changed (a hitch holds it, see {@code lineEnd}). */
        int still;
        /** The length of the last step he made (not counting the ticks it was held). */
        double lastStep;
        int stalled;
        int groundTicks;
        /** A swing of Web Assist on a point of the open sky (no block holds it). */
        boolean sky;
        /** A swing of Web Assist (3.3.1: its first ticks allow the server's copy of him a longer step, see SpiderServer). */
        boolean assisted;

        Line(Vec3 anchor, Direction face, Vec3 stand, double rope, long start, @Nullable UUID kick, Vec3 at) {
            this.anchor = anchor;
            this.face = face;
            this.stand = stand;
            this.rope = rope;
            this.start = start;
            this.kick = kick;
            this.last = at;
        }
    }

    /** Web Shot: when the next may be cast (a hidden timer). */
    long shotReadyAt;
    /** Web Assist's climb out of a forest (3.3.1): when he last leapt (the key that follows it stays quiet). */
    long climbAt = Long.MIN_VALUE / 2;
    /** Until when the hand pose of a Web Shot shows ({@code Live.CASTING}). */
    long castUntil;
    /** Until when the spider-sense pose shows ({@code Live.SENSING}). */
    long senseUntil;
    @Nullable
    Line zip;
    @Nullable
    Line swing;

    // ---- clinging (the flag others see) ----
    SpiderClimb.Surface surface = SpiderClimb.Surface.NONE;
    /** The flag stays up until this time after the contact is lost. */
    long clingUntil;
    /** The surface is held at once (a zip has just arrived at it) until this time, whatever his motion. */
    long clingGraceUntil;
    int groundTicks;
    /** Where he was the last tick, and the heights before (for the fall check), and whether any of it is known. */
    @Nullable
    Vec3 lastPosition;
    double y1;
    double y2;
    double y3;
    int samples;
    /**
     * What the client of this Spider-Man last said he clings to ({@code SpiderClingPayload}), and when. Once a client has
     * said anything, it alone says it: the server no longer guesses from his motion.
     */
    SpiderClimb.Surface reported = SpiderClimb.Surface.NONE;
    long reportedAt = Long.MIN_VALUE / 2;
    boolean reporting;
    /** A jump has been seen: until this time a body that hangs in the air is at the top of its arc, not on a wall. */
    long arcUntil;
    boolean moving;

    // ---- the Iron Spider ----
    /** The nanites are scattered by Magneto until this time. */
    long stripUntil;
    long nextJab;
    int legCursor;
    /** Instant Kill has been asked for once: a second press before this time confirms it. */
    long killArmedUntil;
    long nextKill;

    // ---- Black Eyes ----
    long eyesEndsAt;
    /** The surge cannot be calmed until this time (it came by itself). */
    long eyesLockedUntil;

    // ---- spider-sense ----
    long nextScan;
    /** Entity id → the game time he was warned about it. */
    final Map<Integer, Long> warned = new HashMap<>();

    /** His web blocks, oldest first. */
    final ArrayDeque<SpiderWebs.WebRef> webs = new ArrayDeque<>();
}
