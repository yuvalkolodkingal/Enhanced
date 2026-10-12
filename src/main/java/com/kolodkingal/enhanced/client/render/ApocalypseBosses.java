package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What the client can see of En Sabah Nur from anywhere: the bosses it knows, for the music, the shield, the fog and every
 * effect that follows him. Asked many times a frame, so the answer is kept for the tick. A copy that the model viewer put in
 * the world is not a boss of a fight, but it is drawn like one (the shield of a viewed boss is photographed too).
 */
public final class ApocalypseBosses {
    /** How far the client looks for him: the arena and the margin the music and the bars show at. */
    public static final double REACH = 160.0;

    private static long cachedAt = Long.MIN_VALUE;
    private static List<ApocalypseEntity> cached = List.of();

    /** Every boss the client knows within {@link #REACH} of the camera entity (alive; one or two at most). */
    public static List<ApocalypseEntity> all() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.getCameraEntity() == null) {
            return List.of();
        }
        long now = level.getGameTime();
        if (now == cachedAt) {
            return cached;
        }
        Vec3 from = mc.getCameraEntity().position();
        List<ApocalypseEntity> found = new ArrayList<>(level.getEntitiesOfClass(ApocalypseEntity.class, new AABB(from, from).inflate(REACH), ApocalypseEntity::isAlive));
        cachedAt = now;
        cached = found;
        return found;
    }

    /** The boss nearest to a point, or null. */
    @Nullable
    public static ApocalypseEntity nearest(Vec3 from) {
        ApocalypseEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ApocalypseEntity boss : all()) {
            double distance = boss.position().distanceToSqr(from);
            if (distance < bestDistance) {
                best = boss;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Forgets what was seen (leaving the world). */
    public static void clear() {
        cachedAt = Long.MIN_VALUE;
        cached = List.of();
    }

    private ApocalypseBosses() {
    }
}
