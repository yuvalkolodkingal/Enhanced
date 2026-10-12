package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.registry.ModTags;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Walking on walls and ceilings (DESIGN section 14): a movement rule, not a new physics. Minecraft's hitbox is upright
 * and axis-aligned, so "gravity along the wall" does not exist; instead, while he clings, gravity is switched off for him
 * and his own movement keys move him along the surface, and the model is drawn against it. The player keeps the standing
 * box the whole time, so a cling can neither suffocate nor get stuck.
 *
 * <p>This class has no client imports on purpose: the player's own client (which moves him), the server (which only
 * raises the flag others see) and the game tests all run the same code. Directions are unit {@link Vec3}s; a
 * {@link Surface}'s normal points from the block towards him (a ceiling's is down).
 */
public final class SpiderClimb {
    /** How far off his box a block may be and still be a surface he can cling to. */
    public static final double REACH = 0.12;
    /** The push into the surface that keeps him touching it, in blocks per tick, so that contact never flickers. */
    public static final double STICK = 0.05;
    /** How many ticks after a leap he cannot stick again. */
    public static final int RESTICK_DELAY = 6;
    /** The small upward push of a leap, in blocks per tick. */
    public static final double LEAP_LIFT = 0.12;
    /** How far a block's top may be above his feet and he still climbs onto it when his hands lose the wall. */
    public static final double MANTLE_HEIGHT = 0.65;
    /** How far the key axes lean towards climbing when he looks level: looking 27 degrees down, W holds him still. */
    private static final double LOOK_BIAS = 0.45;
    /** The smallest the dead zone of W may shrink the walking vector's divisor to. */
    private static final double DEAD_ZONE = 0.25;
    /** How much of his motion into a wall counts as pressing into it. */
    private static final double PRESS = 0.3;

    /** What he clings to: nothing, a wall (the normal is horizontal) or a ceiling (the normal is down). */
    public enum Kind {
        NONE, WALL, CEILING
    }

    /** A surface he touches: its kind and the direction it faces, from the block towards him. */
    public record Surface(Kind kind, Direction normal) {
        /** Nothing to cling to. */
        public static final Surface NONE = new Surface(Kind.NONE, Direction.UP);

        public boolean touches() {
            return kind != Kind.NONE;
        }

        /** The normal as a vector. */
        public Vec3 normalVector() {
            return new Vec3(normal.getStepX(), normal.getStepY(), normal.getStepZ());
        }
    }

    // ---- surfaces ----

