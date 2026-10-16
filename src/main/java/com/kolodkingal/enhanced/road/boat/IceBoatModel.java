package com.kolodkingal.enhanced.road.boat;

/**
 * How a boat moves on ice in Minecraft 1.21.1, worked out from the game's own {@code Boat} class (the numbers below are the ones
 * in its code), so that the Ice-Boat Race can be built to what a boat can really do. Everything here is per tick (1/20 s), speeds
 * are blocks per tick (multiply by 20 for blocks per second).
 *
 * <ul>
 * <li><b>Driving.</b> The client of the rider moves the boat; the server only checks and follows. Every tick, on land, the speed is
 *     multiplied by the <i>friction of the blocks under the boat</i> (the plain block friction: 0.98 for ice and packed ice, 0.989
 *     for blue ice, 0.6 for most blocks, not the 0.91 times it that players get) and then the boat is pushed along its heading
 *     with 0.04 (W), 0.005 (S, backwards) or 0.005 (only turning). So the top speed is {@code 0.04 / (1 - friction)}: 2.0 blocks a
 *     tick (40 blocks a second) on packed ice, 3.6 on blue ice, and only 0.1 on snow or planks. It takes about 35 ticks and 20
 *     blocks to reach half of it on packed ice.</li>
 * <li><b>Steering.</b> A or D adds or takes 1 degree to the boat's turning speed each tick, and the turning speed is multiplied
 *     by the same friction, so a turn can speed up to {@code 1 / (1 - friction)} degrees a tick (50 on packed ice) and it dies away
 *     slowly. The boat can turn as fast as anyone needs; what it cannot do is change the direction it <i>travels</i>: the only force
 *     that bends the path is the 0.04 push along the heading, so the tightest curve at speed v has the radius
 *     {@code v * v / 0.04}. A boat that comes into a curve faster than its radius allows slides on in a straight line, until a
 *     wall takes the part of the speed that goes into it (it keeps the part along the wall).</li>
 * <li><b>Air.</b> A boat with nothing under it falls with 0.04 a tick (no fall damage can kill it here) and keeps only 90 % of
 *     its speed each tick; it hangs on the edge until it has gone off it completely (its box is 1.375 wide), so a gap can be
 *     crossed by speed alone, if the far side is lower than the boat sinks while it crosses.</li>
 * <li><b>Obstacles.</b> A boat cannot climb (its step height is 0), not even a slab or a layer of snow: it stops at any wall. So
 *     the course has no ramps up, only edges down.</li>
 * </ul>
 */
public final class IceBoatModel {
    /** The push along the heading while W is held. */
    public static final double THRUST = 0.04;
    /** The push while only turning (A or D alone), and backwards (S): the game's 0.005. */
    public static final double SMALL_THRUST = 0.005;
    /** Gravity of the boat. */
    public static final double GRAVITY = 0.04;
    /** The speed that is kept each tick in the air. */
    public static final double AIR_FRICTION = 0.9;
    /** The game's friction of packed ice and ice (the course is laid in packed ice, ice melts in the light). */
    public static final double PACKED_ICE_FRICTION = 0.98;
    /** The game's friction of blue ice. */
    public static final double BLUE_ICE_FRICTION = 0.989;
    /** The game's friction of snow blocks (and of planks, stone, amethyst: the "ordinary" blocks). */
    public static final double SNOW_FRICTION = 0.6;
    /** The width of the boat's box. */
    public static final double BOAT_WIDTH = 1.375;
    /** The height of the boat's box. */
    public static final double BOAT_HEIGHT = 0.5625;
    /**
     * The part of the push that a course may count on for bending the path (the rest is left for keeping the speed): a curve of
     * radius R is meant to be driven at {@link #cornerSpeed(double)}.
     */
    public static final double CORNER_PUSH = 0.5;
    /** Where a boat that is meant to cross a gap is: the ticks the model looks ahead at most. */
    private static final int MAX_AIR_TICKS = 200;

    /** The top speed on a floor with this friction, with W held, on the flat: {@code 0.04 / (1 - friction)}. */
    public static double terminalSpeed(double friction) {
        return THRUST / (1.0 - friction);
    }

    /** The fastest the boat's turning can get (degrees a tick) on a floor with this friction. */
    public static double maxTurnRate(double friction) {
        return 1.0 / (1.0 - friction);
    }

