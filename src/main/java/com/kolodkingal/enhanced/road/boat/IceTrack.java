package com.kolodkingal.enhanced.road.boat;

import java.util.List;

/**
 * A course of the Ice-Boat Race as plain data (no Minecraft types, so it can be made and tested anywhere): the line it runs along, the
 * pieces it is made of, and the grid of blocks it comes to ({@link Raster}). All coordinates are in blocks from the middle of the
 * trial's cell; {@code level} is a number of blocks below the start (0, -1...); a floor block of level L is the block at height L
 * and the boat rides on its top, at L + 1. {@link IceTrackGenerator} makes it; {@code IceBoatChallenge} draws it.
 */
public final class IceTrack {
    /** What a piece of the course is. */
    public enum Kind {
        /** A straight (or the first and last stretch). */
        STRAIGHT,
        /** A quarter turn. */
        TURN,
        /** Two quarter turns the other way round: the line moves sideways and goes on the same way. */
        S_BEND,
        /** Two quarter turns the same way: the course goes back the way it came. */
        HAIRPIN,
        /** A straight that narrows. */
        NARROW,
        /** A straight with a rough strip of snow across all of it, to slow the boat before a hairpin. */
        BRAKE,
        /** A straight with drifts of snow on one side. */
        DRIFTS,
        /** A straight with snow walls the boat has to wind between. */
        CHICANE,
        /** A straight with a stripe of blue ice. */
        BLUE_LANE,
        /** The fork of a shortcut: the long way round starts here and a straight with a gap goes across. */
        FORK,
        /** The last straight, with the finish line. */
        FINISH
    }

    /** Which line a piece or sample belongs to. */
    public enum Route {
        /** The way round (what the length is measured along). */
        MAIN,
        /** The shortcut across a loop: a narrow straight with a gap in it. */
        CHORD
    }

    /** What a feature on the floor is. */
    public enum FeatureKind {
        /** Snow blocks on the floor: friction 0.6, slows the boat. */
        SNOW,
        /** Blue ice on the floor. */
        BLUE_ICE,
        /** A wall of snow blocks, two high, standing on the floor. */
        SNOW_WALL,
        /** A line across the track (a checkered stripe): start or finish. */
        LINE
    }

    /** What a block column of the grid is. */
    public static final byte VOID = 0;
    public static final byte PACKED = 1;
    public static final byte BLUE = 2;
    public static final byte SNOW = 3;
    public static final byte OBSTACLE = 4;
    public static final byte WALL = 5;

    /** The height of a snow wall obstacle above the floor. */
    public static final int OBSTACLE_HEIGHT = 2;

    /**
     * A point of a line the track runs along.
     *
     * @param x     where (blocks from the middle of the cell, the middle of a block is .5)
     * @param z     where
     * @param dx    the direction of the line here (unit)
     * @param dz    the direction of the line here
     * @param hw    half the width of the floor here
     * @param level the level of the floor here
     * @param s     the length along the line from its first point
     * @param turn  +1 in a right turn, -1 in a left turn, 0 on a straight
     * @param radius the radius of the curve here (0 on a straight)
     */
    public record Sample(double x, double z, double dx, double dz, double hw, int level, double s, int turn, double radius) {
    }

    /** A line: the course, or the shortcut. */
    public record Ribbon(Route route, List<Sample> samples) {
    }

    /**
     * A piece of the course.
     *
     * @param kind    what it is
     * @param group   the number of the group of pieces it belongs to (a hairpin is one group)
     * @param s0      where it starts along the main line (start of the platform = 0)
     * @param s1      where it ends
     * @param radius  the radius of its curves (0 for a straight)
     * @param route   which line
     * @param heading the way a straight faces: 0 = +x, 1 = +z, 2 = -x, 3 = -z (a curve: the way it faced when it began)
     * @param turn    +1 right, -1 left, 0 straight
     */
    public record Piece(Kind kind, int group, double s0, double s1, double radius, Route route, int heading, int turn) {
        public double length() {
            return s1 - s0;
        }
    }

