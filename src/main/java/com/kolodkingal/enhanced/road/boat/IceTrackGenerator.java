package com.kolodkingal.enhanced.road.boat;

import com.kolodkingal.enhanced.road.boat.IceTrack.Checkpoint;
import com.kolodkingal.enhanced.road.boat.IceTrack.Feature;
import com.kolodkingal.enhanced.road.boat.IceTrack.FeatureKind;
import com.kolodkingal.enhanced.road.boat.IceTrack.Kind;
import com.kolodkingal.enhanced.road.boat.IceTrack.Loop;
import com.kolodkingal.enhanced.road.boat.IceTrack.Piece;
import com.kolodkingal.enhanced.road.boat.IceTrack.Raster;
import com.kolodkingal.enhanced.road.boat.IceTrack.Ribbon;
import com.kolodkingal.enhanced.road.boat.IceTrack.Route;
import com.kolodkingal.enhanced.road.boat.IceTrack.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Makes the course of the Ice-Boat Race from a seed: a long track of packed ice with walls on both sides, built of straights and
 * quarter turns (S-bends, hairpins), narrow stretches, snow drifts and snow walls, stripes of blue ice, and loops with a shortcut
 * across a gap. The same seed gives the same course. It is pure code (no Minecraft types), and it works to the boat's physics
 * ({@link IceBoatModel}):
 *
 * <ul>
 * <li>every curve is a quarter circle whose radius lets the boat drive it at {@link IceBoatModel#cornerSpeed} on part of its thrust,
 *     and it is a long enough straight away from the next one for the boat to slow down to that speed;</li>
 * <li>a hairpin is preceded by a rough strip of snow that takes the speed off;</li>
 * <li>the floor is never narrower than 3 blocks (the narrow stretches are 5, the track 9 by default);</li>
 * <li>a shortcut is a straight with a gap in it whose far side is one block lower, wide enough for the speed the straight before it
 *     gives ({@link IceBoatModel#minJumpSpeed}); the long way round meets it again, one block lower;</li>
 * <li>nothing crosses or touches anything else, and everything is within {@link #LIMIT} blocks of the middle of the cell.</li>
 * </ul>
 */
public final class IceTrackGenerator {
    /** How far from the middle of the cell (in blocks) anything of the course may be (the cell allows 450). */
    public static final int LIMIT = 436;
    /** The thickness of the walls. */
    public static final int WALL = 2;
    /** The empty space kept between two parts of the course (beyond their walls). */
    public static final int GAP = 3;
    /** The height of the walls above the floor, and of the outside wall of a curve. */
    public static final int WALL_HEIGHT = 2;
    public static final int BANK_HEIGHT = 4;
    /** Half the width of the floor of the shortcut. */
    public static final double CHORD_HALF_WIDTH = 2.5;
    /** The narrowest stretch (blocks across). */
    public static final int NARROW_WIDTH = 5;
    /** The length of the first straight, counted from the back of the start platform. */
    public static final int LAUNCH = 56;
    /** From the middle of the start platform: where the start platform's back edge, the boat, the start line, and the barrier are. */
    public static final int PLATFORM_BACK = 3;
    public static final int BOAT_PARK = 6;
    public static final int START_LINE = 9;
    public static final int BARRIER = 11;
    /** The length of the last straight before the finish platform, and how far back from its end the finish line is. */
    public static final int FINISH_STRAIGHT = 52;
    public static final int FINISH_LINE_BACK = 14;
    /** Rows of the finish platform (9 across). */
    public static final int FINISH_DOCK = 8;
    /** The straight a hairpin or a fork is approached on, at least. */
    public static final int APPROACH = 44;
    /** The gap of a shortcut, in blocks of air, and the lower level of its far side. */
    public static final int GAP_WIDTH = 5;
    public static final int GAP_DROP = 1;
    /** The straight in front of the gap of a shortcut. */
    public static final int RUN_UP = 70;
    /** The most the course asks of a boat before a curve: the straight after a curve is at least this long. */
    public static final int MIN_STRAIGHT = 24;

    /**
     * The settings.
     *
     * @param length          the length of the course from the start line to the finish line, in blocks (it comes out between this and
     *                        about this plus 120)
     * @param width           the width of the track in blocks (made odd)
     * @param difficulty      1 easy (wide curves, no snow walls), 2 normal, 3 hard (tighter curves, more of everything)
     * @param shortcuts       how many shortcuts the course may have
     * @param obstacles       whether there are snow drifts and snow walls
     * @param blueIce         whether there are stripes of blue ice
     * @param checkpointEvery a checkpoint about every this many blocks
     */
    public record Params(int length, int width, int difficulty, int shortcuts, boolean obstacles, boolean blueIce, int checkpointEvery) {
        public Params {
            width = Math.max(7, width) | 1;
            difficulty = Math.max(1, Math.min(3, difficulty));
            shortcuts = Math.max(0, shortcuts);
            checkpointEvery = Math.max(40, checkpointEvery);
        }

        public double halfWidth() {
            return width / 2.0;
        }
    }

    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DZ = {0, 1, 0, -1};

    /** Makes the course of this seed. The same seed and settings always give the same course. */
    public static IceTrack generate(long seed, Params params) {
        for (int attempt = 0; attempt < 600; attempt++) {
            Gen gen = new Gen(seed, attempt, params);
            IceTrack track = gen.run();
            if (track != null) {
                return track;
            }
        }
        throw new IllegalStateException("No course could be laid for seed " + seed);
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The state of the walk along the line while it is laid out. */
    private static final class Walker {
        double x;
        double z;
        int h;
        double s;
        int level;
        double hw;

        Walker copy() {
            Walker w = new Walker();
            w.x = x;
            w.z = z;
            w.h = h;
            w.s = s;
            w.level = level;
            w.hw = hw;
            return w;
        }

        int dx() {
            return DX[Math.floorMod(h, 4)];
        }

        int dz() {
            return DZ[Math.floorMod(h, 4)];
        }

        /** A straight of {@code len} blocks with one row of samples a block; the width goes from the current one to {@code hwEnd}. */
        void straight(List<Sample> out, int len, double hwEnd, boolean first) {
            double hw0 = hw;
            double x0 = x;
            double z0 = z;
            double s0 = s;
            for (int u = first ? 0 : 1; u <= len; u++) {
                double w = hw0 + (hwEnd - hw0) * u / (double) Math.max(1, len);
                out.add(new Sample(x0 + dx() * u, z0 + dz() * u, dx(), dz(), w, level, s0 + u, 0, 0.0));
            }
            x = x0 + dx() * len;
            z = z0 + dz() * len;
            s = s0 + len;
            hw = hwEnd;
        }

        /** A straight whose width is given row by row. */
        void straight(List<Sample> out, int len, java.util.function.IntToDoubleFunction widthAt) {
            double x0 = x;
            double z0 = z;
            double s0 = s;
            for (int u = 1; u <= len; u++) {
                out.add(new Sample(x0 + dx() * u, z0 + dz() * u, dx(), dz(), widthAt.applyAsDouble(u), level, s0 + u, 0, 0.0));
            }
            x = x0 + dx() * len;
            z = z0 + dz() * len;
            s = s0 + len;
            hw = widthAt.applyAsDouble(len);
        }

        /** A quarter turn of radius {@code radius}: +1 to the right, -1 to the left. */
        void arc(List<Sample> out, int radius, int turn, double hwTarget) {
            int steps = (int) Math.ceil(radius * Math.PI / 2.0 / 0.5);
            int nh = Math.floorMod(h + turn, 4);
            double nx = DX[nh];
            double nz = DZ[nh];
            double cx = x + radius * nx;
            double cz = z + radius * nz;
            double ddx = dx();
            double ddz = dz();
            double s0 = s;
            double hw0 = hw;
            for (int k = 1; k <= steps; k++) {
                double t = Math.PI / 2.0 * k / steps;
                double px = cx + radius * (-nx * Math.cos(t) + ddx * Math.sin(t));
                double pz = cz + radius * (-nz * Math.cos(t) + ddz * Math.sin(t));
                double tx = nx * Math.sin(t) + ddx * Math.cos(t);
                double tz = nz * Math.sin(t) + ddz * Math.cos(t);
                double w = hw0 + (hwTarget - hw0) * k / steps;
                out.add(new Sample(px, pz, tx, tz, w, level, s0 + radius * t, turn, radius));
            }
            x = cx + radius * ddx;
            z = cz + radius * ddz;
            h = nh;
            s = s0 + radius * Math.PI / 2.0;
            hw = hwTarget;
        }
    }

    /** What a piece adds, before it is known to fit. */
    private static final class Candidate {
        final List<Sample> main = new ArrayList<>();
        final List<Sample> chordA = new ArrayList<>();
        final List<Sample> chordB = new ArrayList<>();
        final List<Piece> pieces = new ArrayList<>();
        final List<Feature> features = new ArrayList<>();
        final List<Loop> loops = new ArrayList<>();
        Walker end;
        boolean turnish;
        boolean isHairpin;
        boolean isLoop;
        double saving;
    }

    /** One attempt at a course. */
    private static final class Gen {
        final long seed;
        final Params p;
        final SplittableRandom rng;
        final short[] claim;
        final int span = 2 * LIMIT + 1;
        int group = 1;
        int lastGroup = 0;

        final List<Sample> main = new ArrayList<>();
        final List<List<Sample>> chords = new ArrayList<>();
        final List<Piece> pieces = new ArrayList<>();
        final List<Feature> features = new ArrayList<>();
        final List<Loop> loops = new ArrayList<>();
        Walker w = new Walker();
        boolean lastTurnish;
        int hairpins;
        int loopCount;
        double savings;
        int h0;
        double px;
        double pz;
        double startS;
        double finishS;
        double finishX;
        double finishZ;
        double length;
        double targetEnd;

        Gen(long seed, int attempt, Params p) {
            this.seed = seed;
            this.p = p;
            this.rng = new SplittableRandom(seed * 0x9E3779B97F4A7C15L + attempt * 0xBF58476D1CE4E5B9L + 0x94D049BB133111EBL);
            this.claim = new short[span * span];
        }

        // ---- the ground ----

        boolean inside(int cx, int cz) {
            return cx >= -LIMIT && cx <= LIMIT && cz >= -LIMIT && cz <= LIMIT;
        }

        int idx(int cx, int cz) {
            return (cz + LIMIT) * span + (cx + LIMIT);
        }

        /** Whether the samples are in bounds and clear of what is laid (the last group excepted); with {@code stamp} they are claimed. */
        boolean place(List<Sample> samples, int groupId, boolean stamp, int ignore) {
            for (Sample sm : samples) {
                int reach = (int) Math.ceil(sm.hw()) + WALL + (stamp ? 0 : GAP);
                int cx0 = (int) Math.floor(sm.x());
                int cz0 = (int) Math.floor(sm.z());
                double rr = sm.hw() + WALL + (stamp ? 0 : GAP);
                for (int cx = cx0 - reach; cx <= cx0 + reach; cx++) {
                    for (int cz = cz0 - reach; cz <= cz0 + reach; cz++) {
                        double ddx = cx + 0.5 - sm.x();
                        double ddz = cz + 0.5 - sm.z();
                        if (ddx * ddx + ddz * ddz > rr * rr) {
                            continue;
                        }
                        if (!inside(cx, cz)) {
                            if (stamp) {
                                continue;
                            }
                            return false;
                        }
                        int i = idx(cx, cz);
                        if (stamp) {
                            claim[i] = (short) groupId;
                        } else if (claim[i] != 0 && claim[i] != ignore && claim[i] != groupId) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        boolean fits(Candidate c) {
            int ignore = lastGroup;
            if (!place(c.main, group, false, ignore)) {
                return false;
            }
            if (!c.chordA.isEmpty() && !place(c.chordA, group, false, ignore)) {
                return false;
            }
            return c.chordB.isEmpty() || place(c.chordB, group, false, ignore);
        }

        void commit(Candidate c) {
            place(c.main, group, true, 0);
            place(c.chordA, group, true, 0);
            place(c.chordB, group, true, 0);
            main.addAll(c.main);
            if (!c.chordA.isEmpty()) {
                List<Sample> chord = new ArrayList<>(c.chordA);
                chord.addAll(c.chordB);
                chords.add(chord);
            }
            pieces.addAll(c.pieces);
            features.addAll(c.features);
            loops.addAll(c.loops);
            w = c.end;
            lastTurnish = c.turnish;
            if (c.isHairpin) {
                hairpins++;
            }
            if (c.isLoop) {
                loopCount++;
                savings += c.saving;
            }
            lastGroup = group;
            group++;
        }

        // ---- the pieces ----

        int turnRadius() {
            return switch (p.difficulty()) {
                case 1 -> 34 + rng.nextInt(17);
                case 2 -> 26 + rng.nextInt(15);
                default -> 22 + rng.nextInt(11);
            };
        }

        int sRadius() {
            return switch (p.difficulty()) {
                case 1 -> 30 + rng.nextInt(15);
                case 2 -> 24 + rng.nextInt(11);
                default -> 20 + rng.nextInt(9);
            };
        }

        int hairpinRadius() {
            return switch (p.difficulty()) {
                case 1 -> 20;
                case 2 -> 16 + rng.nextInt(3);
                default -> 13 + rng.nextInt(3);
            };
        }

        Candidate straightPiece(Kind kind, int len) {
            Candidate c = new Candidate();
            Walker t = w.copy();
            double s0 = t.s;
            int h = t.h;
            double x0 = t.x;
            double z0 = t.z;
            double base = p.halfWidth();
            switch (kind) {
                case NARROW -> {
                    double narrow = NARROW_WIDTH / 2.0;
                    int ramp = 10;
                    t.straight(c.main, len, u -> {
                        double down = Math.min(1.0, u / (double) ramp);
                        double up = Math.min(1.0, (len - u) / (double) ramp);
                        return base - (base - narrow) * Math.min(down, up);
                    });
                }
                default -> t.straight(c.main, len, t.hw, false);
            }
            c.pieces.add(new Piece(kind, group, s0, t.s, 0, Route.MAIN, h, 0));
            c.end = t;
            addFeatures(c, kind, x0, z0, h, len);
            return c;
        }

        /** The snow and the blue ice of a straight, in cells. */
        void addFeatures(Candidate c, Kind kind, double x0, double z0, int h, int len) {
            int level = w.level;
            int hwi = (int) Math.floor(p.halfWidth());
            switch (kind) {
                case BRAKE -> {
                    // A strip of snow across all of the track, a little before the end of the straight.
                    int u0 = len - 18;
                    c.features.add(rect(FeatureKind.SNOW, x0, z0, h, u0, u0 + 7, -hwi - 1, hwi + 1, level));
                }
                case DRIFTS -> {
                    int side = rng.nextBoolean() ? 1 : -1;
                    int count = 1 + rng.nextInt(2);
                    int u = 14 + rng.nextInt(10);
                    // Covers all but three cells of the width, on one side; the other side is clear ice.
                    int cover = (2 * hwi + 1) - 3;
                    for (int i = 0; i < count && u + 12 < len - 6; i++) {
                        int length = 8 + rng.nextInt(6);
                        int a = side > 0 ? -hwi : hwi - cover + 1;
                        int b = side > 0 ? -hwi + cover - 1 : hwi;
                        c.features.add(rect(FeatureKind.SNOW, x0, z0, h, u, u + length, a, b, level));
                        side = -side;
                        u += length + 14 + rng.nextInt(10);
                    }
                }
                case CHICANE -> {
                    int side = rng.nextBoolean() ? 1 : -1;
                    int walls = len >= 100 ? 3 : 2;
                    int cover = (2 * hwi + 1) - 5;
                    int u = 16;
                    for (int i = 0; i < walls; i++) {
                        int a = side > 0 ? hwi - cover + 1 : -hwi;
                        int b = side > 0 ? hwi : -hwi + cover - 1;
                        c.features.add(rect(FeatureKind.SNOW_WALL, x0, z0, h, u, u + 1, a, b, level));
                        side = -side;
                        u += 24;
                    }
                }
                case BLUE_LANE -> {
                    int u0 = 14 + rng.nextInt(Math.max(1, len - 50));
                    c.features.add(rect(FeatureKind.BLUE_ICE, x0, z0, h, u0, u0 + 28, -1, 1, level));
                }
                default -> {
                }
            }
        }

        /** The rectangle of cells that is {@code u0..u1} along the straight and {@code l0..l1} across (right positive). */
        Feature rect(FeatureKind kind, double x0, double z0, int h, int u0, int u1, int l0, int l1, int level) {
            int dx = DX[Math.floorMod(h, 4)];
            int dz = DZ[Math.floorMod(h, 4)];
            int rx = -dz;
            int rz = dx;
            int cx0 = (int) Math.floor(x0);
            int cz0 = (int) Math.floor(z0);
            int ax = cx0 + dx * u0 + rx * l0;
            int az = cz0 + dz * u0 + rz * l0;
            int bx = cx0 + dx * u1 + rx * l1;
            int bz = cz0 + dz * u1 + rz * l1;
            return new Feature(kind, Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz), level);
        }

        Candidate turnPiece(int turn) {
            Candidate c = new Candidate();
            Walker t = w.copy();
            int radius = turnRadius();
            double s0 = t.s;
            int h = t.h;
            t.arc(c.main, radius, turn, t.hw);
            c.pieces.add(new Piece(Kind.TURN, group, s0, t.s, radius, Route.MAIN, h, turn));
            c.end = t;
            c.turnish = true;
            return c;
        }

        Candidate sBend(int turn) {
            Candidate c = new Candidate();
            Walker t = w.copy();
            int radius = sRadius();
            double s0 = t.s;
            int h = t.h;
            int between = rng.nextInt(10);
            t.arc(c.main, radius, turn, t.hw);
            if (between > 0) {
                t.straight(c.main, between, t.hw, false);
            }
            t.arc(c.main, radius, -turn, t.hw);
            c.pieces.add(new Piece(Kind.S_BEND, group, s0, t.s, radius, Route.MAIN, h, turn));
            c.end = t;
            c.turnish = true;
            return c;
        }

        /** The snow strip, then a wider hairpin. */
        Candidate hairpin(int turn) {
            Candidate c = new Candidate();
            Walker t = w.copy();
            int radius = hairpinRadius();
            double base = p.halfWidth();
            double wide = base + 1.0;
            double x0 = t.x;
            double z0 = t.z;
            int h = t.h;
            double s0 = t.s;
            // The approach: snow across the track, then the track opens out.
            t.straight(c.main, APPROACH, wide, false);
            c.pieces.add(new Piece(Kind.BRAKE, group, s0, t.s, 0, Route.MAIN, h, 0));
            c.features.add(rect(FeatureKind.SNOW, x0, z0, h, APPROACH - 14, APPROACH - 7, -(int) wide - 1, (int) wide + 1, t.level));
            double a0 = t.s;
            int mid = 2 + rng.nextInt(6);
            t.arc(c.main, radius, turn, wide);
            t.straight(c.main, mid, wide, false);
            t.arc(c.main, radius, turn, wide);
            c.pieces.add(new Piece(Kind.HAIRPIN, group, a0, t.s, radius, Route.MAIN, h, turn));
            // The width goes back to normal on the straight after it (the next piece starts with the wide track: narrow it at once).
            double s1 = t.s;
            t.straight(c.main, 16, base, false);
            c.pieces.add(new Piece(Kind.STRAIGHT, group, s1, t.s, 0, Route.MAIN, t.h, 0));
            c.end = t;
            c.turnish = true;
            c.isHairpin = true;
            return c;
        }

        /** A fork: the long way round (a bump out to one side) and a straight across it with a gap. */
        Candidate loop(int side) {
            Candidate c = new Candidate();
            Walker t = w.copy();
            double base = p.halfWidth();
            int r = 24;
            int a = 18 + rng.nextInt(7);
            int b = 44 + rng.nextInt(17);
            double s0 = t.s;
            int h = t.h;
            double x0 = t.x;
            double z0 = t.z;
            int level0 = t.level;
            // The approach to the fork.
            t.straight(c.main, APPROACH, base, false);
            c.pieces.add(new Piece(Kind.STRAIGHT, group, s0, t.s, 0, Route.MAIN, h, 0));
            double forkS = t.s;
            double forkX = t.x;
            double forkZ = t.z;
            // The long way round: out, along, and back; one block lower after the middle of the way back.
            Walker bump = t.copy();
            bump.arc(c.main, r, side, base);
            bump.straight(c.main, a, base, false);
            bump.arc(c.main, r, -side, base);
            bump.straight(c.main, b, base, false);
            bump.arc(c.main, r, -side, base);
            int cliffAt = c.main.size();
            bump.straight(c.main, a, base, false);
            int dropFrom = cliffAt + a / 2;
            for (int i = dropFrom; i < c.main.size(); i++) {
                Sample sm = c.main.get(i);
                c.main.set(i, new Sample(sm.x(), sm.z(), sm.dx(), sm.dz(), sm.hw(), level0 - GAP_DROP, sm.s(), sm.turn(), sm.radius()));
            }
            bump.level = level0 - GAP_DROP;
            bump.arc(c.main, r, side, base);
            c.pieces.add(new Piece(Kind.FORK, group, forkS, bump.s, r, Route.MAIN, h, side));
            // The short way: a straight to the end of the long way, with a gap in it.
            int chordLength = (int) Math.round(Math.abs((bump.x - forkX) * DX[Math.floorMod(h, 4)] + (bump.z - forkZ) * DZ[Math.floorMod(h, 4)]));
            int gapFrom = RUN_UP;
            if (chordLength < RUN_UP + GAP_WIDTH + 40) {
                return null;
            }
            Walker chord = t.copy();
            chord.hw = CHORD_HALF_WIDTH;
            chord.s = 0;
            int len = chordLength;
            for (int u = 0; u <= len; u++) {
                boolean inGap = u >= gapFrom && u < gapFrom + GAP_WIDTH;
                if (inGap) {
                    continue;
                }
                int lv = u >= gapFrom + GAP_WIDTH ? level0 - GAP_DROP : level0;
                Sample sm = new Sample(forkX + DX[Math.floorMod(h, 4)] * u, forkZ + DZ[Math.floorMod(h, 4)] * u, DX[Math.floorMod(h, 4)],
                        DZ[Math.floorMod(h, 4)], CHORD_HALF_WIDTH, lv, u, 0, 0.0);
                if (u < gapFrom) {
                    c.chordA.add(sm);
                } else {
                    c.chordB.add(sm);
                }
            }
            double bumpLength = bump.s - forkS;
            c.saving = bumpLength - len;
            c.loops.add(new Loop(forkX, forkZ, h, gapFrom, GAP_WIDTH, GAP_DROP, gapFrom, c.saving));
            c.end = bump;
            c.isLoop = true;
            c.turnish = true;
            return c;
        }

        // ---- the course ----

        IceTrack run() {
            // The start: a platform on the edge of the field, facing in.
            int[] headings = {0, 2, 3};
            h0 = headings[rng.nextInt(headings.length)];
            int dx = DX[h0];
            int dz = DZ[h0];
            int lateral = rng.nextInt(2 * 170 + 1) - 170;
            int back = LIMIT - 14 - (int) Math.ceil(p.halfWidth());
            px = (dx != 0 ? -dx * back : lateral) + 0.5;
            pz = (dz != 0 ? -dz * back : lateral) + 0.5;
            w.x = px - dx * PLATFORM_BACK;
            w.z = pz - dz * PLATFORM_BACK;
            w.h = h0;
            w.hw = p.halfWidth();
            w.level = 0;
            w.s = 0;
            Candidate start = new Candidate();
            w.straight(start.main, LAUNCH, w.hw, true);
            start.pieces.add(new Piece(Kind.STRAIGHT, 0, 0, w.s, 0, Route.MAIN, h0, 0));
            start.end = w;
            if (!place(start.main, group, false, 0)) {
                return null;
            }
            group = 1;
            commit(start);
            startS = PLATFORM_BACK + START_LINE + 0.5;

            double target = startS + p.length() + FINISH_LINE_BACK - 0.5;
            targetEnd = target;
            int guard = 0;
            while (w.s + MIN_STRAIGHT + FINISH_STRAIGHT <= target && guard++ < 400) {
                boolean laid = false;
                for (int tries = 0; tries < 80 && !laid; tries++) {
                    Candidate c = pick(target);
                    if (c != null && fits(c)) {
                        commit(c);
                        laid = true;
                    }
                }
                if (!laid) {
                    return null;
                }
            }
            // The finish: a last straight (long enough to make the length), and the platform.
            int finishLen = (int) Math.max(FINISH_STRAIGHT, Math.min(260.0, Math.ceil(target - w.s)));
            Candidate fin = new Candidate();
            Walker t = w.copy();
            double s0 = t.s;
            int h = t.h;
            // The width: the track's.
            t.straight(fin.main, finishLen + FINISH_DOCK, p.halfWidth(), false);
            fin.pieces.add(new Piece(Kind.FINISH, group, s0, t.s, 0, Route.MAIN, h, 0));
            fin.end = t;
            if (!fits(fin)) {
                // Try the plain length.
                fin = new Candidate();
                t = w.copy();
                t.straight(fin.main, FINISH_STRAIGHT + FINISH_DOCK, p.halfWidth(), false);
                fin.pieces.add(new Piece(Kind.FINISH, group, s0, t.s, 0, Route.MAIN, h, 0));
                fin.end = t;
                finishLen = FINISH_STRAIGHT;
                if (!fits(fin)) {
                    return null;
                }
            }
            double endS = s0 + finishLen;
            finishS = endS - FINISH_LINE_BACK + 0.5;
            finishX = w.x + DX[Math.floorMod(h, 4)] * (finishLen + 4);
            finishZ = w.z + DZ[Math.floorMod(h, 4)] * (finishLen + 4);
            commit(fin);
            length = finishS - startS;
            return finish();
        }

        Candidate pick(double target) {
            double left = target - w.s - FINISH_STRAIGHT;
            int roll = rng.nextInt(100);
            int turn = rng.nextBoolean() ? 1 : -1;
            int d = p.difficulty();
            if (lastTurnish) {
                // After a curve: a straight of at least MIN_STRAIGHT, with whatever a straight may carry.
                return straightKind(left, true);
            }
            if (loopCount < p.shortcuts() && w.s > 120 && left > 330 && roll < 28) {
                return loop(turn);
            }
            if (hairpins < (d == 1 ? 1 : d == 2 ? 2 : 3) && left > 150 && roll < 30) {
                return hairpin(turn);
            }
            if (roll < 55) {
                return turnPiece(turn);
            }
            if (roll < 72) {
                return sBend(turn);
            }
            return straightKind(left, false);
        }

        Candidate straightKind(double left, boolean afterTurn) {
            int roll = rng.nextInt(100);
            int minLen = afterTurn ? MIN_STRAIGHT : 30;
            int len = minLen + rng.nextInt(40);
            int d = p.difficulty();
            if (roll < 22 && p.obstacles() && d >= 2) {
                return straightPiece(Kind.CHICANE, Math.max(len, 80) + rng.nextInt(30));
            }
            if (roll < 40 && p.obstacles()) {
                return straightPiece(Kind.DRIFTS, Math.max(len, 55));
            }
            if (roll < 56) {
                return straightPiece(Kind.NARROW, Math.max(len, 50));
            }
            if (roll < 72 && p.blueIce()) {
                return straightPiece(Kind.BLUE_LANE, Math.max(len, 70));
            }
            return straightPiece(Kind.STRAIGHT, len + 6);
        }

        // ---- the result ----

        IceTrack finish() {
            // The features of the start and finish: the lines across the track.
            int hwi = (int) Math.floor(p.halfWidth());
            features.add(rect(FeatureKind.LINE, px, pz, h0, START_LINE, START_LINE + 1, -hwi, hwi, 0));
            Piece last = pieces.get(pieces.size() - 1);
            int fh = last.heading();
            double endX = main.get(main.size() - 1 - FINISH_DOCK).x();
            double endZ = main.get(main.size() - 1 - FINISH_DOCK).z();
            int lowest = 0;
            for (Sample sm : main) {
                lowest = Math.min(lowest, sm.level());
            }
            features.add(rect(FeatureKind.LINE, endX, endZ, fh, -FINISH_LINE_BACK, -FINISH_LINE_BACK + 1, -hwi, hwi, lowest));
            List<Ribbon> ribbons = new ArrayList<>();
            ribbons.add(new Ribbon(Route.MAIN, main));
            for (List<Sample> chord : chords) {
                ribbons.add(new Ribbon(Route.CHORD, chord));
            }
            List<Checkpoint> checkpoints = checkpoints(lowest);
            Raster raster = raster(ribbons, lowest);
            if (raster == null) {
                return null;
            }
            return new IceTrack(seed, p, ribbons, pieces, features, checkpoints, loops, raster, h0, fh, px, pz, startS, finishS, finishX, finishZ,
                    length, length - savings, lowest);
        }

        /** Whether the main line is a plain straight at this sample and for {@code around} samples either side. */
        List<Checkpoint> checkpoints(int lowest) {
            int n = main.size();
            // 2 = a plain straight clear of snow and snow walls, 1 = a straight with something near, 0 = no place for a checkpoint.
            int[] ok = new int[n];
            for (int i = 0; i < n; i++) {
                Sample sm = main.get(i);
                if (sm.turn() != 0 || sm.s() <= startS + 30 || sm.s() >= finishS - 60 || onBump(sm)) {
                    continue;
                }
                boolean plain = true;
                boolean uniform = true;
                for (int j = Math.max(0, i - 5); j <= Math.min(n - 1, i + 5); j++) {
                    Sample o = main.get(j);
                    if (o.turn() != 0 || o.level() != sm.level() || o.dx() != sm.dx() || o.dz() != sm.dz()) {
                        plain = false;
                        uniform = false;
                        break;
                    }
                    if (Math.abs(o.hw() - sm.hw()) > 1.0E-9) {
                        plain = false;
                    }
                }
                if (uniform) {
                    ok[i] = plain && clearOfFeatures(sm) ? 2 : 1;
                }
            }
            List<Checkpoint> result = new ArrayList<>();
            double step = p.checkpointEvery();
            double wanted = startS + step;
            double last = startS;
            while (wanted < finishS - 60) {
                int best = -1;
                double bestScore = Double.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    if (ok[i] == 0) {
                        continue;
                    }
                    double s = main.get(i).s();
                    if (s < last + step * 0.45) {
                        continue;
                    }
                    double gap = Math.abs(s - wanted);
                    if (gap > step * 0.8) {
                        continue;
                    }
                    // A clear straight beats a nearer one with snow beside it.
                    double score = gap + (ok[i] == 2 ? 0.0 : step * 0.5);
                    if (score < bestScore) {
                        bestScore = score;
                        best = i;
                    }
                }
                if (best >= 0) {
                    Sample sm = main.get(best);
                    result.add(new Checkpoint(sm.x(), sm.z(), headingOf(sm), sm.hw(), sm.level(), sm.s()));
                    last = sm.s();
                    wanted = last + step;
                } else {
                    wanted += step * 0.25;
                }
            }
            return result;
        }

        boolean clearOfFeatures(Sample sm) {
            for (Feature f : features) {
                if (f.kind() == FeatureKind.LINE || f.kind() == FeatureKind.BLUE_ICE) {
                    continue;
                }
                int cx = (int) Math.floor(sm.x());
                int cz = (int) Math.floor(sm.z());
                if (cx >= f.x0() - 9 && cx <= f.x1() + 9 && cz >= f.z0() - 9 && cz <= f.z1() + 9) {
                    return false;
                }
            }
            return true;
        }

        boolean onBump(Sample sm) {
            for (Piece piece : pieces) {
                if (piece.kind() == Kind.FORK && sm.s() > piece.s0() && sm.s() < piece.s1()) {
                    return true;
                }
            }
            return false;
        }

        // ---- the grid ----

        Raster raster(List<Ribbon> ribbons, int lowest) {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Ribbon ribbon : ribbons) {
                for (Sample sm : ribbon.samples()) {
                    int reach = (int) Math.ceil(sm.hw()) + WALL + 2;
                    minX = Math.min(minX, (int) Math.floor(sm.x()) - reach);
                    maxX = Math.max(maxX, (int) Math.floor(sm.x()) + reach);
                    minZ = Math.min(minZ, (int) Math.floor(sm.z()) - reach);
                    maxZ = Math.max(maxZ, (int) Math.floor(sm.z()) + reach);
                }
            }
            Raster r = new Raster(minX, minZ, maxX - minX + 1, maxZ - minZ + 1);
            int w = r.maxX() - r.minX() + 1;
            int d = r.maxZ() - r.minZ() + 1;
            float[] best = new float[w * d];
            java.util.Arrays.fill(best, Float.MAX_VALUE);
            boolean[] outer = new boolean[w * d];
            for (Ribbon ribbon : ribbons) {
                for (Sample sm : ribbon.samples()) {
                    int reach = (int) Math.ceil(sm.hw()) + 1;
                    int cx0 = (int) Math.floor(sm.x());
                    int cz0 = (int) Math.floor(sm.z());
                    boolean row = sm.turn() == 0;
                    for (int cx = cx0 - reach; cx <= cx0 + reach; cx++) {
                        for (int cz = cz0 - reach; cz <= cz0 + reach; cz++) {
                            double ddx = cx + 0.5 - sm.x();
                            double ddz = cz + 0.5 - sm.z();
                            double along = ddx * sm.dx() + ddz * sm.dz();
                            double lat = ddx * -sm.dz() + ddz * sm.dx();
                            if (row) {
                                if (Math.abs(along) >= 0.5 || Math.abs(lat) > sm.hw() + 1.0E-9) {
                                    continue;
                                }
                            } else if (ddx * ddx + ddz * ddz > sm.hw() * sm.hw()) {
                                continue;
                            }
                            int i = (cz - r.minZ()) * w + (cx - r.minX());
                            float dist = (float) (ddx * ddx + ddz * ddz);
                            if (dist < best[i]) {
                                best[i] = dist;
                                r.set(cx, cz, IceTrack.PACKED, sm.level(), 0);
                                outer[i] = sm.turn() != 0 && lat * sm.turn() < 0 && Math.abs(lat) > sm.hw() - 3.0;
                            }
                        }
                    }
                }
            }
            // The gap of a shortcut counts as floor for the walls (the rails run on over the air), then it is cleared again.
            List<int[]> holes = new ArrayList<>();
            for (Loop loop : loops) {
                int dx = DX[Math.floorMod(loop.heading(), 4)];
                int dz = DZ[Math.floorMod(loop.heading(), 4)];
                int cx0 = (int) Math.floor(loop.forkX());
                int cz0 = (int) Math.floor(loop.forkZ());
                int hwi = (int) Math.floor(CHORD_HALF_WIDTH);
                for (int u = loop.gapFrom(); u < loop.gapFrom() + loop.gap(); u++) {
                    for (int l = -hwi; l <= hwi; l++) {
                        int cx = cx0 + dx * u + (-dz) * l;
                        int cz = cz0 + dz * u + dx * l;
                        r.set(cx, cz, IceTrack.PACKED, 0, 0);
                        holes.add(new int[]{cx, cz});
                    }
                }
            }
            // The walls: two cells round the floor, as high as the floor beside them says.
            List<int[]> floors = new ArrayList<>();
            for (int cz = r.minZ(); cz <= r.maxZ(); cz++) {
                for (int cx = r.minX(); cx <= r.maxX(); cx++) {
                    if (r.isFloor(cx, cz)) {
                        floors.add(new int[]{cx, cz});
                    }
                }
            }
            for (int[] f : floors) {
                int cx = f[0];
                int cz = f[1];
                int level = r.level(cx, cz);
                int height = outer[(cz - r.minZ()) * w + (cx - r.minX())] ? BANK_HEIGHT : WALL_HEIGHT;
                for (int ox = -WALL; ox <= WALL; ox++) {
                    for (int oz = -WALL; oz <= WALL; oz++) {
                        int nx = cx + ox;
                        int nz = cz + oz;
                        if (!r.has(nx, nz) || r.isFloor(nx, nz)) {
                            continue;
                        }
                        if (r.kind(nx, nz) == IceTrack.WALL) {
                            // The higher base and the higher bank win.
                            int oldLevel = r.level(nx, nz);
                            int oldHeight = r.wallHeight(nx, nz);
                            r.set(nx, nz, IceTrack.WALL, Math.max(level, oldLevel), Math.max(height, oldHeight));
                        } else {
                            r.set(nx, nz, IceTrack.WALL, level, height);
                        }
                    }
                }
            }
            for (int[] hole : holes) {
                r.set(hole[0], hole[1], IceTrack.VOID, 0, 0);
            }
            // The features.
            for (Feature f : features) {
                for (int cx = f.x0(); cx <= f.x1(); cx++) {
                    for (int cz = f.z0(); cz <= f.z1(); cz++) {
                        if (!r.isFloor(cx, cz)) {
                            continue;
                        }
                        switch (f.kind()) {
                            case SNOW -> r.setKind(cx, cz, IceTrack.SNOW);
                            case BLUE_ICE -> r.setKind(cx, cz, IceTrack.BLUE);
                            case SNOW_WALL -> r.set(cx, cz, IceTrack.OBSTACLE, r.level(cx, cz), IceTrack.OBSTACLE_HEIGHT);
                            case LINE -> r.setKind(cx, cz, ((cx + cz) & 1) == 0 ? IceTrack.BLUE : IceTrack.PACKED);
                        }
                    }
                }
            }
            return r;
        }

        int headingOf(Sample sm) {
            for (int i = 0; i < 4; i++) {
                if (DX[i] == Math.round(sm.dx()) && DZ[i] == Math.round(sm.dz())) {
                    return i;
                }
            }
            return 0;
        }
    }

    private IceTrackGenerator() {
    }
}