    /** The speed after {@code ticks} of W from {@code v0} on a floor with this friction. */
    public static double speedAfter(double friction, double v0, int ticks) {
        double top = terminalSpeed(friction);
        return top - (top - v0) * Math.pow(friction, ticks);
    }

    /** The blocks a boat that coasts (no W) covers while it slows from {@code vFrom} to {@code vTo} (about {@code dv / (1 - f)}). */
    public static double coastDistance(double friction, double vFrom, double vTo) {
        return Math.max(0.0, vFrom - vTo) / (1.0 - friction);
    }

    /** The speed at which a curve of this radius can be driven with {@link #CORNER_PUSH} of the thrust: {@code sqrt(0.5 * 0.04 * R)}. */
    public static double cornerSpeed(double radius) {
        return Math.sqrt(CORNER_PUSH * THRUST * radius);
    }

    /** The radius of the tightest curve that can be driven at this speed with {@link #CORNER_PUSH} of the thrust. */
    public static double cornerRadius(double speed) {
        return speed * speed / (CORNER_PUSH * THRUST);
    }

    /**
     * How far (blocks) the middle of a boat goes sideways while it falls {@code drop} blocks, from the moment it is off the edge
     * completely, with the thrust off (a boat that keeps W held goes farther).
     *
     * @param speed the speed (blocks per tick) it leaves the edge with
     * @param drop  how far it may fall before it must be over the far side
     */
    public static double airReach(double speed, double drop) {
        double x = 0.0;
        double v = speed;
        double fall = 0.0;
        double vy = 0.0;
        for (int tick = 0; tick < MAX_AIR_TICKS; tick++) {
            v *= AIR_FRICTION;
            vy += GRAVITY;
            if (fall + vy > drop) {
                // The tick in which it would be down: the part of it that is still above.
                x += v * (drop - fall) / vy;
                return x;
            }
            fall += vy;
            x += v;
        }
        return x;
    }

    /**
     * The slowest speed at which a boat crosses a gap of {@code gap} blocks of air whose far side is {@code drop} blocks lower
     * than the near side. It is on the edge until its middle is 0.6875 past it, and has crossed when its middle is 0.6875 short of
     * the far edge, so the free flight is {@code gap - 1.375}.
     */
    public static double minJumpSpeed(double gap, double drop) {
        double need = Math.max(0.0, gap - BOAT_WIDTH);
        double lo = 0.0;
        double hi = 10.0;
        for (int i = 0; i < 60; i++) {
            double mid = 0.5 * (lo + hi);
            if (airReach(mid, drop) >= need) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return hi;
    }

    /**
     * The fastest a boat can pass a line of points: at most {@code cap[i]} at point {@code i}, no faster than it can be slowed down
     * to the cap ahead by coasting (the friction takes {@code 1 - f} of the speed a block), and no faster than the thrust can have
     * brought it up from the start. {@code ds[i]} is the distance from point {@code i - 1} to point {@code i}.
     */
    public static double[] speedProfile(double[] cap, double[] ds, double friction, double startSpeed) {
        int n = cap.length;
        double[] v = cap.clone();
        for (int i = n - 2; i >= 0; i--) {
            v[i] = Math.min(v[i], v[i + 1] + (1.0 - friction) * ds[i + 1]);
        }
        v[0] = Math.min(v[0], startSpeed);
        for (int i = 1; i < n; i++) {
            double gain = Math.max(0.0, THRUST - (1.0 - friction) * v[i - 1]);
            v[i] = Math.min(v[i], Math.sqrt(v[i - 1] * v[i - 1] + 2.0 * gain * ds[i]));
        }
        return v;
    }

    /** The ticks it takes to pass the line at the speeds of {@link #speedProfile}. */
    public static double profileTicks(double[] v, double[] ds) {
        double ticks = 0.0;
        for (int i = 1; i < v.length; i++) {
            double mean = Math.max(0.05, 0.5 * (v[i - 1] + v[i]));
            ticks += ds[i] / mean;
        }
        return ticks;
    }

    /** The speed a boat has after running {@code distance} blocks from rest on a floor with this friction with W held. */
    public static double speedAfterDistance(double friction, double distance) {
        double v = 0.0;
        double x = 0.0;
        double top = terminalSpeed(friction);
        while (x < distance && v < top) {
            v = v * friction + THRUST;
            x += v;
        }
        return v;
    }

    private IceBoatModel() {
    }
}