    /** A rectangle of the grid (cell indices, both ends included) with something on it. */
    public record Feature(FeatureKind kind, int x0, int z0, int x1, int z1, int level) {
        public boolean contains(int x, int z) {
            return x >= x0 && x <= x1 && z >= z0 && z <= z1;
        }
    }

    /**
     * A checkpoint: where the boat is put when the player is sent back, and the zone that makes it his checkpoint.
     *
     * @param x       the middle of the track here
     * @param z       the middle of the track here
     * @param heading the way the course goes (0 = +x, 1 = +z, 2 = -x, 3 = -z)
     * @param hw      half the width of the floor
     * @param level   the level
     * @param s       the length along the main line
     */
    public record Checkpoint(double x, double z, int heading, double hw, int level, double s) {
    }

    /**
     * The shortcut of a loop: the place where the long way and the short way part, the gap in the short way, and where they meet.
     *
     * @param forkX   where the two ways part
     * @param forkZ   where
     * @param heading the direction both go in
     * @param gapFrom the first cell of the gap, as the distance along the short way from the fork
     * @param gap     the cells of air in the gap
     * @param drop    how much lower the short way is after the gap (blocks)
     * @param run     the length of the straight before the gap (blocks)
     * @param saving  how many blocks shorter the short way is than the long way
     */
    public record Loop(double forkX, double forkZ, int heading, int gapFrom, int gap, int drop, int run, double saving) {
    }

    /**
     * The blocks of the course on a grid of columns.
     *
     * <p>A column is {@code VOID}, a floor ({@code PACKED}, {@code BLUE}, {@code SNOW}), a snow wall standing on a floor
     * ({@code OBSTACLE}) or a wall of the track ({@code WALL}). Its {@link #level(int, int)} is the level of the floor (of the floor
     * next to it, for a wall) and {@link #wallHeight(int, int)} the number of blocks of a wall or an obstacle above the floor.
     */
    public static final class Raster {
        private final int minX;
        private final int minZ;
        private final int width;
        private final int depth;
        private final byte[] kind;
        private final byte[] level;
        private final byte[] wall;

        public Raster(int minX, int minZ, int width, int depth) {
            this.minX = minX;
            this.minZ = minZ;
            this.width = width;
            this.depth = depth;
            this.kind = new byte[width * depth];
            this.level = new byte[width * depth];
            this.wall = new byte[width * depth];
        }

        public int minX() {
            return minX;
        }

        public int minZ() {
            return minZ;
        }

        public int maxX() {
            return minX + width - 1;
        }

        public int maxZ() {
            return minZ + depth - 1;
        }

        public boolean has(int x, int z) {
            return x >= minX && x < minX + width && z >= minZ && z < minZ + depth;
        }

        private int index(int x, int z) {
            return (z - minZ) * width + (x - minX);
        }

        /** What is in the column (VOID outside the grid). */
        public byte kind(int x, int z) {
            return has(x, z) ? kind[index(x, z)] : VOID;
        }

        /** The level of the floor of the column (0 or below). */
        public int level(int x, int z) {
            return has(x, z) ? -level[index(x, z)] : 0;
        }

        /** How many blocks a wall or an obstacle rises above the floor. */
        public int wallHeight(int x, int z) {
            return has(x, z) ? wall[index(x, z)] : 0;
        }

        public boolean isFloor(int x, int z) {
            byte k = kind(x, z);
            return k == PACKED || k == BLUE || k == SNOW;
        }

        /** Whether a boat can drive on it or has to go round it: a floor or an obstacle's foot counts as a floor for the walls. */
        public boolean isSolid(int x, int z) {
            byte k = kind(x, z);
            return k == WALL || k == OBSTACLE;
        }

        public void set(int x, int z, byte kind, int level, int wallHeight) {
            int i = index(x, z);
            this.kind[i] = kind;
            this.level[i] = (byte) -level;
            this.wall[i] = (byte) wallHeight;
        }

        public void setKind(int x, int z, byte kind) {
            this.kind[index(x, z)] = kind;
        }

