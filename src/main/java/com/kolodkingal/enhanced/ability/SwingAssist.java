package com.kolodkingal.enhanced.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Web Assist (3.3): the swing made easy, the way Marvel's Spider-Man chooses where the web goes (see the lane's notes).
 * The same code runs on the hero's own client (which moves him and chooses), on the server (which checks what the client
 * chose) and in the game tests, so nothing here knows of a client.
 *
 * <ul>
 *   <li>{@link #choose}: the best anchor ahead and above him. A fan of rays round his heading finds the candidates (leaves
 *   and logs are blocks like any other); they are scored (high anchors over high ground, a steep forward angle, along
 *   his heading, a good length, near the crosshair), and the best few are flown through with the assist's own rules: a
 *   path in which his body would touch a block (a low canopy, a trunk, a hill) is not taken. In the open, a fallback:
 *   a web to the open sky, or a short boost, or nothing ({@link OpenLand}).</li>
 *   <li>{@link #reel}: the rope is drawn in whenever the bottom of the arc would come near the ground.</li>
 *   <li>{@link #pump}: the swing keeps its speed (a push along his motion below a cruise speed, W a little more).</li>
 *   <li>{@link #releaseNow}: the best moment to let go, past the bottom and rising.</li>
 * </ul>
 */
public final class SwingAssist {
    /** What the web is fixed to. */
    public enum Kind {
        /** A block's face, as in a manual swing. */
        BLOCK,
        /** A point of the open sky ahead and above him (the open-land fallback). */
        SKY,
        /** No swing: a short web boost forward and up (the other open-land fallback). */
        BOOST,
        /**
         * No swing (3.3.1): from the floor of a forest, a dash to a gap in the canopy and a web leap up through it, to swing
         * on from above it (see {@link #climb}). The anchor's point is the top of the gap, its pivot the foot of it.
         */
        CLIMB
    }

    /** What the assist does where there is nothing to swing from (the config's {@code webAssistOpenLand}). */
    public enum OpenLand {
        SKY, ZIP, NONE;

        public static OpenLand of(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "zip" -> ZIP;
                case "none" -> NONE;
                default -> SKY;
            };
        }
    }

    /**
     * An anchor the assist chose: where the web is fixed, on which face, of what kind, the point he swings about (see
     * {@link #pivot}), the rope to start with (from the middle of his body to the pivot), and its score. (For a
     * {@link Kind#CLIMB} the point is the top of the gap and the pivot the foot of it, where he leaps.)
     */
    public record Anchor(Vec3 point, Direction face, Kind kind, Vec3 pivot, double rope, double score) {
        /** The length of the web itself, from the middle of his body to where it is fixed. */
        public double web(Vec3 centre) {
            return centre.distanceTo(point);
        }
    }

    /** The numbers of the swing the assist plans with (the config's, scaled by his speed). */
    public record Params(double range, double gravity, double maxSpeed) {
    }

    /** The middle of his body above his feet: the point that swings on the rope. */
    public static final double CENTRE = 0.9;
    /** An anchor at least this far above his eyes. */
    public static final double MIN_RISE = 2.5;
    /** The shortest rope the assist reels in to. */
    public static final double MIN_ROPE = 2.8;
    /** The middle of his body stays this far above the ground (his feet 0.2 above it). */
    public static final double CLEARANCE = 1.1;
    /** The most the rope is reeled in in one tick. */
    public static final double REEL = 0.35;
    /** The rope swings past the vertical by this many degrees before he lets go. */
    public static final double RELEASE_ANGLE = 35.0;
    /** A swing is flown through for this many ticks before it is taken. */
    public static final int LOOK_AHEAD = 40;
    /** The drag of a swing (the same as a manual swing's). */
    public static final double DRAG = 0.992;
    /** Below this share of his top speed, an assisted swing is pushed along. */
    public static final double CRUISE = 0.55;
    private static final double PUSH = 0.02;
    private static final double PUMP = 0.015;
    /** The best few candidates (by score) are flown through, in order. */
    private static final int TRIED = 8;
    /** The ideal angle of the web above the horizontal, and the ideal length. */
    private static final double IDEAL_ANGLE = 50.0;
    private static final double IDEAL_LENGTH = 16.0;
    /** The fan of rays: yaw offsets round his heading, and pitches above the horizontal (degrees). */
    private static final double[] YAWS = {0, -12, 12, -24, 24, -36, 36, -48, 48};
    private static final double[] PITCHES = {18, 26, 34, 42, 50, 58, 66, 75, 84};
    /** Where a sky anchor is put: blocks ahead, blocks up, tried in order. */
    private static final double[][] SKY = {{9, 13}, {6, 11}, {12, 16}, {4, 9}};
    /** A sky anchor needs the sky open this far above it. */
    private static final int SKY_OPEN = 24;
    /** How far down the ground is looked for under a point. */
    private static final int DEPTH = 48;
    /** A swing begun on the ground may rise over a step this high in its first ticks (as his body does when he hops). */
    private static final double STEP = 1.0;
    private static final int STEP_TICKS = 5;

    // ---- the climb out of a forest (3.3.1) ----
    /** The physics of his leap: the game's gravity and vertical drag, and the horizontal drag kept between two webs. */
    public static final double LEAP_GRAVITY = 0.08;
    public static final double LEAP_DRAG = 0.98;
    /** The fastest leap up (a leap of about 12 blocks), and the slowest that is tried. */
    private static final double LEAP_MAX_UP = 1.6;
    private static final double LEAP_MIN_UP = 0.7;
    /** At the top of the leap his feet are this far above the canopy round the gap. */
    private static final double LEAP_CLEAR = 1.0;
    /** The gaps looked for: this many blocks ahead of him, this many to each side. */
    public static final int CLIMB_AHEAD = 5;
    private static final int CLIMB_SIDE = 2;
    /** How fast he dashes along the floor to the foot of the gap, and for how long at most. */
    public static final double CLIMB_DASH = 0.45;
    public static final int CLIMB_DASH_TICKS = 16;
    /** A canopy is what stands over him within this many blocks (higher than that, the leap would not get out of it). */
    public static final int CLIMB_HEIGHT = 12;
    /** How near the ground he must be for a climb (his feet over the ground under him). */
    public static final double CLIMB_NEAR_GROUND = 1.5;

    // =================================================================================================================
    // Choosing
    // =================================================================================================================

    /**
     * The best anchor for a swing of {@code body} from where it is now, or null when there is none (and the open-land
     * fallback is off or finds nothing either).
     *
     * @param feet     where his feet are
     * @param look     where he looks (a unit vector)
     * @param velocity how he moves now (with the hop of a swing begun on the ground already in it)
     * @param bounds   when not null, only blocks inside it are anchors (the game tests keep to their own arena)
     */
    @Nullable
    public static Anchor choose(Level level, Entity body, Vec3 feet, Vec3 look, Vec3 velocity, Params params, OpenLand openLand,
                                @Nullable AABB bounds) {
        Ground ground = new Ground(level);
        Vec3 eye = feet.add(0, body.getEyeHeight(), 0);
        Vec3 centre = feet.add(0, CENTRE, 0);
        Vec3 heading = heading(look, velocity, body.getYRot());
        double range = params.range();

        // The block the crosshair is on: a good anchor there wins a tie.
        BlockHitResult aimed = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body));
        BlockPos aimedBlock = aimed.getType() == HitResult.Type.BLOCK ? aimed.getBlockPos() : null;

        List<Anchor> candidates = new ArrayList<>();
        Map<BlockPos, Boolean> seen = new HashMap<>();
        double baseYaw = Math.atan2(heading.z, heading.x);
        List<BlockHitResult> hits = new ArrayList<>();
        if (aimedBlock != null) {
            hits.add(aimed);
        }
        for (double yaw : YAWS) {
            double angle = baseYaw + Math.toRadians(yaw);
            for (double pitch : PITCHES) {
                double p = Math.toRadians(pitch);
                Vec3 direction = new Vec3(Math.cos(angle) * Math.cos(p), Math.sin(p), Math.sin(angle) * Math.cos(p));
                BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(direction.scale(range)), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, body));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    hits.add(hit);
                }
            }
        }
        for (BlockHitResult hit : hits) {
            if (seen.putIfAbsent(hit.getBlockPos(), Boolean.TRUE) != null) {
                continue;
            }
            Vec3 point = hit.getLocation();
            if (bounds != null && !bounds.contains(point)) {
                continue;
            }
            double score = score(ground, eye, look, heading, point, hit.getDirection(), range, hit.getBlockPos().equals(aimedBlock));
            if (!Double.isNaN(score)) {
                Vec3 pivot = pivot(point, centre, heading);
                candidates.add(new Anchor(point, hit.getDirection(), Kind.BLOCK, pivot, Math.max(MIN_ROPE, centre.distanceTo(pivot)), score));
            }
        }
        candidates.sort(Comparator.comparingDouble(Anchor::score).reversed());
        for (int i = 0; i < Math.min(TRIED, candidates.size()); i++) {
            Anchor candidate = candidates.get(i);
            if (fly(level, body, ground, feet, velocity, candidate.pivot(), candidate.rope(), params).clear()) {
                return candidate;
            }
        }
        if (openLand == OpenLand.SKY) {
            Anchor sky = sky(level, body, ground, feet, eye, heading, velocity, params);
            if (sky != null) {
                return sky;
            }
        }
        // (3.3.1) On the floor of a forest nothing is clear to swing from, nor the sky: up through a gap in the canopy first.
        Anchor climb = climb(level, body, ground, feet, heading, bounds);
        if (climb != null) {
            return climb;
        }
        return switch (openLand) {
            case SKY -> null;
            case ZIP -> {
                Vec3 toward = eye.add(heading.scale(8.0)).add(0, 4.0, 0);
                yield new Anchor(toward, Direction.DOWN, Kind.BOOST, toward, 0.0, 0.0);
            }
            case NONE -> null;
        };
    }

    /** The share of an anchor's offset to the side of his way that still pulls him sideways. */
    public static final double SIDE_PULL = 0.25;

    /**
     * The point an assisted swing swings about: the anchor, moved towards the line he goes along so that only a quarter
     * of its offset to the side is left. A pendulum hung from a wall beside him swings him into the wall; the games pull
     * him away from the building he hangs from (Insomniac's "outward force") so that he swings down the middle of the
     * street, and this is that pull. The web is still drawn to the anchor itself.
     */
    public static Vec3 pivot(Vec3 anchor, Vec3 centre, Vec3 heading) {
        Vec3 to = new Vec3(anchor.x - centre.x, 0, anchor.z - centre.z);
        Vec3 along = heading.scale(to.dot(heading));
        Vec3 side = to.subtract(along);
        return anchor.subtract(side.scale(1.0 - SIDE_PULL));
    }

    /**
     * The way he is going, flat: where he looks, leaning towards where he moves when he moves fast (his yaw when he looks
     * straight up or down and does not move).
     */
    public static Vec3 heading(Vec3 look, Vec3 velocity, float yaw) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 moving = new Vec3(velocity.x, 0, velocity.z);
        Vec3 sum = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;
        if (moving.lengthSqr() > 0.0625) {
            sum = sum.add(moving.normalize().scale(0.4));
        }
        if (sum.lengthSqr() < 1.0E-4) {
            double radians = yaw * Mth.DEG_TO_RAD;
            return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
        }
        return sum.normalize();
    }

    /**
     * How good an anchor is (higher is better), or NaN when it is none: it must be at least {@link #MIN_RISE} above his
     * eyes, ahead of him, at least five blocks off and within range.
     */
    private static double score(Ground ground, Vec3 eye, Vec3 look, Vec3 heading, Vec3 point, Direction face, double range, boolean aimed) {
        Vec3 to = point.subtract(eye);
        double rise = to.y;
        double distance = to.length();
        double flat = Math.hypot(to.x, to.z);
        double ahead = to.x * heading.x + to.z * heading.z;
        if (rise < MIN_RISE || distance < 5.0 || distance > range || ahead < 1.0 || face == Direction.UP) {
            return Double.NaN;
        }
        Vec3 out = point.add(face.getStepX() * 0.3, face.getStepY() * 0.3, face.getStepZ() * 0.3);
        double height = Math.min(24.0, point.y - ground.below(out.x, out.y - 0.01, out.z));
        if (height < MIN_ROPE + CLEARANCE) {
            return Double.NaN;
        }
        double angle = Math.toDegrees(Math.atan2(rise, flat));
        double steep = 1.0 - Math.min(1.0, Math.abs(angle - IDEAL_ANGLE) / IDEAL_ANGLE);
        double along = ahead / Math.max(flat, 1.0E-3);
        double length = 1.0 - Math.min(1.0, Math.abs(distance - IDEAL_LENGTH) / IDEAL_LENGTH);
        double tall = height / 24.0;
        double near = Math.pow(Math.max(0.0, to.scale(1.0 / distance).dot(look)), 4.0);
        return 1.0 * steep + 1.2 * along + 0.6 * length + 1.4 * tall + 0.8 * near + (aimed ? 0.6 : 0.0);
    }

    /** A point of the open sky ahead and above him whose swing flies clear, or null. */
    @Nullable
    private static Anchor sky(Level level, Entity body, Ground ground, Vec3 feet, Vec3 eye, Vec3 heading, Vec3 velocity, Params params) {
        Vec3 centre = feet.add(0, CENTRE, 0);
        for (double[] place : SKY) {
            Vec3 point = centre.add(heading.scale(place[0])).add(0, place[1], 0);
            if (!openSky(level, body, eye, point)) {
                continue;
            }
            double rope = centre.distanceTo(point);
            if (fly(level, body, ground, feet, velocity, point, rope, params).clear()) {
                return new Anchor(point, Direction.DOWN, Kind.SKY, point, rope, 0.0);
            }
        }
        return null;
    }

    /** Whether a sky anchor may be at {@code point}: in sight from his eyes, with no block over it for a long way up. */
    public static boolean openSky(Level level, Entity body, Vec3 eye, Vec3 point) {
        if (level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body)).getType() != HitResult.Type.MISS) {
            return false;
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int x = Mth.floor(point.x);
        int z = Mth.floor(point.z);
        int top = Math.min(level.getMaxBuildHeight(), Mth.floor(point.y) + SKY_OPEN);
        for (int y = Mth.floor(point.y) - 1; y <= top; y++) {
            at.set(x, y, z);
            if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // =================================================================================================================
    // Flying it through
    // =================================================================================================================

    /** What a swing flown through came to: whether it stayed clear, when it would let go, and its lowest clearance. */
    public record Flight(boolean clear, int releaseTick, double lowest) {
    }

    /**
     * Flies a swing from {@code feet} at {@code velocity} on a rope of {@code rope} about {@code anchor}, with the assist's
     * reeling, push and release, for up to {@link #LOOK_AHEAD} ticks: clear when his body touches no block on the way
     * (each tick and half-way between) and his middle never comes within the clearance of the ground.
     */
    public static Flight fly(Level level, Entity body, Vec3 feet, Vec3 velocity, Vec3 anchor, double rope, Params params) {
        return fly(level, body, new Ground(level), feet, velocity, anchor, rope, params);
    }

    private static Flight fly(Level level, Entity body, Ground ground, Vec3 feet, Vec3 velocity, Vec3 anchor, double rope, Params params) {
        Vec3 centre = feet.add(0, CENTRE, 0);
        Vec3 v = velocity;
        double length = rope;
        double lowest = Double.MAX_VALUE;
        boolean grounded = feet.y - ground.below(feet.x, feet.y + 0.01, feet.z) < 0.05;
        for (int tick = 1; tick <= LOOK_AHEAD; tick++) {
            length = reel(ground, anchor, length, centre, v);
            SpiderMath.Swing step = SpiderMath.swingStep(centre, v, anchor, length, pump(v, centre, anchor, 0, params.maxSpeed()),
                    params.gravity(), DRAG, params.maxSpeed());
            Vec3 next = step.position();
            if (!fits(level, body, centre.lerp(next, 0.5)) || !fits(level, body, next)) {
                // (Begun on the ground, a step of the forest floor in front of him only holds him back while he rises
                // over it, as the game moves his body: not a snag. A trunk or a wall is higher than a step.)
                Vec3 up = new Vec3(centre.x, next.y, centre.z);
                if (grounded && tick <= STEP_TICKS && next.y >= centre.y && fits(level, body, next.add(0, STEP, 0)) && fits(level, body, up)) {
                    centre = up;
                    v = new Vec3(0, step.velocity().y, 0);
                    continue;
                }
                return new Flight(false, tick, lowest);
            }
            lowest = Math.min(lowest, next.y - ground.below(next.x, next.y, next.z));
            if (lowest < CLEARANCE - 0.2) {
                return new Flight(false, tick, lowest);
            }
            centre = next;
            v = step.velocity();
            if (releaseNow(anchor, centre, v, tick)) {
                return new Flight(true, tick, lowest);
            }
        }
        return new Flight(true, -1, lowest);
    }

    /** Whether his body, its middle at {@code centre}, touches no block there. */
    private static boolean fits(Level level, Entity body, Vec3 centre) {
        double half = body.getBbWidth() / 2.0 - 0.01;
        double height = body.getBbHeight() - 0.02;
        double y = centre.y - CENTRE + 0.01;
        return level.noCollision(body, new AABB(centre.x - half, y, centre.z - half, centre.x + half, y + height, centre.z + half));
    }

    // =================================================================================================================
    // The climb out of a forest (3.3.1)
    // =================================================================================================================

    /**
     * A climb up through a gap in the canopy, for when he stands on the floor of a forest (or near it) under leaves or
     * branches and nothing is clear to swing from: the gaps ahead of him (up to {@link #CLIMB_AHEAD} blocks ahead and two
     * to each side, the nearest and straightest first) whose column is open from the floor to the sky. He dashes along
     * the floor to the foot of the gap and leaps straight up it, his feet {@link #LEAP_CLEAR} over the canopy round it at
     * the top; both are flown through (his body touches nothing on the way). From the top of the leap the next web swings
     * him over the trees. Null when he is not near the ground, nothing stands over him, or no gap lets him out.
     *
     * <p>The anchor's point is the top of the gap (two blocks over the top of the leap), its pivot the foot of the gap
     * where the leap begins ({@link #leapUp} is the speed of the leap).
     */
    @Nullable
    public static Anchor climb(Level level, Entity body, Vec3 feet, Vec3 heading, @Nullable AABB bounds) {
        return climb(level, body, new Ground(level), feet, heading, bounds);
    }

    @Nullable
    private static Anchor climb(Level level, Entity body, Ground ground, Vec3 feet, Vec3 heading, @Nullable AABB bounds) {
        if (feet.y - ground.below(feet.x, feet.y + 0.01, feet.z) > CLIMB_NEAR_GROUND || !covered(level, body, feet)) {
            return null;
        }
        Vec3 ahead = new Vec3(heading.x, 0, heading.z).normalize();
        Vec3 side = new Vec3(-ahead.z, 0, ahead.x);
        for (int forward = 0; forward <= CLIMB_AHEAD; forward++) {
            for (int across = 0; across <= 2 * CLIMB_SIDE; across++) {
                int sideways = (across + 1) / 2 * (across % 2 == 0 ? 1 : -1);
                Vec3 at = feet.add(ahead.scale(forward)).add(side.scale(sideways));
                Vec3 spot = new Vec3(Mth.floor(at.x) + 0.5, feet.y, Mth.floor(at.z) + 0.5);
                if (bounds != null && !bounds.contains(spot.add(0, 1.0, 0)) || !open(level, feet, spot)) {
                    continue;
                }
                // (The foot of the gap may be a step up, or down, from where he stands.)
                double floor = ground.below(spot.x, feet.y + STEP + 0.01, spot.z);
                if (floor < feet.y - CLIMB_NEAR_GROUND) {
                    continue;
                }
                spot = new Vec3(spot.x, floor, spot.z);
                double top = Math.max(canopy(level, spot, 2), feet.y) + LEAP_CLEAR;
                if (top - feet.y > CLIMB_HEIGHT + LEAP_CLEAR) {
                    continue;
                }
                double up = leapUp(top - feet.y);
                if (!Double.isNaN(up) && dashClear(level, body, feet, spot) && leapClear(level, body, spot, new Vec3(0, up, 0))) {
                    return new Anchor(new Vec3(spot.x, top + 2.0, spot.z), Direction.DOWN, Kind.CLIMB, spot, 0.0, 0.0);
                }
            }
        }
        return null;
    }

    /** The speed of the leap of a climb (see {@link #climb}): up from its pivot to two blocks under its point. */
    public static double leapUp(Anchor climb) {
        double up = leapUp(climb.point().y - 2.0 - climb.pivot().y);
        return Double.isNaN(up) ? LEAP_MAX_UP : up;
    }

    /** Whether the column of {@code column} is open from about his feet up to the sky (nothing that blocks motion over it). */
    public static boolean open(Level level, Vec3 feet, Vec3 column) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(column.x), Mth.floor(column.z)) <= Mth.floor(feet.y) + 1;
    }

    /** The highest top of what blocks motion in the square of the radius round a point, as high as {@link #CLIMB_HEIGHT} over it. */
    private static double canopy(Level level, Vec3 at, int radius) {
        int x0 = Mth.floor(at.x);
        int z0 = Mth.floor(at.z);
        int limit = Mth.floor(at.y) + CLIMB_HEIGHT + 1;
        double top = Double.NEGATIVE_INFINITY;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                top = Math.max(top, Math.min(limit, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x0 + dx, z0 + dz)));
            }
        }
        return top;
    }

    /**
     * Whether he is among trees: leaves or branches over him or round him (within three blocks), a block over the height of
     * his head with room under it (a wall or a trunk is no canopy, the crown on it is).
     */
    private static boolean covered(Level level, Entity body, Vec3 feet) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int x0 = Mth.floor(feet.x);
        int z0 = Mth.floor(feet.z);
        int head = Mth.floor(feet.y + body.getBbHeight());
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                at.set(x0 + dx, head, z0 + dz);
                if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                    continue;
                }
                for (int y = head + 1; y <= Mth.floor(feet.y) + CLIMB_HEIGHT; y++) {
                    at.set(x0 + dx, y, z0 + dz);
                    if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The slowest leap straight up (the game's gravity and drag) that rises {@code rise} blocks, or NaN when none does. */
    public static double leapUp(double rise) {
        for (double up = LEAP_MIN_UP; up <= LEAP_MAX_UP + 1.0E-9; up += 0.05) {
            double y = 0.0;
            double vy = up;
            while (vy > 0.0) {
                y += vy;
                vy = (vy - LEAP_GRAVITY) * LEAP_DRAG;
            }
            if (y >= rise) {
                return up;
            }
        }
        return Double.NaN;
    }

    /**
     * Whether his body fits all along the way from {@code feet} to {@code spot} on the floor: a step up of one block on
     * the way is hopped over (his client hops when the dash meets one), a step down is walked off.
     */
    private static boolean dashClear(Level level, Entity body, Vec3 feet, Vec3 spot) {
        double length = Math.hypot(spot.x - feet.x, spot.z - feet.z);
        int steps = Math.max(1, Mth.ceil(length / 0.25));
        double y = feet.y;
        for (int i = 1; i <= steps; i++) {
            Vec3 at = feet.lerp(spot, i / (double) steps);
            if (fits(level, body, new Vec3(at.x, y + CENTRE + 0.01, at.z))) {
                continue;
            }
            if (y + STEP > feet.y + STEP + 1.0E-6 || !fits(level, body, new Vec3(at.x, y + STEP + CENTRE + 0.01, at.z))) {
                return false;
            }
            y += STEP;
        }
        return true;
    }

    /** Whether his body touches no block on the way up a leap of {@code velocity} from {@code feet}, to its top. */
    public static boolean leapClear(Level level, Entity body, Vec3 feet, Vec3 velocity) {
        Vec3 centre = feet.add(0, CENTRE, 0);
        Vec3 v = velocity;
        for (int tick = 0; tick < 60 && v.y > 0.0; tick++) {
            Vec3 next = centre.add(v);
            if (!fits(level, body, centre.lerp(next, 0.5)) || !fits(level, body, next)) {
                return false;
            }
            centre = next;
            v = new Vec3(v.x * LEAP_DRAG, (v.y - LEAP_GRAVITY) * LEAP_DRAG, v.z * LEAP_DRAG);
        }
        return true;
    }

    // =================================================================================================================
    // While he swings
    // =================================================================================================================

    /**
     * The rope after this tick's reeling: drawn in (by {@link #REEL} at most, never under {@link #MIN_ROPE}) when the
     * bottom of the arc would come within {@link #CLEARANCE} of the ground under the anchor, or under where he will be in
     * four ticks (a hill ahead).
     */
    public static double reel(Level level, Vec3 anchor, double rope, Vec3 centre, Vec3 velocity) {
        return reel(new Ground(level), anchor, rope, centre, velocity);
    }

    private static double reel(Ground ground, Vec3 anchor, double rope, Vec3 centre, Vec3 velocity) {
        double top = anchor.y - 0.01;
        // (Under the anchor on his side of it: an anchor on the side of a block is on the block's edge.)
        Vec3 side = new Vec3(centre.x - anchor.x, 0, centre.z - anchor.z);
        Vec3 under = side.lengthSqr() < 1.0E-4 ? anchor : anchor.add(side.normalize().scale(0.35));
        double floor = ground.below(under.x, top, under.z);
        Vec3 ahead = centre.add(velocity.x * 4.0, 0, velocity.z * 4.0);
        if (ahead.distanceTo(new Vec3(anchor.x, ahead.y, anchor.z)) < rope) {
            floor = Math.max(floor, ground.below(ahead.x, Math.min(top, centre.y + 1.0), ahead.z));
        }
        double short_ = anchor.y - rope - (floor + CLEARANCE);
        if (short_ >= 0.0) {
            return rope;
        }
        return Math.max(MIN_ROPE, rope - Math.min(REEL, -short_));
    }

    /**
     * The push an assisted swing gets this tick, along his motion: below {@link #CRUISE} of his top speed, while he is
     * below the anchor, it keeps him going; {@code forward} (W +1, S -1) pumps or brakes a little.
     */
    public static Vec3 pump(Vec3 velocity, Vec3 centre, Vec3 anchor, int forward, double maxSpeed) {
        double speed = velocity.length();
        if (speed < 1.0E-3) {
            return Vec3.ZERO;
        }
        Vec3 along = velocity.scale(1.0 / speed);
        double push = 0.0;
        if (speed < CRUISE * maxSpeed && centre.y < anchor.y - 1.0) {
            push += PUSH;
        }
        push += forward * PUMP;
        return along.scale(push);
    }

    /**
     * Whether an assisted swing lets go now: after its first ticks, rising, with the web {@link #RELEASE_ANGLE} past the
     * vertical the way he goes, or the arc about to stall past the bottom, or his body up level with the anchor.
     */
    public static boolean releaseNow(Vec3 anchor, Vec3 centre, Vec3 velocity, int age) {
        if (age < 6 || velocity.y <= 0.02) {
            return false;
        }
        double flat = Math.hypot(velocity.x, velocity.z);
        if (flat < 1.0E-3) {
            return centre.y > anchor.y - 1.0;
        }
        double past = ((centre.x - anchor.x) * velocity.x + (centre.z - anchor.z) * velocity.z) / flat;
        double below = anchor.y - centre.y;
        if (below < 1.0) {
            return true;
        }
        double angle = Math.toDegrees(Math.atan2(past, below));
        return angle >= RELEASE_ANGLE || (past > 0.0 && velocity.length() < 0.3);
    }

    /**
     * His horizontal motion turned towards {@code heading} by at most {@code degrees}, its speed kept (the gentle steering
     * of an assisted swing: the arc follows the camera).
     */
    public static Vec3 steer(Vec3 velocity, Vec3 heading, double degrees) {
        double flat = Math.hypot(velocity.x, velocity.z);
        if (flat < 0.05 || heading.lengthSqr() < 1.0E-6) {
            return velocity;
        }
        double from = Math.atan2(velocity.z, velocity.x);
        double to = Math.atan2(heading.z, heading.x);
        double turn = Mth.wrapDegrees(Math.toDegrees(to - from));
        double step = Math.toRadians(Mth.clamp(turn, -degrees, degrees));
        double angle = from + step;
        return new Vec3(Math.cos(angle) * flat, velocity.y, Math.sin(angle) * flat);
    }

    /**
     * How he leaves the ground when a swing begins there: hopped up and pushed towards the anchor (from rest the
     * pendulum's way down is into the floor, so it would never get going).
     */
    public static Vec3 hop(Vec3 velocity, Vec3 feet, Vec3 anchor, Vec3 look, double hop, double push) {
        Vec3 toward = new Vec3(anchor.x - feet.x, 0, anchor.z - feet.z);
        toward = toward.lengthSqr() < 1.0 ? new Vec3(look.x, 0, look.z) : toward;
        toward = toward.lengthSqr() < 1.0E-6 ? Vec3.ZERO : toward.normalize().scale(push);
        return new Vec3(velocity.x + toward.x, Math.max(velocity.y, hop), velocity.z + toward.z);
    }

    // =================================================================================================================
    // The ground
    // =================================================================================================================

    /** The top of the ground (a solid top, or the surface of water or lava) under points, remembered for one choice. */
    private static final class Ground {
        private final Level level;
        private final Map<Long, Double> known = new HashMap<>();

        Ground(Level level) {
            this.level = level;
        }

        /** The height of the first solid top at or under {@code y} in the column of x, z (far below when none is near). */
        double below(double x, double y, double z) {
            int bx = Mth.floor(x);
            int by = Mth.floor(y);
            int bz = Mth.floor(z);
            long key = BlockPos.asLong(bx, by, bz);
            Double cached = known.get(key);
            if (cached != null) {
                return cached;
            }
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            double found = by - DEPTH;
            for (int cy = by; cy > by - DEPTH && cy >= level.getMinBuildHeight(); cy--) {
                at.set(bx, cy, bz);
                BlockState state = level.getBlockState(at);
                if (!state.getFluidState().isEmpty()) {
                    // (Water and lava are ground too: a swing over a river keeps out of it.)
                    double surface = cy + state.getFluidState().getHeight(level, at);
                    if (surface <= y + 1.0E-6) {
                        found = surface;
                        break;
                    }
                }
                VoxelShape shape = state.getCollisionShape(level, at);
                if (!shape.isEmpty()) {
                    double topOf = cy + shape.max(Direction.Axis.Y);
                    if (topOf <= y + 1.0E-6) {
                        found = topOf;
                        break;
                    }
                }
            }
            known.put(key, found);
            return found;
        }
    }

    /** The ground under a point (see {@link #choose}): the first solid top at or under it, or far below. */
    public static double groundBelow(Level level, Vec3 point) {
        return new Ground(level).below(point.x, point.y, point.z);
    }

    private SwingAssist() {
    }
}
