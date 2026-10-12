package com.kolodkingal.enhanced.ability;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * The arithmetic of Spider-Man's movement, with no world and no client in it, so that the client (which moves the hero
 * himself), the server (which checks him) and the game tests run the same code: his speed factor, the speed profile of
 * a zip, the stand point of a zip, the pendulum of a swing and the directions of the web's strands.
 */
public final class SpiderMath {
    /** The walking speed of a player with no bonus (the attribute's base). */
    public static final double BASE_SPEED = 0.1;
    /** What sprinting multiplies the movement-speed attribute by (vanilla's +30 percent modifier). */
    public static final double SPRINT_MULTIPLIER = 1.3;
    /** The most the speed factor reaches, so that a modded speed boost cannot turn a zip into a teleport. */
    public static final double MAX_SPEED_FACTOR = 4.0;
    /** How much faster the Iron Spider's legs make the crawl (they climb too). */
    public static final double LEGS_CRAWL_FACTOR = 1.25;
    /** The golden angle in radians, the step of a Fibonacci sphere. */
    private static final double GOLDEN_ANGLE = 2.399963229728653;

    // ---- the speed factor ----

    /**
     * How fast he is, against a player with no bonus: the movement-speed attribute with the sprint modifier divided out
     * (so that starting to sprint does not change a crawl, a zip or a swing), over the base speed, from 0 to 4. A plain
     * Spider-Man is 1.15, 1.84 with the Black Eyes; Slowness, Quicksilver's zone, Billy's Spellbound (zero), the Webbed
     * effect and Loki's Scepter all lower it, which is why they slow his crawl, zip and swing too.
     */
    public static double speedFactor(double movementSpeed, boolean sprinting) {
        double plain = sprinting ? movementSpeed / SPRINT_MULTIPLIER : movementSpeed;
        return Mth.clamp(plain / BASE_SPEED, 0.0, MAX_SPEED_FACTOR);
    }

    /** {@link #speedFactor(double, boolean)} of a player's own attribute. */
    public static double speedFactor(Player player) {
        return speedFactor(player.getAttributeValue(Attributes.MOVEMENT_SPEED), player.isSprinting());
    }

    /** His speed on a wall or ceiling in blocks per tick: the crawl speed of the config, scaled by his factor. */
    public static double crawlSpeed(double configSpeed, double speedFactor, boolean legsOut) {
        return configSpeed * speedFactor * (legsOut ? LEGS_CRAWL_FACTOR : 1.0);
    }

    // ---- the zip ----

    /**
     * His speed on tick {@code tick} of a zip, {@code distance} blocks from his stand point: the smallest of the top
     * speed (the config's, scaled by the square root of his speed factor), a ramp that rises 0.1 a tick from 0.3, and a
     * brake that leaves 0.2 plus 0.12 a block of speed to cover what is left.
     */
    public static double zipSpeed(int tick, double distance, double topSpeed, double speedFactor) {
        double top = topSpeed * Math.sqrt(speedFactor);
        return Math.min(top, Math.min(0.3 + 0.1 * tick, 0.2 + 0.12 * distance));
    }

    /**
     * Where his body ends up when a zip's web is anchored at {@code anchor} on a face looking along {@code face}: the
     * position of his feet. On a wall he hangs 0.35 out from the face with the anchor at chest height, under a ceiling
     * his head is against it, and on top of a block he stands on it. The first of a few offsets where his body fits
     * ({@code fits} says whether it does) is taken, so a block in the way chooses the next offset; null if none fits.
     *
     * @param face the side of the block that was hit (the way the face looks: up for a floor, down for a ceiling)
     */
    @Nullable
    public static Vec3 standAt(Predicate<Vec3> fits, Vec3 anchor, Direction face, double height) {
        if (face == Direction.UP) {
            return firstThatFits(fits, anchor.add(0, 0.001, 0), anchor.add(0, 0.001, 0).add(0.3, 0, 0), anchor.add(0, 0.001, 0).add(-0.3, 0, 0),
                    anchor.add(0, 0.001, 0).add(0, 0, 0.3), anchor.add(0, 0.001, 0).add(0, 0, -0.3));
        }
        if (face == Direction.DOWN) {
            // Hanging: the top of his head a hair under the ceiling, then lower, then to the side.
            Vec3 hang = anchor.add(0, -height - 0.01, 0);
            return firstThatFits(fits, hang, hang.add(0, -0.25, 0), hang.add(0.3, 0, 0), hang.add(-0.3, 0, 0), hang.add(0, 0, 0.3),
                    hang.add(0, 0, -0.3), hang.add(0, -0.5, 0));
        }
        Vec3 out = new Vec3(face.getStepX(), 0, face.getStepZ());
        double chest = 1.2;
        Vec3[] candidates = new Vec3[OUT.length * RAISE.length];
        int n = 0;
        for (double away : OUT) {
            for (double raise : RAISE) {
                candidates[n++] = anchor.add(out.scale(away)).add(0, -chest + raise, 0);
            }
        }
        return firstThatFits(fits, candidates);
    }

