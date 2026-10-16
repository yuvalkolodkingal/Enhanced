package com.kolodkingal.enhanced.road.parkour;

/**
 * How far a player can jump in the game, worked out tick by tick with the game's own physics for a player (Java 1.21): the
 * jump gives an upward speed of 0.42, gravity takes 0.08 a tick and the air 2 % (0.98), a sprinting jump adds 0.2 forwards, the air
 * keeps 91 % of the sideways speed and a sprinting player in the air pushes forwards with 0.026 a tick, on the ground 0.13 against
 * a friction of 0.546. The numbers it gives agree with what players know: a running jump on the flat crosses a gap of up to
 * four blocks and no more.
 *
 * <p>The Parkour keeps every jump well inside these limits ({@link #safeReach}).
 */
public final class JumpModel {
    /** A player can stand with his body this far past the edge of a block (his box is 0.6 wide), and it counts the same on landing. */
    public static final double EDGE_BONUS = 0.6;
    /** The run-up (ticks of running on the ground before the jump) the Parkour counts on: the player always has the stone he stands on. */
    public static final int RUN_UP = 2;
    /** What the Parkour leaves unused of what the physics allow, in blocks. */
    public static final double MARGIN = 0.5;

    /**
     * How far the middle of a player travels sideways from take-off until his feet come down to {@code dy} blocks above the
     * take-off height (on the way down).
     *
     * @param dy      the landing height relative to the take-off height (-1, 0, 1...)
     * @param runUp   ticks of running on the ground first
     * @param sprint  whether he is sprinting
     */
    public static double travel(int dy, int runUp, boolean sprint) {
        double accel = 0.1 * (sprint ? 1.3 : 1.0);
        double v = 0.0;
        for (int i = 0; i < runUp; i++) {
            v = (v + accel) * 0.546;
        }
        if (sprint) {
            v += 0.2;
        }
        double air = sprint ? 0.026 : 0.02;
        double vy = 0.42;
        double y = 0.0;
        double x = 0.0;
        double prevX = 0.0;
        double prevY = 0.0;
        for (int tick = 0; tick < 200; tick++) {
            if (tick == 0) {
                // The jump happens at the start of the tick, but the player is still on the ground for its movement.
                v += accel;
                x += v;
                v *= 0.546;
            } else {
                v += air;
                x += v;
                v *= 0.91;
            }
            y += vy;
            vy = (vy - 0.08) * 0.98;
            if (tick > 2 && prevY >= dy && y < dy) {
                double f = (prevY - dy) / (prevY - y);
                return prevX + f * (x - prevX);
            }
            prevX = x;
            prevY = y;
        }
        throw new IllegalStateException("The jump never came down to " + dy);
    }

    /**
     * The widest gap of air (the distance between the two edges of the stones) a player can cross with a running jump, with
     * the given run-up, in blocks. 4.07 for a gap on the flat.
     */
    public static double maxGap(int dy, int runUp) {
        return travel(dy, runUp, true) + EDGE_BONUS;
    }

    /** The widest gap the Parkour ever asks of a player for a change of height {@code dy}: the physics, less a margin. */
    public static double safeReach(int dy) {
        return maxGap(dy, RUN_UP) - MARGIN;
    }

    private JumpModel() {
    }
}