        public void setWall(int x, int z, int wallHeight) {
            this.wall[index(x, z)] = (byte) wallHeight;
        }

        /** The top of the column as a boat meets it: {@code Double.NEGATIVE_INFINITY} where there is nothing. */
        public double top(int x, int z) {
            byte k = kind(x, z);
            return switch (k) {
                case VOID -> Double.NEGATIVE_INFINITY;
                case WALL, OBSTACLE -> level(x, z) + 1.0 + wallHeight(x, z);
                default -> level(x, z) + 1.0;
            };
        }

        /** How many columns are not void. */
        public int count() {
            int n = 0;
            for (byte b : kind) {
                if (b != VOID) {
                    n++;
                }
            }
            return n;
        }
    }

    // ---- the data ----
    private final long seed;
    private final IceTrackGenerator.Params params;
    private final List<Ribbon> ribbons;
    private final List<Piece> pieces;
    private final List<Feature> features;
    private final List<Checkpoint> checkpoints;
    private final List<Loop> loops;
    private final Raster raster;
    private final int heading;
    private final int finishHeading;
    private final double platformX;
    private final double platformZ;
    private final double startLine;
    private final double finishLine;
    private final double finishX;
    private final double finishZ;
    private final double length;
    private final double shortest;
    private final int lowestLevel;

    IceTrack(long seed, IceTrackGenerator.Params params, List<Ribbon> ribbons, List<Piece> pieces, List<Feature> features,
             List<Checkpoint> checkpoints, List<Loop> loops, Raster raster, int heading, int finishHeading, double platformX, double platformZ,
             double startLine, double finishLine, double finishX, double finishZ, double length, double shortest, int lowestLevel) {
        this.seed = seed;
        this.params = params;
        this.ribbons = ribbons;
        this.pieces = pieces;
        this.features = features;
        this.checkpoints = checkpoints;
        this.loops = loops;
        this.raster = raster;
        this.heading = heading;
        this.finishHeading = finishHeading;
        this.platformX = platformX;
        this.platformZ = platformZ;
        this.startLine = startLine;
        this.finishLine = finishLine;
        this.finishX = finishX;
        this.finishZ = finishZ;
        this.length = length;
        this.shortest = shortest;
        this.lowestLevel = lowestLevel;
    }

    public long seed() {
        return seed;
    }

    public IceTrackGenerator.Params params() {
        return params;
    }

    public List<Ribbon> ribbons() {
        return ribbons;
    }

    public List<Piece> pieces() {
        return pieces;
    }

    public List<Feature> features() {
        return features;
    }

    /** The checkpoints in the order they are reached (the start is not one of them). */
    public List<Checkpoint> checkpoints() {
        return checkpoints;
    }

    public List<Loop> loops() {
        return loops;
    }

    public Raster raster() {
        return raster;
    }

    /** The way the course leaves the start: 0 = +x, 1 = +z, 2 = -x, 3 = -z. */
    public int heading() {
        return heading;
    }

    /** The way the last straight faces. */
    public int finishHeading() {
        return finishHeading;
    }

    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DZ = {0, 1, 0, -1};

    public static int dirX(int heading) {
        return DX[Math.floorMod(heading, 4)];
    }

    public static int dirZ(int heading) {
        return DZ[Math.floorMod(heading, 4)];
    }

    /** The middle of the start platform. */
    public double platformX() {
        return platformX;
    }

    public double platformZ() {
        return platformZ;
    }

    /** The point on the main line {@code along} blocks from the middle of the start platform, straight ahead (the first stretch is straight). */
    public double startAlongX(double along) {
        return platformX + dirX(heading) * along;
    }

    public double startAlongZ(double along) {
        return platformZ + dirZ(heading) * along;
    }

    /** Where the main line's length counts from, as a distance from the platform's middle, and where the finish line is. */
    public double startLine() {
        return startLine;
    }

    /** The length along the main line (from the sample that begins the line) at which the finish line is. */
    public double finishLine() {
        return finishLine;
    }

    /** The middle of the finish platform. */
    public double finishX() {
        return finishX;
    }

