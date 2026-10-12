package com.kolodkingal.enhanced.ultron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How Ultron's body and the Sentries move: they hover, with no gravity and no pathfinding, so each tick the brain says
 * how fast and which way it wants to go and the mob's velocity is steered toward that (the game then moves it, with
 * collision). The ground under a flier is found by looking down from where it is, not by the height map, so it is the
 * ground under a ceiling or a canopy too.
 */
final class UltronMotion {
    /** How far down {@link #groundBelow} looks. */
    private static final int LOOK_DOWN = 96;

    /**
     * The top of the highest solid block at or below {@code y} in the column of {@code (x, z)}, found by looking down
     * from {@code y} (up to 96 blocks); the world's bottom if there is none.
     */
    static double groundBelow(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(y), Math.floor(z));
        int bottom = level.getMinBuildHeight();
        for (int i = 0; i < LOOK_DOWN && pos.getY() > bottom; i++, pos.move(0, -1, 0)) {
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.max(Direction.Axis.Y);
            }
        }
        return bottom;
    }

    /**
     * The surface of the column of {@code (x, z)} near {@code y}: if {@code y} is inside the ground (a hill beside the
     * arena) it first climbs out of it, then looks down for the top of the ground.
     */
    static double surface(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(y), Math.floor(z));
        int top = level.getMaxBuildHeight();
        for (int i = 0; i < 48 && pos.getY() < top && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); i++) {
            pos.move(0, 1, 0);
        }
        return groundBelow(level, x, pos.getY() + 0.5, z);
    }

    /**
     * The ground of the column of {@code (x, z)} near {@code y}, as the ritual judges a site: like {@link #surface}, but a
     * tree is not ground. Logs and leaves are skipped (the island carries a tree, it is not a hill), so a column under
     * a canopy or a trunk has the dirt it grows from for its ground and a forest is not a mountain range.
     */
    static double groundBelowTheTrees(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(y), Math.floor(z));
        int top = level.getMaxBuildHeight();
        for (int i = 0; i < 48 && pos.getY() < top && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); i++) {
            pos.move(0, 1, 0);
        }
        int bottom = level.getMinBuildHeight();
        for (int i = 0; i < LOOK_DOWN && pos.getY() > bottom; i++, pos.move(0, -1, 0)) {
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
                return pos.getY() + shape.max(Direction.Axis.Y);
            }
        }
        return bottom;
    }

    /** The y of the ground under the mob (found from its own height). */
    static double groundUnder(Mob mob) {
        return groundBelow(mob.level(), mob.getX(), mob.getY() + 0.1, mob.getZ());
    }

    /**
     * Steers the mob's velocity toward {@code wanted} (blocks per tick), changing it by at most {@code accel} a tick. The
     * game then moves the mob by it, with collision; the velocity it leaves is what the next tick steers from.
     */
    static void steer(Mob mob, Vec3 wanted, double accel) {
        Vec3 velocity = mob.getDeltaMovement();
        Vec3 diff = wanted.subtract(velocity);
        double length = diff.length();
        if (length > accel) {
            diff = diff.scale(accel / length);
        }
        mob.setDeltaMovement(velocity.add(diff));
    }

    /**
     * {@code velocity} without the part that would carry the mob out of the disc round {@code centre} of the given
     * radius (a mob at the edge slides along it).
     */
    static Vec3 keepInDisc(Vec3 position, Vec3 velocity, Vec3 centre, double radius) {
        Vec3 next = position.add(velocity);
        double dx = next.x - centre.x;
        double dz = next.z - centre.z;
        double distance = Math.hypot(dx, dz);
        if (distance <= radius || distance < 1.0E-6) {
            return velocity;
        }
        // Remove the outward part of the velocity.
        double nx = dx / distance;
        double nz = dz / distance;
        double outward = velocity.x * nx + velocity.z * nz;
        double pull = (distance - radius) * 0.5;
        return new Vec3(velocity.x - nx * (Math.max(0.0, outward) + pull), velocity.y, velocity.z - nz * (Math.max(0.0, outward) + pull));
    }

    /** The unit vector from {@code a} to {@code b} on the ground plane (zero if they are above each other). */
    static Vec3 flatDirection(Vec3 a, Vec3 b) {
        Vec3 flat = new Vec3(b.x - a.x, 0.0, b.z - a.z);
        double length = flat.length();
        return length < 1.0E-6 ? Vec3.ZERO : flat.scale(1.0 / length);
    }

    private UltronMotion() {
    }
}
