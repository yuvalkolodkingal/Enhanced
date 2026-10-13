package com.kolodkingal.enhanced.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Line-of-sight helpers shared by the server (abilities) and the client (beam rendering), so both agree on what
 * the player is looking at.
 */
public final class Raycasts {

    /**
     * The first entity matching {@code filter} along the player's view, if it is closer than the first solid
     * block.
     *
     * @param inflate extra margin around each entity's hitbox, to make small targets (dropped items) easier to hit
     */
    @Nullable
    public static EntityHitResult entity(Player player, double range, double inflate, Predicate<Entity> filter, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        return entityBetween(player.level(), player, eye, eye.add(look.scale(maxDist)), inflate, filter);
    }

    /** The closest entity matching {@code filter} whose (inflated) hitbox the segment from {@code start} to {@code end} crosses. */
    @Nullable
    public static EntityHitResult entityBetween(Level level, @Nullable Entity except, Vec3 start, Vec3 end, double inflate, Predicate<Entity> filter) {
        AABB search = new AABB(start, end).inflate(1.0 + inflate);
        Entity best = null;
        Vec3 bestHit = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : level.getEntities(except, search, filter)) {
            AABB box = e.getBoundingBox().inflate(e.getPickRadius() + inflate);
            if (box.contains(start)) {
                if (bestDist > 0) {
                    best = e;
                    bestHit = start;
                    bestDist = 0;
                }
                continue;
            }
            Optional<Vec3> hit = box.clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(hit.get());
                if (d < bestDist) {
                    best = e;
                    bestHit = hit.get();
                    bestDist = d;
                }
            }
        }
        return best == null ? null : new EntityHitResult(best, bestHit);
    }

    /** The first solid block along the player's view. */
    public static BlockHitResult block(Player player, double range, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 end = eye.add(player.getViewVector(partialTick).scale(range));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    /**
     * The first block along the player's view whose outline the ray touches, not only its collision shape: a flower, a
     * torch, a rail or a pressure plate is found as what it is, where {@link #block} would pass through it to the ground or
     * the wall behind (the picking ray of Build Magic).
     */
    public static BlockHitResult outline(Player player, double range, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 end = eye.add(player.getViewVector(partialTick).scale(range));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    /**
     * Magneto's aim at a metal block (D-M 3.3): the ray walks cell by cell, and stops in a metal cell ({@link
     * Metal#isMetalBlock}) where it touches the block's outline (a rail, a pressure plate, a chain, a lantern), and in any
     * other cell only where it touches its collision shape. So it passes over a carpet, a snow layer or a bottom slab,
     * through grass, flowers and water, and stops at a wall, which is then the block it found (not metal).
     *
     * @return the block the ray stopped at, or a miss at the end of its range
     */
    public static BlockHitResult metalBlock(Player player, double range, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 end = eye.add(player.getViewVector(partialTick).scale(range));
        CollisionContext context = CollisionContext.of(player);
        return BlockGetter.traverseBlocks(eye, end, player.level(), (level, cell) -> {
            BlockState state = level.getBlockState(cell);
            VoxelShape shape = Metal.isMetalBlock(state) ? state.getShape(level, cell) : state.getCollisionShape(level, cell, context);
            return shape.isEmpty() ? null : shape.clip(eye, end, cell.immutable());
        }, level -> {
            Vec3 back = eye.subtract(end);
            return BlockHitResult.miss(end, Direction.getNearest(back.x, back.y, back.z), BlockPos.containing(end));
        });
    }

    /**
     * Whether the player can see the block at {@code pos}: the line from his eyes to the middle of the cell touches the
     * collision shape of no other cell (air, grass, a carpet or a bottom slab under the line let it through; a wall does
     * not). The walk reads at most {@code 1 + |dx| + |dy| + |dz|} cells ({@code dx}, {@code dy}, {@code dz} from the cell of
     * his eyes to {@code pos}), which a caller with a budget can count beforehand.
     */
    public static boolean inSight(Player player, BlockPos pos) {
        return clear(player, player.getEyePosition(), Vec3.atCenterOf(pos), pos);
    }

    /**
     * Whether the player can see the point {@code to} (another player's eyes): the line from his eyes to it touches no
     * collision shape on the way.
     */
    public static boolean inSight(Player player, Vec3 to) {
        return clear(player, player.getEyePosition(), to, null);
    }

    /** Whether the line from {@code from} to {@code to} touches the collision shape of no cell but {@code target}. */
    private static boolean clear(Player player, Vec3 from, Vec3 to, @Nullable BlockPos target) {
        CollisionContext context = CollisionContext.of(player);
        return BlockGetter.traverseBlocks(from, to, player.level(), (level, cell) -> {
            if (cell.equals(target)) {
                return null; // the block looked at does not hide itself
            }
            BlockState state = level.getBlockState(cell);
            VoxelShape shape = state.getCollisionShape(level, cell, context);
            return !shape.isEmpty() && shape.clip(from, to, cell) != null ? Boolean.FALSE : null;
        }, level -> Boolean.TRUE);
    }

    /** Whatever the player is looking at: the first matching entity, otherwise the first block (or a miss at full range). */
    public static HitResult any(Player player, double range, Predicate<Entity> filter, float partialTick) {
        EntityHitResult entity = entity(player, range, 0.0, filter, partialTick);
        if (entity != null) {
            return entity;
        }
        return block(player, range, partialTick);
    }

    private Raycasts() {
    }
}