    /** How far out from a wall's face he is put, in the order tried. */
    private static final double[] OUT = {0.35, 0.5, 0.75, 1.0};
    /** How much higher than chest height his feet are put, in the order tried (a floor in the way raises him). */
    private static final double[] RAISE = {0.0, 0.4, -0.5, 0.8};

    @Nullable
    private static Vec3 firstThatFits(Predicate<Vec3> fits, Vec3... candidates) {
        for (Vec3 candidate : candidates) {
            if (fits.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    // ---- the swing ----

    /** Where a swinging body is and how fast it moves, after one tick. */
    public record Swing(Vec3 position, Vec3 velocity) {
    }

    /**
     * One tick of the pendulum on a taut web: gravity and the hero's air control are added to the velocity, drag slows
     * it, it is capped at {@code maxSpeed}, and the body is pulled back onto the sphere of radius {@code rope} about the
     * anchor when it would go past it, so the speed along the web is lost and the speed across it is kept.
     *
     * @param position   the centre of his body
     * @param airControl the push of his keys this tick, in blocks per tick (0.02 at most)
     * @param gravity    blocks per tick squared (the config's swingGravity)
     * @param drag       the share of the speed kept each tick (0.992)
     */
    public static Swing swingStep(Vec3 position, Vec3 velocity, Vec3 anchor, double rope, Vec3 airControl, double gravity, double drag,
                                  double maxSpeed) {
        Vec3 next = velocity.add(0, -gravity, 0).add(airControl).scale(drag);
        double speed = next.length();
        if (speed > maxSpeed) {
            next = next.scale(maxSpeed / speed);
        }
        Vec3 candidate = position.add(next);
        Vec3 away = candidate.subtract(anchor);
        double length = away.length();
        if (length > rope) {
            candidate = anchor.add(away.scale(rope / length));
        }
        return new Swing(candidate, candidate.subtract(position));
    }

    /** The speed he keeps when he lets go of a swing: his velocity, a little more if he jumped, never past the cap. */
    public static Vec3 releaseVelocity(Vec3 velocity, boolean jumped, double maxSpeed) {
        Vec3 kept = jumped ? velocity.scale(1.12).add(0, 0.12, 0) : velocity;
        double speed = kept.length();
        return speed > maxSpeed ? kept.scale(maxSpeed / speed) : kept;
    }

    // ---- the strands of the burst ----

    /**
     * {@code count} directions spread evenly over a sphere (a Fibonacci sphere): the i-th has height {@code 1 - 2(i +
     * 0.5) / n} and turns by the golden angle, and the whole sphere is turned about the vertical by an angle taken from
     * {@code seed}, so that two bursts do not leave the same pattern. Unit vectors.
     */
    public static Vec3[] fibonacciSphere(int count, long seed) {
        Vec3[] directions = new Vec3[count];
        double turn = (seed & 0xFFFFL) / 65536.0 * Mth.TWO_PI;
        for (int i = 0; i < count; i++) {
            double y = 1.0 - 2.0 * (i + 0.5) / count;
            double ring = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double azimuth = i * GOLDEN_ANGLE + turn;
            directions[i] = new Vec3(Math.cos(azimuth) * ring, y, Math.sin(azimuth) * ring);
        }
        return directions;
    }

    private SpiderMath() {
    }
}
