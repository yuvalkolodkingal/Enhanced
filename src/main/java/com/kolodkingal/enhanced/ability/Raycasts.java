package com.kolodkingal.enhanced.ability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
