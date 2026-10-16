package com.kolodkingal.enhanced.road;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a trial tells the framework about the course it drew: where the player starts, where he is put back (the
 * checkpoints), how far he may fall, where he wins and where the gate to the next trial stands. Everything is in absolute
 * coordinates of the Road dimension. A trial whose rules are not "get from here to there" leaves {@code finish} out and
 * calls {@link RoadTrial#succeed()} itself.
 *
 * @param checkpoints  the places the player is put back at, in the order they are reached; the first is the start
 *                     (its zone is ignored). The saved checkpoint of a player is an index into this list.
 * @param finish       the zone that wins the trial when the player is in it (null: the trial decides by itself)
 * @param gate         the gate to the next trial, sealed until the trial is won (null: the trial has no gate)
 * @param exitDoor     the Witches' Road door set in the floor where the player can leave the Road (null: none)
 * @param timeLimitTicks the time allowed from the start to the finish (0: none); when it runs out the player is put back
 *                     at the start
 */
public record RoadLayout(List<Checkpoint> checkpoints, @Nullable AABB finish, @Nullable Gate gate, @Nullable BlockPos exitDoor,
                         int timeLimitTicks) {

    public RoadLayout {
        if (checkpoints.isEmpty()) {
            throw new IllegalArgumentException("A layout needs a start");
        }
        checkpoints = List.copyOf(checkpoints);
    }

    /**
     * A place the player is put back at.
     *
     * @param respawn where he stands (feet)
     * @param yaw     which way he looks
     * @param zone    the zone that makes this the player's checkpoint when he is in it (null for the start)
     * @param failY   he is put back at this checkpoint when he falls below this height
     */
    public record Checkpoint(Vec3 respawn, float yaw, @Nullable AABB zone, double failY) {
    }

    /**
     * The gate to the next trial: a frame of amethyst with an opening three blocks wide and three high, standing on
     * {@code base} (the middle of its floor) across the given axis. {@link RoadPalette} draws it.
     */
    public record Gate(BlockPos base, Direction.Axis axis) {
        /** The opening: whoever stands in it has stepped through. */
        public AABB zone() {
            double x = base.getX() + 0.5;
            double z = base.getZ() + 0.5;
            boolean alongX = axis == Direction.Axis.X;
            return new AABB(x - (alongX ? 1.2 : 0.7), base.getY(), z - (alongX ? 0.7 : 1.2),
                    x + (alongX ? 1.2 : 0.7), base.getY() + 3.0, z + (alongX ? 0.7 : 1.2));
        }
    }

    public Vec3 start() {
        return checkpoints.get(0).respawn();
    }

    /** The checkpoint with this index, or the start if there is no such one (a saved index from another course). */
    public Checkpoint checkpoint(int index) {
        return checkpoints.get(index < 0 || index >= checkpoints.size() ? 0 : index);
    }
}