    public double finishZ() {
        return finishZ;
    }

    /** The length of the main line from the start line to the finish line, in blocks. */
    public double length() {
        return length;
    }

    /** The length of the shortest way, taking every shortcut. */
    public double shortest() {
        return shortest;
    }

    /** The lowest level the course reaches (the way after the last shortcut). */
    public int lowestLevel() {
        return lowestLevel;
    }

    /** The main line. */
    public Ribbon main() {
        return ribbons.get(0);
    }

    /**
     * The zone that wins the race: across the track at the finish line, {@code [minX, minZ, maxX, maxZ]} (a boat whose middle is in it
     * has crossed the line; it is deep enough for the fastest a boat can go, 3.6 blocks a tick).
     */
    public double[] finishZone() {
        int fx = dirX(finishHeading);
        int fz = dirZ(finishHeading);
        // The middle of the finish platform is 4 blocks past the end of the last straight, the line 14 blocks before the end.
        double endX = finishX - fx * 4.0;
        double endZ = finishZ - fz * 4.0;
        double hw = params.halfWidth() + 1.0;
        double a0 = -IceTrackGenerator.FINISH_LINE_BACK - 0.5;
        double a1 = -IceTrackGenerator.FINISH_LINE_BACK + 8.0;
        double x0 = endX + fx * a0 + (-fz) * -hw;
        double z0 = endZ + fz * a0 + fx * -hw;
        double x1 = endX + fx * a1 + (-fz) * hw;
        double z1 = endZ + fz * a1 + fx * hw;
        return new double[]{Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)};
    }

    /**
     * The time a good driver needs from the start to the finish line, in ticks, worked out from the boat's physics: the fastest
     * the boat can pass the main line ({@link IceBoatModel#speedProfile}: the top speed on packed ice, the cornering speed in every
     * curve, slowed and speeded by the friction and the thrust), and a little for each snow strip, snow wall and drop.
     */
    public double parTicks() {
        List<Sample> samples = main().samples();
        int from = 0;
        int to = samples.size() - 1;
        for (int i = 0; i < samples.size(); i++) {
            double s = samples.get(i).s();
            if (s < IceTrackGenerator.PLATFORM_BACK + IceTrackGenerator.BOAT_PARK) {
                from = i;
            }
            if (s <= finishLine) {
                to = i;
            }
        }
        int n = to - from + 1;
        double[] cap = new double[n];
        double[] ds = new double[n];
        double top = IceBoatModel.terminalSpeed(IceBoatModel.PACKED_ICE_FRICTION);
        for (int i = 0; i < n; i++) {
            Sample sm = samples.get(from + i);
            double limit = top;
            if (sm.turn() != 0) {
                limit = Math.min(limit, IceBoatModel.cornerSpeed(sm.radius()));
            }
            if (2.0 * sm.hw() < params.width() - 1.0) {
                limit = Math.min(limit, 1.5);
            }
            cap[i] = limit;
            if (i > 0) {
                Sample prev = samples.get(from + i - 1);
                ds[i] = Math.hypot(sm.x() - prev.x(), sm.z() - prev.z());
            }
        }
        double[] v = IceBoatModel.speedProfile(cap, ds, IceBoatModel.PACKED_ICE_FRICTION, 0.0);
        double ticks = IceBoatModel.profileTicks(v, ds);
        for (Feature f : features) {
            switch (f.kind()) {
                case SNOW -> ticks += (f.x1() - f.x0() + 1) * (f.z1() - f.z0() + 1) >= 40 ? 25.0 : 8.0;
                case SNOW_WALL -> ticks += 10.0;
                default -> {
                }
            }
        }
        ticks += 15.0 * loops.size();
        return ticks;
    }

    /** The narrowest floor anywhere on the main line (blocks across). */
    public double narrowest() {
        double min = Double.MAX_VALUE;
        for (Ribbon ribbon : ribbons) {
            for (Sample sample : ribbon.samples()) {
                min = Math.min(min, 2.0 * sample.hw());
            }
        }
        return min;
    }
}
