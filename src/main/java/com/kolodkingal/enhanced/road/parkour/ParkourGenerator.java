package com.kolodkingal.enhanced.road.parkour;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Makes a course of jumps over the void: pure and deterministic (the same seed and settings give the same course), with no
 * knowledge of the game but {@link JumpModel}. The course starts on a platform, winds across the void in jumps that get harder
 * (wider gaps, smaller stones, steps up and down), has a checkpoint platform every few jumps, and ends on a finish
 * platform. Every jump is checked before it is accepted: no gap is wider than the physics allow with a margin
 * ({@link JumpModel#safeReach}) or than the difficulty's cap, no stone comes near an older one, and the whole course stays
 * inside {@link #BOUND} blocks of the start.
 *
 * <p>Coordinates are offsets from the start platform's middle; {@code y} is the height of the top block of a pad.
 */
public final class ParkourGenerator {
    /** The course stays within this many blocks of the start in x and z (a cell reaches 450). */
    public static final int BOUND = 380;
    /** The course stays within this many blocks above and below the start. */
    public static final int HEIGHT_BOUND = 28;
    /** The sides of the special platforms. */
    public static final int START_SIZE = 7;
    public static final int CHECKPOINT_SIZE = 5;
    public static final int FINISH_SIZE = 9;
    /** How much longer the legs of each ring of the spiral are than those of the ring inside. */
    static final int RING = 24;
    /** Stones keep this many blocks (horizontally) from every older one but the one they follow, unless a floor apart. */
    private static final int CLEARANCE = 3;
    private static final int MAX_TRIES = 60;

    /** What kind of pad it is. */
    public enum Kind {
        START, STONE, CHECKPOINT, FINISH
    }

    /** A square pad: its corners are blocks {@code minX..maxX, minZ..maxZ}; {@code y} is its top block. */
    public record Pad(int index, Kind kind, int minX, int minZ, int maxX, int maxZ, int y) {
        public int size() {
            return maxX - minX + 1;
        }

        public double centerX() {
            return (minX + maxX + 1) / 2.0;
        }

        public double centerZ() {
            return (minZ + maxZ + 1) / 2.0;
        }

        /** The block coordinate of the middle (the platforms are odd, a 2x2 stone has no middle block: it takes the lower one). */
        public int midX() {
            return (minX + maxX) / 2;
        }

        public int midZ() {
            return (minZ + maxZ) / 2;
        }
    }

    /** The settings of a course. */
    public record Params(int jumps, int difficulty, int checkpointEvery) {
    }

    /**
     * A course.
     *
     * @param pads        start first, finish last
     * @param checkpoints the indices (into {@code pads}) of the checkpoint platforms, in order
     */
    public record Plan(List<Pad> pads, List<Integer> checkpoints) {
        public Pad start() {
            return pads.get(0);
        }

        public Pad finish() {
            return pads.get(pads.size() - 1);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The jumps

    /** The air between two pads, along x and along z (0 where they overlap in that direction), in blocks. */
    public static double[] air(Pad a, Pad b) {
        int dx = Math.max(0, Math.max(b.minX() - a.maxX() - 1, a.minX() - b.maxX() - 1));
        int dz = Math.max(0, Math.max(b.minZ() - a.maxZ() - 1, a.minZ() - b.maxZ() - 1));
        return new double[]{dx, dz};
    }

    /** The straight distance across the air between two pads' edges. */
    public static double gap(Pad a, Pad b) {
        double[] air = air(a, b);
        return Math.hypot(air[0], air[1]);
    }

    /** Whether a player can jump from {@code a} to {@code b} (with the margin): a real jump, not too wide, not too steep. */
    public static boolean reachable(Pad a, Pad b) {
        int dy = b.y() - a.y();
        if (dy < -1 || dy > 1) {
            return false;
        }
        double gap = gap(a, b);
        return gap >= 1.0 && gap <= JumpModel.safeReach(dy);
    }

    /** The widest gap (blocks) the difficulty asks for this far into the course (0 at the start, 1 at the finish). */
    static int gapCap(int difficulty, double progress) {
        return switch (difficulty) {
            case 1 -> progress > 0.55 ? 2 : 1;
            case 3 -> progress > 0.35 ? 3 : 2;
            default -> 1 + (progress > 0.2 ? 1 : 0) + (progress > 0.6 ? 1 : 0);
        };
    }

    /** The side of a stone this far into the course. */
    static int stoneSize(int difficulty, double progress, SplittableRandom random) {
        return switch (difficulty) {
            case 1 -> progress > 0.6 ? 2 + random.nextInt(2) : 3;
            case 3 -> progress < 0.4 ? 2 : random.nextInt(3) == 0 ? 2 : 1;
            default -> progress < 0.25 ? 3 : progress < 0.7 ? 2 + random.nextInt(2) : random.nextInt(3) == 0 ? 1 : 2;
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // The course

    /**
     * The course winds outward in a square spiral round the start, which is what keeps it from ever running into itself: it goes
     * along a heading for a while, turns the same way (a quarter turn), and every two legs the legs are {@link #RING} blocks longer.
     * The jumps along a leg are random in length, size and height, and the stones wander a little from side to side.
     */
    public static Plan generate(long seed, Params params) {
        SplittableRandom random = new SplittableRandom(seed);
        int jumps = params.jumps();
        List<Pad> pads = new ArrayList<>();
        List<Integer> checkpoints = new ArrayList<>();
        int half = START_SIZE / 2;
        pads.add(new Pad(0, Kind.START, -half, -half, half, half, 0));
        int heading = random.nextInt(4); // 0 = +x, 1 = +z, 2 = -x, 3 = -z
        int turn = random.nextBoolean() ? 1 : 3;
        int leg = 0;
        Pad legStart = pads.get(0);
        int lane = 0; // how far the stones have wandered from the leg's straight line
        for (int i = 1; i <= jumps; i++) {
            double progress = jumps <= 1 ? 1.0 : (i - 1) / (double) (jumps - 1);
            Kind kind = i == jumps ? Kind.FINISH : i % params.checkpointEvery() == 0 ? Kind.CHECKPOINT : Kind.STONE;
            Pad from = pads.get(pads.size() - 1);
            if (traveled(legStart, from, heading) >= legLength(leg)) {
                heading = (heading + turn) % 4;
                leg++;
                legStart = from;
                lane = 0;
            }
            Candidate chosen = null;
            for (int tries = 0; tries < MAX_TRIES && chosen == null; tries++) {
                chosen = candidate(random, params, pads, from, heading, lane, progress, i, kind, tries);
            }
            if (chosen == null) {
                chosen = fallback(pads, from, heading, i, kind);
                if (chosen.heading != heading) {
                    heading = chosen.heading;
                    leg++;
                    legStart = from;
                    lane = 0;
                }
            }
            lane += chosen.lateral;
            pads.add(chosen.pad);
            if (kind == Kind.CHECKPOINT) {
                checkpoints.add(pads.size() - 1);
            }
        }
        return new Plan(List.copyOf(pads), List.copyOf(checkpoints));
    }

    /** The length (blocks) of a leg of the spiral: two of each, every pair {@link #RING} longer than the one before. */
    static int legLength(int leg) {
        return 12 + RING * (leg / 2);
    }

    /** How far along the heading the pad has come from the start of the leg (middle to middle). */
    private static double traveled(Pad legStart, Pad pad, int heading) {
        return switch (heading) {
            case 0 -> pad.centerX() - legStart.centerX();
            case 1 -> pad.centerZ() - legStart.centerZ();
            case 2 -> legStart.centerX() - pad.centerX();
            default -> legStart.centerZ() - pad.centerZ();
        };
    }

    private record Candidate(Pad pad, int heading, int lateral) {
    }

    private static Candidate candidate(SplittableRandom random, Params params, List<Pad> pads, Pad from, int heading, int lane,
                                       double progress, int index, Kind kind, int tries) {
        int difficulty = params.difficulty();
        int cap = gapCap(difficulty, progress);
        int minGap = difficulty == 3 ? 2 : 1;
        int gap = minGap + random.nextInt(cap - minGap + 1);
        int size = switch (kind) {
            case FINISH -> FINISH_SIZE;
            case CHECKPOINT -> CHECKPOINT_SIZE;
            default -> stoneSize(difficulty, progress, random);
        };
        int dy = 0;
        double stepChance = difficulty == 1 ? (progress > 0.5 ? 0.2 : 0.0) : 0.15 + 0.08 * difficulty;
        if (kind == Kind.STONE && random.nextDouble() < stepChance) {
            dy = random.nextBoolean() ? 1 : -1;
        }
        // The course climbs and falls within bounds.
        if (from.y() + dy > HEIGHT_BOUND || from.y() + dy < -HEIGHT_BOUND) {
            dy = -dy;
        }
        // The stones wander from side to side, but never far from the straight line of the leg.
        int lateralMax = difficulty == 1 ? 1 : 2;
        int lateral = 0;
        if (kind == Kind.STONE) {
            int target = random.nextInt(2 * lateralMax + 1) - lateralMax;
            lateral = target - lane;
        }
        Pad next = place(from, heading, gap, lateral, size, dy, index, kind);
        // A sideways shift makes the jump a diagonal, and a diagonal is longer: it must not be longer than the difficulty's cap.
        return accept(pads, from, next, cap) ? new Candidate(next, heading, lateral) : null;
    }

    /** The straight, easy jump that always works if anything does: tries every heading with a gap of one. */
    private static Candidate fallback(List<Pad> pads, Pad from, int heading, int index, Kind kind) {
        int size = switch (kind) {
            case FINISH -> FINISH_SIZE;
            case CHECKPOINT -> CHECKPOINT_SIZE;
            default -> 3;
        };
        // As much room as there is, then less and less (a short course with big platforms is crowded near its start).
        for (int clearance = CLEARANCE; clearance >= 0; clearance--) {
            for (int turn = 0; turn < 4; turn++) {
                for (int gap = 1; gap <= 2; gap++) {
                    for (int lateral : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                        for (int dy : new int[]{0, -1}) {
                            int h = (heading + turn) % 4;
                            Pad next = place(from, h, gap, lateral, size, from.y() + dy < -HEIGHT_BOUND ? 0 : dy, index, kind);
                            if (accept(pads, from, next, 2, clearance)) {
                                return new Candidate(next, h, h == heading ? lateral : 0);
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("A parkour course boxed itself in at jump " + index);
    }

    /** The pad {@code gap} blocks of air beyond {@code from} along the heading, shifted sideways, of the given size. */
    static Pad place(Pad from, int heading, int gap, int lateral, int size, int dy, int index, Kind kind) {
        int minX;
        int minZ;
        // The middle of the new pad's far side runs along the old pad's middle, shifted by the lateral offset.
        int sideShiftX = heading % 2 == 0 ? 0 : lateral;
        int sideShiftZ = heading % 2 == 0 ? lateral : 0;
        int midX = from.midX() + sideShiftX;
        int midZ = from.midZ() + sideShiftZ;
        int lowOffset = (size - 1) / 2;
        switch (heading) {
            case 0 -> {
                minX = from.maxX() + 1 + gap;
                minZ = midZ - lowOffset;
            }
            case 1 -> {
                minZ = from.maxZ() + 1 + gap;
                minX = midX - lowOffset;
            }
            case 2 -> {
                minX = from.minX() - gap - size;
                minZ = midZ - lowOffset;
            }
            default -> {
                minZ = from.minZ() - gap - size;
                minX = midX - lowOffset;
            }
        }
        return new Pad(index, kind, minX, minZ, minX + size - 1, minZ + size - 1, from.y() + dy);
    }

    /** Whether the jump works and the pad is clear of the course so far and inside its bounds. */
    private static boolean accept(List<Pad> pads, Pad from, Pad next, int cap) {
        return accept(pads, from, next, cap, CLEARANCE);
    }

    private static boolean accept(List<Pad> pads, Pad from, Pad next, int cap, int clearance) {
        if (!reachable(from, next) || gap(from, next) > cap + 1.0E-9) {
            return false;
        }
        if (Math.abs(next.minX()) > BOUND || Math.abs(next.maxX()) > BOUND || Math.abs(next.minZ()) > BOUND || Math.abs(next.maxZ()) > BOUND) {
            return false;
        }
        if (Math.abs(next.y()) > HEIGHT_BOUND) {
            return false;
        }
        for (Pad old : pads) {
            if (old == from) {
                continue;
            }
            // The pad before the one he jumps from may be close (that is what a turn is); every older one keeps its distance.
            if (old.index() == from.index() - 1 ? overlaps(old, next) : near(old, next, clearance)) {
                return false;
            }
        }
        // And not on top of the one it follows.
        return !overlaps(from, next);
    }

    private static boolean overlaps(Pad a, Pad b) {
        return a.minX() <= b.maxX() && b.minX() <= a.maxX() && a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ() && Math.abs(a.y() - b.y()) < 8;
    }

    /** Too close to an older pad: within the clearance horizontally and less than a floor apart. */
    private static boolean near(Pad old, Pad next, int clearance) {
        if (Math.abs(old.y() - next.y()) >= 8) {
            return false;
        }
        return old.minX() - clearance <= next.maxX() && next.minX() - clearance <= old.maxX()
                && old.minZ() - clearance <= next.maxZ() && next.minZ() - clearance <= old.maxZ();
    }

    private ParkourGenerator() {
    }
}