    /**
     * The surface he touches, or {@link Surface#NONE}.
     *
     * <ul>
     *   <li>A wall on a side: the thin box on that side of his box, {@link #REACH} thick, shrunk by 0.1 along the face,
     *       from 0.3 above his feet to 0.1 below his head, contains a block with a collision shape.</li>
     *   <li>A ceiling: the thin box above his head, shrunk by 0.1 all round, does.</li>
     *   <li>A block in the tag {@code enhanced:cannot_cling}, and a block with no collision (a cobweb, a vine, a ladder)
     *       never counts.</li>
     * </ul>
     *
     * Where several touch (a corner) he picks the wall he presses into (the largest {@code wish . -normal}, at least
     * 0.3), else the previous one, else the first; a ceiling only when no wall is pressed, or when it was the previous.
     *
     * @param wish     where his keys point (a horizontal vector, see {@link #wish}), or zero when unknown
     * @param previous what he clung to a moment ago, or null
     */
    public static Surface detect(Level level, Player player, Vec3 wish, @Nullable Surface previous) {
        AABB box = player.getBoundingBox();
        List<Direction> walls = new ArrayList<>(4);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (blockIn(level, player, wallSlab(box, side, REACH))) {
                walls.add(side.getOpposite());
            }
        }
        boolean ceiling = blockIn(level, player, ceilingSlab(box, REACH));
        if (ceiling && previous != null && previous.kind() == Kind.CEILING) {
            return previous;
        }
        Direction pressed = null;
        double most = PRESS - 1.0E-9;
        for (Direction normal : walls) {
            double press = wish.x * -normal.getStepX() + wish.z * -normal.getStepZ();
            if (press > most) {
                most = press;
                pressed = normal;
            }
        }
        if (pressed != null) {
            return new Surface(Kind.WALL, pressed);
        }
        if (previous != null && previous.kind() == Kind.WALL && walls.contains(previous.normal())) {
            return previous;
        }
        if (ceiling) {
            return new Surface(Kind.CEILING, Direction.DOWN);
        }
        return walls.isEmpty() ? Surface.NONE : new Surface(Kind.WALL, walls.get(0));
    }

    /** The thin box, {@code reach} thick, on the {@code side} of his box. */
    private static AABB wallSlab(AABB box, Direction side, double reach) {
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
    private static AABB ceilingSlab(AABB box, double reach) {
        return new AABB(box.minX + 0.1, box.maxY, box.minZ + 0.1, box.maxX - 0.1, box.maxY + reach, box.maxZ - 0.1);
    }

    /**
     * Whether a block he may stick to is within {@code reach} of the surface that his own client says he clings to (the
     * server's copy of him lags the client's a little, so the reach is wider than {@link #REACH}). This is how the server
     * checks a claim, which it never takes on trust.
     */
    public static boolean near(Level level, Player player, Surface claimed, double reach) {
        AABB box = player.getBoundingBox();
        return switch (claimed.kind()) {
            case WALL -> claimed.normal().getAxis().isHorizontal() && blockIn(level, player, wallSlab(box, claimed.normal().getOpposite(), reach));
            case CEILING -> blockIn(level, player, ceilingSlab(box, reach));
            case NONE -> false;
        };
    }

    /** Whether a block he may stick to has a collision shape that reaches into {@code slab}. */
    private static boolean blockIn(Level level, Player player, AABB slab) {
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
                    if (state.isAir() || state.is(ModTags.CANNOT_CLING)) {
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

    // ---- entering ----

    /**
     * Where his movement keys point, as a horizontal unit vector (zero when none is pressed), built from the key axes so
     * that Sneak's scaling of the input does not matter: {@code forward} and {@code strafe} are the signs of W/S and
     * A/D (+1 forward, +1 to the left, as the game's own impulses), {@code yaw} his facing in degrees.
     */
    public static Vec3 wish(int forward, int strafe, float yaw) {
        if (forward == 0 && strafe == 0) {
            return Vec3.ZERO;
        }
        double rad = yaw * Mth.DEG_TO_RAD;
        double sin = Mth.sin((float) rad);
        double cos = Mth.cos((float) rad);
        // Facing is (-sin, cos); the player's left is (cos, sin).
        double x = -sin * forward + cos * strafe;
        double z = cos * forward + sin * strafe;
        double length = Math.sqrt(x * x + z * z);
        return new Vec3(x / length, 0, z / length);
    }

    /** Whether the keys push him into the surface hard enough to count as pressing (a wall only). */
    public static boolean pressesInto(Surface surface, Vec3 wish) {
        return surface.kind() == Kind.WALL && wish.x * -surface.normal().getStepX() + wish.z * -surface.normal().getStepZ() >= PRESS;
    }

    /**
     * Whether he starts to cling to {@code surface} (the checks that do not depend on the surface, such as the power,
     * the toggle, food and being free to move, are the caller's). On a wall he must press into it, and either be in the
     * air or hold Space ("jump at a wall and keep pushing"; walking into a wall on the ground does nothing). Under a
     * ceiling he holds Space. A zip or a swing that ends at a surface needs no input at all and does not ask.
     */
    public static boolean enters(Surface surface, Vec3 wish, boolean onGround, boolean jumping) {
        return switch (surface.kind()) {
            case WALL -> pressesInto(surface, wish) && (!onGround || jumping);
            case CEILING -> jumping;
            case NONE -> false;
        };
    }

    // ---- moving ----

    /** The horizontal unit vector of where he looks (his yaw when he looks straight up or down). */
    private static Vec3 horizontal(Vec3 look, float yaw) {
        double length = Math.sqrt(look.x * look.x + look.z * look.z);
        if (length < 1.0E-4) {
            double rad = yaw * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin((float) rad), 0, Mth.cos((float) rad));
        }
        return new Vec3(look.x / length, 0, look.z / length);
    }

    /** The horizontal unit vector to the camera's right: facing (-sin, cos) turned a quarter to the right. */
    private static Vec3 right(Vec3 forward) {
        return new Vec3(-forward.z, 0, forward.x);
    }

    /**
     * The direction W moves him: {@code F}, of length at most 1. On a wall, with {@code L} the horizontal unit vector in
     * the wall's plane that points to the camera's right, {@code a = look . L} (how far along the wall he looks) and
     * {@code b = look.y}: {@code F = (a L + (b + 0.45) up) / max(|(a, b + 0.45)|, 0.25)}. Looking along the wall, W goes
     * that way; looking at the wall (or up), W climbs; looking down more than about 27 degrees, W climbs down; between
     * them the vector shrinks smoothly to zero (W holds him still) instead of flipping, so there is no jitter. On a
     * ceiling it is the horizontal forward of his yaw: W walks the way he faces, upside down.
     */
    public static Vec3 forwardAxis(Surface surface, Vec3 look, float yaw) {
        if (surface.kind() == Kind.CEILING) {
            return horizontal(look, yaw);
        }
        Vec3 along = along(surface, look, yaw);
        double a = look.dot(along);
        double up = look.y + LOOK_BIAS;
        double divisor = Math.max(Math.sqrt(a * a + up * up), DEAD_ZONE);
        return new Vec3(along.x * a / divisor, up / divisor, along.z * a / divisor);
    }

    /**
     * The direction D moves him: {@code S}, a unit vector in the surface's plane at a right angle to F's direction, so
     * that D goes to his right as he would see it walking along the surface with its normal as his up ({@code F x
     * normal}); the strafe is never stalled by the dead zone of W. On a ceiling, the horizontal right of his yaw.
     */
    public static Vec3 strafeAxis(Surface surface, Vec3 look, float yaw) {
        if (surface.kind() == Kind.CEILING) {
            return right(horizontal(look, yaw));
        }
        Vec3 along = along(surface, look, yaw);
        double a = look.dot(along);
        double up = look.y + LOOK_BIAS;
        double length = Math.sqrt(a * a + up * up);
        // The direction of F, even where its length is nearly zero.
        Vec3 f = length < 1.0E-6 ? along : new Vec3(along.x * a / length, up / length, along.z * a / length);
        Vec3 s = f.cross(surface.normalVector());
        double size = s.length();
        return size < 1.0E-6 ? along : s.scale(1.0 / size);
    }

    /** L: the horizontal unit vector in the wall's plane that points to the camera's right (or any of the two if neither). */
    private static Vec3 along(Surface surface, Vec3 look, float yaw) {
        Direction normal = surface.normal();
        Vec3 tangent = new Vec3(-normal.getStepZ(), 0, normal.getStepX());
        Vec3 right = right(horizontal(look, yaw));
        return tangent.dot(right) < 0 ? tangent.scale(-1) : tangent;
    }

    /**
     * His velocity for one tick of clinging: {@code speed (forward F + strafe S) + (-normal) STICK}. The whole velocity
     * is set each tick, so gravity does not apply. {@code forward} and {@code strafe} are the key axes in -1..1.
     */
    public static Vec3 velocity(Surface surface, Vec3 look, float yaw, float forward, float strafe, double speed) {
        Vec3 f = forwardAxis(surface, look, yaw);
        Vec3 s = strafeAxis(surface, look, yaw);
        Vec3 stick = surface.normalVector().scale(-STICK);
        return f.scale(forward * speed).add(s.scale(strafe * speed)).add(stick);
    }

    // ---- leaving ----

    /**
     * The leap off a surface: {@code d = normalize(look + normal 0.35)}, turned if need be so that it points at least 0.2
     * away from the surface, at {@code leapSpeed} times the square root of his speed factor, plus a little lift. (He
     * cannot stick again for {@link #RESTICK_DELAY} ticks.)
     */
    public static Vec3 leap(Surface surface, Vec3 look, double leapSpeed, double speedFactor) {
        Vec3 normal = surface.normalVector();
        Vec3 d = look.add(normal.scale(0.35)).normalize();
        double away = d.dot(normal);
        if (away < 0.2) {
            // Keep the part along the surface, and put the part away from it at exactly 0.2.
            Vec3 along = d.subtract(normal.scale(away));
            double size = along.length();
            d = size < 1.0E-6 ? normal : along.scale(Math.sqrt(1.0 - 0.04) / size).add(normal.scale(0.2));
        }
        return d.scale(leapSpeed * Math.sqrt(speedFactor)).add(0, LEAP_LIFT, 0);
    }

    /**
     * Whether he walks up onto the top of the block he was climbing: his hands lost the wall while he climbed up, and the
     * block's top is within {@link #MANTLE_HEIGHT} of his feet (a gentle push into the former wall then walks him onto
     * it, 0.2 a tick for up to 6 ticks, with no more upward speed).
     *
     * @param topAboveFeet the top of the block in front of him minus the height of his feet, or NaN if there is none
     */
    public static boolean mantles(boolean climbingUp, boolean contactLost, double topAboveFeet) {
        return climbingUp && contactLost && !Double.isNaN(topAboveFeet) && Math.abs(topAboveFeet) <= MANTLE_HEIGHT;
    }

    /**
     * The top of the block he was climbing, above his feet, or NaN: the highest collision top in the column of the cell
     * {@code wall} looks away from him ({@code -normal} of the surface), within a block above his feet.
     */
    public static double topAboveFeet(Level level, Player player, Surface wall) {
        if (wall.kind() != Kind.WALL) {
            return Double.NaN;
        }
        Vec3 feet = player.position();
        BlockPos column = BlockPos.containing(feet.x - wall.normal().getStepX() * 0.5, feet.y, feet.z - wall.normal().getStepZ() * 0.5);
        double best = Double.NaN;
        for (int dy = -1; dy <= 1; dy++) {
            BlockPos pos = column.offset(0, dy, 0);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.of(player));
            if (!shape.isEmpty()) {
                double top = pos.getY() + shape.max(Direction.Axis.Y) - feet.y;
                if (top <= 1.0 && (Double.isNaN(best) || top > best)) {
                    best = top;
                }
            }
        }
        return best;
    }

    private SpiderClimb() {
    }
}
