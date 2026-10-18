package com.kolodkingal.enhanced.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A speedster running up walls and along ceilings (3.3, Quicksilver and Tommy): the rules, as a pure step that turns
 * the runner's state and keys into the velocity of one tick. Like Spider-Man's cling it is a movement rule, not a new
 * physics: the hitbox stays the upright standing box (so he can never end up inside a block: every move goes through
 * the game's own collision), and while he runs on a surface his whole velocity is set at the start of the tick, so
 * gravity does not apply. Unlike Spider-Man he does not cling: he runs, and below {@code minSpeed} he drops off.
 *
 * <p>No client imports: the player's own client (which moves him, see {@code SpeedsterMotion}), the server (which only
 * checks what the client says, see {@link SpeedSurfaces}) and the game tests (which move a server player with
 * {@code Player.move}) all run the same code.
 *
 * <p>The transitions: a wall he sprints into (W pressing into it) at super speed takes him up it; at its top he hops
 * over onto it; a ceiling above takes him onto the ceiling (running on, away from the wall); on a ceiling a wall ahead
 * takes him down that wall to the floor; the edge of a ceiling with a face going up (the lip of an overhang) wraps him
 * round the lip and up the outer face; a hole in the ceiling, the end of a wall in the air, or too little speed drops
 * him. The camera of a hero who has to turn round to keep running (onto a ceiling, round a lip) is turned by
 * {@link Runner#turn} degrees over the next ticks by his client.
 */
public final class SurfaceRun {
    /** How far off his box a block may be and still be the surface he runs on. */
    public static final double REACH = 0.12;
    /** The push into the surface that keeps him touching it, in blocks per tick. */
    public static final double STICK = 0.04;
    /** How many ticks the contact may be lost before he drops (a gap of a block in a wall is crossed). */
    public static final int LOST_TICKS = 2;
    /** The ticks round the lip of an overhang before his sides must touch its outer face. */
    public static final int LIP_GRACE = 3;
    /** After a drop he does not take a wall again for this many ticks. */
    public static final int RESTICK_TICKS = 10;
    /** The hop over the top of a wall lasts at most this many ticks. */
    public static final int HOP_TICKS = 8;
    /** How much of the forward key must push into a wall to run up it (0.5: within 60 degrees of straight at it). */
    public static final double PRESS = 0.5;
    /** The momentum gained in a tick towards the run speed, and the part of it kept in a tick without running. */
    public static final double ACCEL = 0.25;
    public static final double DECAY = 0.72;
    /** How much of the run speed a strafe key adds along a wall. */
    public static final double STRAFE = 0.4;
    /** How fast the heading turns towards the look on a ceiling (part of the angle per tick). */
    public static final double STEER = 0.35;
    /** Ground speed per tick of a movement speed attribute of 1 (vanilla sprint: 0.13 is about 0.28 blocks a tick). */
    public static final double GROUND_SPEED = 2.16;
    /** The camera's turn at a transition that reverses his way (degrees, spread over the next ticks). */
    public static final float REVERSE = 180.0F;

    /** What he runs on: nothing, a wall (the normal is horizontal) or a ceiling (the normal is down). */
    public enum Kind {
        NONE, WALL, CEILING
    }

    /** What happened in the last step (for the effects, the animation and the tests). */
    public enum Event {
        NONE, ENTER_WALL, ONTO_CEILING, DOWN_WALL, ROUND_LIP, OVER_TOP, TO_GROUND, DROP
    }

    /** A surface he runs on: its kind and the direction it faces, from the block towards him (a ceiling's is down). */
    public record Surface(Kind kind, Direction normal) {
        public static final Surface NONE = new Surface(Kind.NONE, Direction.UP);
        public static final Surface CEILING = new Surface(Kind.CEILING, Direction.DOWN);

        public static Surface wall(Direction normal) {
            return new Surface(Kind.WALL, normal);
        }

        public boolean touches() {
            return kind != Kind.NONE;
        }

        public Vec3 normalVector() {
            return new Vec3(normal.getStepX(), normal.getStepY(), normal.getStepZ());
        }
    }

    /**
     * The keys and conditions of one tick.
     *
     * @param forward  the W/S axis (-1..1)
     * @param strafe   the A/D axis (+1 to the left, as the game's own impulse)
     * @param yaw      his facing in degrees
     * @param running  he runs at super speed now (and may run on surfaces: the toggle and the config are on)
     * @param speed    the speed of a run on a surface ({@link #targetSpeed})
     * @param minSpeed below this he drops
     * @param ceilings ceilings (and overhangs) may be run along
     */
    public record Input(float forward, float strafe, float yaw, boolean running, double speed, double minSpeed, boolean ceilings) {
    }

    /** One runner's state between ticks. */
    public static final class Runner {
        public Surface surface = Surface.NONE;
        /** His speed along the surface, in blocks per tick. */
        public double momentum;
        /** On a wall: +1 up, -1 down. */
        public int vertical = 1;
        /** On a ceiling: the horizontal unit vector he runs along. */
        public Vec3 heading = Vec3.ZERO;
        /** Degrees the camera still has to turn (the client turns it and counts it down). */
        public float turn;
        public Event event = Event.NONE;
        /** Ticks on the current run (from the wall he took). */
        public int age;
        int lost;
        int hop;
        Direction hopNormal = Direction.NORTH;
        int restick;

        /** Whether he runs on a surface or hops over the top of one (his client then owns his velocity). */
        public boolean active() {
            return surface.touches() || hop > 0;
        }

        public void reset() {
            surface = Surface.NONE;
            momentum = 0.0;
            vertical = 1;
            heading = Vec3.ZERO;
            turn = 0.0F;
            event = Event.NONE;
            age = 0;
            lost = 0;
            hop = 0;
            restick = 0;
        }
    }

    // ---- speeds ----

    /** The speed of a run on a surface: {@code factor} of his ground speed (from his movement speed), at most {@code max}. */
    public static double targetSpeed(double movementSpeed, double factor, double max) {
        return Math.min(max, movementSpeed * GROUND_SPEED * factor);
    }

    // ---- the step ----

    /**
     * One tick of the run: the velocity he is to have for this tick's move, or null when the game moves him as ever
     * (not on a surface). Call it at the start of his tick, after the last tick's move (its collisions say whether he ran
     * into a wall), and give him the velocity before he moves.
     */
    @Nullable
    public static Vec3 step(Level level, Player player, Runner r, Input in) {
        r.event = Event.NONE;
        if (r.restick > 0) {
            r.restick--;
        }
        if (!r.surface.touches()) {
            if (r.hop > 0) {
                return hop(level, player, r);
            }
            Surface wall = in.running && r.restick <= 0 && in.forward > 0.0F && player.horizontalCollision
                    ? pressedWall(level, player, wish(in.forward, in.strafe, in.yaw)) : Surface.NONE;
            if (!wall.touches()) {
                return null;
            }
            r.surface = wall;
            r.vertical = 1;
            r.momentum = Math.max(r.momentum, in.speed * 0.75);
            r.lost = 0;
            r.age = 0;
            r.event = Event.ENTER_WALL;
        }
        r.age++;
        double target = in.running && in.forward > 0.0F ? in.speed : 0.0;
        r.momentum = target >= r.momentum ? Math.min(target, r.momentum + ACCEL) : Math.max(target, r.momentum * DECAY);
        if (r.momentum < in.minSpeed) {
            return drop(r);
        }
        return r.surface.kind() == Kind.WALL ? wall(level, player, r, in) : ceiling(level, player, r, in);
    }

    private static Vec3 wall(Level level, Player player, Runner r, Input in) {
        Direction normal = r.surface.normal();
        AABB box = player.getBoundingBox();
        boolean contact = blockIn(level, player, wallSlab(box, normal.getOpposite(), REACH));
        // (Past the top of the wall with something over his head: over the top, not onto that ceiling.)
        if (r.vertical > 0 && (contact || r.lost < 0) && blockIn(level, player, ceilingSlab(box, REACH))) {
            if (!in.ceilings) {
                return drop(r);
            }
            // A ceiling over the wall: on along it, away from the wall (the camera turns round to face the way).
            r.surface = Surface.CEILING;
            r.heading = new Vec3(normal.getStepX(), 0.0, normal.getStepZ());
            r.turn = REVERSE;
            r.lost = 0;
            r.event = Event.ONTO_CEILING;
            return ceilingVelocity(r);
        }
        if (r.vertical < 0 && player.onGround()) {
            r.surface = Surface.NONE;
            r.restick = RESTICK_TICKS;
            r.event = Event.TO_GROUND;
            return null;
        }
        if (!contact) {
            if (r.lost < 0) {
                // Round the lip of an overhang: the outer face is still above his sides.
                r.lost++;
            } else if (r.vertical > 0) {
                // The top of the wall: he hops over onto it.
                r.surface = Surface.NONE;
                r.hop = HOP_TICKS;
                r.hopNormal = normal;
                r.event = Event.OVER_TOP;
                return hop(level, player, r);
            } else if (++r.lost >= LOST_TICKS) {
                return drop(r);
            }
        } else {
            r.lost = 0;
        }
        Vec3 into = new Vec3(-normal.getStepX(), 0.0, -normal.getStepZ());
        // Facing the wall (into), his right is (-into.z, 0, into.x); a positive strafe goes left.
        Vec3 right = new Vec3(-into.z, 0.0, into.x);
        return new Vec3(0.0, r.vertical * r.momentum, 0.0).add(right.scale(-in.strafe() * STRAFE * r.momentum)).add(into.scale(STICK));
    }

    private static Vec3 ceiling(Level level, Player player, Runner r, Input in) {
        if (!in.ceilings) {
            return drop(r);
        }
        if (r.turn <= 0.0F) {
            // He steers with his look (and the strafe keys) once the camera faces the way he runs.
            Vec3 desired = wish(1.0F, in.strafe(), in.yaw());
            Vec3 blended = r.heading.scale(1.0 - STEER).add(desired.scale(STEER));
            if (blended.lengthSqr() > 1.0E-4) {
                r.heading = blended.normalize();
            }
        }
        AABB box = player.getBoundingBox();
        Direction ahead = Direction.getNearest(r.heading.x, 0.0, r.heading.z);
        if (alongAxis(r.heading, ahead) && player.horizontalCollision && blockIn(level, player, wallSlab(box, ahead, REACH))) {
            // A wall ahead: down it to the floor, facing it (the camera already does).
            r.surface = Surface.wall(ahead.getOpposite());
            r.vertical = -1;
            r.lost = 0;
            r.event = Event.DOWN_WALL;
            return wall(level, player, r, in);
        }
        if (!blockIn(level, player, ceilingSlab(box, REACH))) {
            if (alongAxis(r.heading, ahead)) {
                double back = lipBehind(level, player, ahead);
                if (!Double.isNaN(back)) {
                    // The lip of an overhang: round it and up its outer face (back flush against it, facing it).
                    r.surface = Surface.wall(ahead);
                    r.vertical = 1;
                    // The outer face is above his head at first: a few ticks pass before his sides touch it.
                    r.lost = -LIP_GRACE;
                    r.turn = REVERSE;
                    r.event = Event.ROUND_LIP;
                    Vec3 away = new Vec3(ahead.getStepX(), 0.0, ahead.getStepZ());
                    if (back < 0.0) {
                        // Still a little under the lip (the ceiling is lost a tenth of a block early): out from under it first.
                        return away.scale(-back + 0.02);
                    }
                    return new Vec3(0.0, r.momentum, 0.0).add(away.scale(-(back + STICK)));
                }
            }
            if (++r.lost >= LOST_TICKS) {
                return drop(r);
            }
        } else {
            r.lost = 0;
        }
        return ceilingVelocity(r);
    }

    private static Vec3 ceilingVelocity(Runner r) {
        return r.heading.scale(r.momentum).add(0.0, STICK, 0.0);
    }

    /** He drops off the surface: a little away from it, and the game lets him fall (the server spares him the fall). */
    private static Vec3 drop(Runner r) {
        Surface was = r.surface;
        r.surface = Surface.NONE;
        r.hop = 0;
        r.momentum = 0.0;
        r.restick = RESTICK_TICKS;
        r.event = Event.DROP;
        return was.kind() == Kind.WALL ? was.normalVector().scale(0.15) : new Vec3(r.heading.x * 0.1, -0.05, r.heading.z * 0.1);
    }

    /**
     * He leaps off the surface on purpose (Space): off a wall away from it and up, off a ceiling down and on. Returns the
     * leap's velocity, or null when he runs on nothing.
     */
    @Nullable
    public static Vec3 leap(Runner r) {
        if (!r.surface.touches()) {
            return null;
        }
        Surface was = r.surface;
        double speed = Math.max(0.4, r.momentum * 0.6);
        Vec3 heading = r.heading;
        drop(r);
        r.event = Event.DROP;
        return was.kind() == Kind.WALL ? was.normalVector().scale(speed).add(0.0, 0.42, 0.0)
                : new Vec3(heading.x * speed, -0.2, heading.z * speed);
    }

    /** The hop over the top of a wall: up until his feet are over the top, then onto it. */
    private static Vec3 hop(Level level, Player player, Runner r) {
        r.hop--;
        double top = topAboveFeet(level, player, r.hopNormal);
        if (player.onGround() || Double.isNaN(top)) {
            r.hop = 0;
            return null;
        }
        Vec3 into = new Vec3(-r.hopNormal.getStepX(), 0.0, -r.hopNormal.getStepZ());
        if (top <= 0.0) {
            // His feet are over the top: one push onto it, and the game has him again (he runs on, or lands).
            r.hop = 0;
            return into.scale(Math.max(0.3, Math.min(0.6, r.momentum * 0.6)));
        }
        return into.scale(0.04).add(0.0, Math.min(0.6, top + 0.15), 0.0);
    }

    // ---- surfaces ----

    /** Where his keys point, as a horizontal unit vector (zero when none is pressed); yaw in degrees. */
    public static Vec3 wish(float forward, float strafe, float yaw) {
        if (Math.abs(forward) < 1.0E-3F && Math.abs(strafe) < 1.0E-3F) {
            return Vec3.ZERO;
        }
        double rad = yaw * Mth.DEG_TO_RAD;
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        // Facing is (-sin, cos); the player's left is (cos, sin).
        double x = -sin * forward + cos * strafe;
        double z = cos * forward + sin * strafe;
        double length = Math.sqrt(x * x + z * z);
        return new Vec3(x / length, 0.0, z / length);
    }

    /** The wall his keys press into (at least {@link #PRESS}), the one pressed most; or none. */
    public static Surface pressedWall(Level level, Player player, Vec3 wish) {
        AABB box = player.getBoundingBox();
        Direction best = null;
        double most = PRESS - 1.0E-9;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            double press = wish.x * side.getStepX() + wish.z * side.getStepZ();
            if (press > most && blockIn(level, player, wallSlab(box, side, REACH))) {
                most = press;
                best = side;
            }
        }
        return best == null ? Surface.NONE : Surface.wall(best.getOpposite());
    }

    /**
     * What a runner seen from outside runs on (another player's client and the server read it from where he is): a
     * ceiling over his head, else the wall on the side nearest to {@code hint} (a horizontal direction, may be zero),
     * else none.
     */
    public static Surface touched(Level level, Player player, boolean ceiling, Vec3 hint) {
        AABB box = player.getBoundingBox();
        if (ceiling) {
            return blockIn(level, player, ceilingSlab(box, REACH * 2)) ? Surface.CEILING : Surface.NONE;
        }
        Direction best = null;
        double most = -2.0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (blockIn(level, player, wallSlab(box, side, REACH * 2))) {
                double score = hint.x * side.getStepX() + hint.z * side.getStepZ();
                if (score > most) {
                    most = score;
                    best = side;
                }
            }
        }
        return best == null ? Surface.NONE : Surface.wall(best.getOpposite());
    }

    /**
     * Whether a block is within {@code reach} of the side of him that {@code claimed} says he runs on: how the server
     * checks his client's word (its copy of him lags a little, so the reach is wider than {@link #REACH}).
     */
    public static boolean near(Level level, Player player, Surface claimed, double reach) {
        AABB box = player.getBoundingBox();
        return switch (claimed.kind()) {
            case WALL -> claimed.normal().getAxis().isHorizontal() && blockIn(level, player, wallSlab(box, claimed.normal().getOpposite(), reach));
            case CEILING -> blockIn(level, player, ceilingSlab(box, reach));
            case NONE -> false;
        };
    }

    private static boolean alongAxis(Vec3 heading, Direction direction) {
        return heading.x * direction.getStepX() + heading.z * direction.getStepZ() >= 0.7;
    }

    /**
     * The lip he has just run past: the ceiling block right behind him (on the side opposite {@code ahead}), level with
     * the ceiling, with room in front of its outer face for him to run up. Returns how far his back is past that face
     * (negative while he is still a little under the lip), or NaN when there is no such lip.
     */
    static double lipBehind(Level level, Player player, Direction ahead) {
        AABB box = player.getBoundingBox();
        double backFace = ahead.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.min(ahead.getAxis()) : -box.max(ahead.getAxis());
        double y = box.maxY + 0.05;
        double cx = (box.minX + box.maxX) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;
        // The cell behind his back, up to a block back (a fast runner may be well past the lip when it is noticed).
        for (double back = 0.05; back <= 1.25; back += 0.2) {
            double px = ahead.getAxis() == Direction.Axis.X ? (ahead.getStepX() > 0 ? box.minX - back : box.maxX + back) : cx;
            double pz = ahead.getAxis() == Direction.Axis.Z ? (ahead.getStepZ() > 0 ? box.minZ - back : box.maxZ + back) : cz;
            BlockPos cell = BlockPos.containing(px, y, pz);
            if (solid(level, player, cell)) {
                // The face of the lip towards {@code ahead}, as a coordinate along it (positive towards ahead).
                double face = ahead.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? cell.get(ahead.getAxis()) + 1.0 : -cell.get(ahead.getAxis());
                BlockPos front = cell.relative(ahead);
                return solid(level, player, front) ? Double.NaN : backFace - face;
            }
        }
        return Double.NaN;
    }

    private static boolean solid(Level level, Player player, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.of(player)).isEmpty();
    }

    /**
     * The top of the wall he hopped over, above his feet, or NaN: the highest collision top in the column of the cell the
     * wall's normal {@code normal} looks away from, within a block and a half above his feet.
     */
    static double topAboveFeet(Level level, Player player, Direction normal) {
        Vec3 feet = player.position();
        double reach = player.getBbWidth() / 2.0 + 0.3;
        BlockPos column = BlockPos.containing(feet.x - normal.getStepX() * reach, feet.y, feet.z - normal.getStepZ() * reach);
        double best = Double.NaN;
        for (int dy = -1; dy <= 2; dy++) {
            BlockPos pos = column.offset(0, dy, 0);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.of(player));
            if (!shape.isEmpty()) {
                double top = pos.getY() + shape.max(Direction.Axis.Y) - feet.y;
                if (top <= 1.5 && (Double.isNaN(best) || top > best)) {
                    best = top;
                }
            }
        }
        return best;
    }

    /** The thin box, {@code reach} thick, on the {@code side} of his box (from his knees to just under his head). */
    static AABB wallSlab(AABB box, Direction side, double reach) {
        double low = box.minY + 0.3;
        double high = box.maxY - 0.1;
        return switch (side) {
            case EAST -> new AABB(box.maxX, low, box.minZ + 0.1, box.maxX + reach, high, box.maxZ - 0.1);
            case WEST -> new AABB(box.minX - reach, low, box.minZ + 0.1, box.minX, high, box.maxZ - 0.1);
            case SOUTH -> new AABB(box.minX + 0.1, low, box.maxZ, box.maxX - 0.1, high, box.maxZ + reach);
            default -> new AABB(box.minX + 0.1, low, box.minZ - reach, box.maxX - 0.1, high, box.minZ);
        };
    }

    /** The thin box, {@code reach} thick, above his head. */
    static AABB ceilingSlab(AABB box, double reach) {
        return new AABB(box.minX + 0.1, box.maxY, box.minZ + 0.1, box.maxX - 0.1, box.maxY + reach, box.maxZ - 0.1);
    }

    /** Whether a block with a collision shape reaches into {@code slab}. */
    static boolean blockIn(Level level, Player player, AABB slab) {
        CollisionContext context = CollisionContext.of(player);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int x0 = Mth.floor(slab.minX);
        int y0 = Mth.floor(slab.minY);
        int z0 = Mth.floor(slab.minZ);
        int x1 = Mth.floor(slab.maxX - 1.0E-7);
        int y1 = Mth.floor(slab.maxY - 1.0E-7);
        int z1 = Mth.floor(slab.maxZ - 1.0E-7);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    at.set(x, y, z);
                    BlockState state = level.getBlockState(at);
                    if (state.isAir()) {
                        continue;
                    }
                    VoxelShape shape = state.getCollisionShape(level, at, context);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    for (AABB part : shape.toAabbs()) {
                        if (part.move(x, y, z).intersects(slab)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private SurfaceRun() {
    }
}
