package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.thanos.ThanosEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What the client can see of the Mad Titan from anywhere: the nearest one it knows. The boss bar, the music, the fog
 * and the illusions that look like him all ask for it, many times a frame, so the answer is kept for the tick.
 */
public final class ThanosClientView {
    /** How far the client looks for him: the fight's arena and the margin the bar shows at. */
    public static final double REACH = 160.0;

    private static long cachedAt = Long.MIN_VALUE;
    private static Vec3 cachedFrom = Vec3.ZERO;
    @Nullable
    private static ThanosEntity cached;

    /** The Thanos nearest to {@code from} that is alive on this client, or null. */
    @Nullable
    public static ThanosEntity nearestBoss(Entity from) {
        return nearestBoss(from.position());
    }

    /** The Thanos nearest to a point, or null. */
    @Nullable
    public static ThanosEntity nearestBoss(Vec3 from) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        long now = level.getGameTime();
        if (now == cachedAt && cachedFrom.distanceToSqr(from) < 4.0) {
            return cached;
        }
        List<ThanosEntity> found = level.getEntitiesOfClass(ThanosEntity.class, new AABB(from, from).inflate(REACH), boss -> boss.isAlive() && !boss.shownByViewer());
        ThanosEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ThanosEntity boss : found) {
            double distance = boss.position().distanceToSqr(from);
            if (distance < bestDistance) {
                best = boss;
                bestDistance = distance;
            }
        }
        cachedAt = now;
        cachedFrom = from;
        cached = best;
        return best;
    }

    /** Forgets what was seen (leaving the world). */
    public static void clear() {
        cachedAt = Long.MIN_VALUE;
        cached = null;
    }

    private ThanosClientView() {
    }
}
