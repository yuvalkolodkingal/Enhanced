package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.mixin.AbstractArrowAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Spider-Man's spider-sense (DESIGN section 14): a warning of danger, no more. It finds the things about to hurt him: a
 * projectile in flight whose path comes close, a mob about to strike, a fuse burning, a player about to hit him. The
 * server scans every few ticks; a threat he was not warned about in the last 40 ticks raises his "sensing" pose (everyone
 * sees his head snap round) and is sent to him alone. Nothing it finds changes what happens.
 */
public final class SpiderSense {
    /** A threat is not warned about again for this many ticks. */
    public static final int MEMORY_TICKS = 40;
    /** How close (blocks) a projectile's path must pass his body to count. */
    public static final double PASSING_DISTANCE = 1.2;
    /** How far ahead (ticks) a projectile's path is looked at. */
    public static final int HORIZON = 25;
    /** A hit that is nearer than this many ticks makes the warning strong. */
    public static final int STRONG_TICKS = 12;
    /** Mobs about to strike are looked for this near (blocks). */
    public static final double STRIKE_RANGE = 6.0;
    /** Creepers about to blow, and primed TNT, this near. */
    public static final double CREEPER_RANGE = 7.0;
    public static final double TNT_RANGE = 8.0;
    /** TNT with at most this long of fuse left is a threat. */
    public static final int TNT_FUSE = 40;
    /** Players about to hit him, this near. */
    public static final double PLAYER_RANGE = 4.0;
    /** The most threats one warning names (what the payload carries). */
    public static final int MAX_THREATS = 64;

    /** A danger: the entity, and whether it can hit within {@link #STRONG_TICKS}. */
    public record Threat(Entity entity, boolean strong) {
    }

    /**
     * The tick on which a straight path from {@code position} with {@code velocity} (per tick) comes closest to {@code
     * body}, within {@code horizon} ticks, and how close it gets: {@code {ticks, distance}}. A path that is not
     * approaching (or not moving) answers an infinite distance.
     */
    public static double[] closestApproach(Vec3 position, Vec3 velocity, Vec3 body, int horizon) {
        Vec3 toBody = body.subtract(position);
        double speedSquared = velocity.lengthSqr();
        double along = toBody.dot(velocity);
        if (speedSquared < 1.0E-4 || along <= 0.0) {
            return new double[]{0.0, Double.POSITIVE_INFINITY};
        }
        double ticks = Mth.clamp(along / speedSquared, 0.0, horizon);
        return new double[]{ticks, toBody.subtract(velocity.scale(ticks)).length()};
    }

    /**
     * Everything near {@code spider} that is about to hurt him, within {@code range} blocks. The strike and fuse ranges
     * are the fixed ones above times {@code scale} (the Black Eyes widen all of it), capped by {@code range}.
     */
    public static List<Threat> threats(ServerPlayer spider, double range, double scale) {
        return threats(spider, range, scale, false);
    }

    /**
     * As {@link #threats(ServerPlayer, double, double)}; with {@code everyHostile} every hostile monster in {@code range}
     * is named too (the sensory overload of the Black Eyes: all of them are outlined, not only the ones about to strike).
     */
    public static List<Threat> threats(ServerPlayer spider, double range, double scale, boolean everyHostile) {
        ServerLevel level = spider.serverLevel();
        Vec3 body = spider.position().add(0, spider.getBbHeight() * 0.5, 0);
        List<Threat> found = new ArrayList<>();
        AABB around = spider.getBoundingBox().inflate(range);
        double strike = Math.min(range, STRIKE_RANGE * scale);
        double fuse = Math.min(range, CREEPER_RANGE * scale);
        double tntRange = Math.min(range, TNT_RANGE * scale);
        double playerRange = Math.min(range, PLAYER_RANGE * scale);

        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, around)) {
            if (projectile.getOwner() == spider || projectile instanceof FishingHook
                    || (projectile instanceof AbstractArrow arrow
                    && ((AbstractArrowAccessor) arrow).enhanced$isInGround())) {
                continue;
            }
            double[] approach = closestApproach(projectile.position(), projectile.getDeltaMovement(), body, HORIZON);
            if (approach[1] < PASSING_DISTANCE) {
                found.add(new Threat(projectile, approach[0] < STRONG_TICKS));
            }
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, everyHostile ? around : spider.getBoundingBox().inflate(strike))) {
            if (!mob.isAlive() || mob instanceof LokiIllusionEntity) {
                continue; // (Loki's duplicates never hit)
            }
            if (mob.getTarget() == spider && mob.isAggressive() && mob.distanceTo(spider) <= strike) {
                found.add(new Threat(mob, mob.distanceToSqr(spider) < 9.0));
            } else if (everyHostile && mob instanceof Enemy && mob.distanceTo(spider) <= range) {
                found.add(new Threat(mob, false));
            }
        }
        for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, spider.getBoundingBox().inflate(fuse))) {
            if ((creeper.getSwellDir() > 0 || creeper.isIgnited()) && creeper.distanceTo(spider) <= fuse) {
                found.add(new Threat(creeper, true));
            }
        }
        for (PrimedTnt tnt : level.getEntitiesOfClass(PrimedTnt.class, spider.getBoundingBox().inflate(tntRange))) {
            if (tnt.getFuse() <= TNT_FUSE && tnt.distanceTo(spider) <= tntRange) {
                found.add(new Threat(tnt, tnt.getFuse() < STRONG_TICKS));
            }
        }
        if (level.getServer().isPvpAllowed()) {
            for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, spider.getBoundingBox().inflate(playerRange))) {
                if (other != spider && other.isAlive() && !other.isCreative() && !other.isSpectator() && other.canHarmPlayer(spider)
                        && other.distanceTo(spider) <= playerRange && other.swinging
                        && other.getViewVector(1.0F).dot(body.subtract(other.getEyePosition()).normalize()) > 0.8) {
                    found.add(new Threat(other, true));
                }
            }
        }
        return found;
    }

    /**
     * The scan: the threats he has not been warned about in the last {@link #MEMORY_TICKS} ticks. They are remembered as
     * warned from now on.
     */
    static List<Threat> scan(ServerPlayer spider, SpiderState state, double range, double scale, boolean everyHostile) {
        long now = spider.serverLevel().getGameTime();
        state.warned.values().removeIf(at -> now - at >= MEMORY_TICKS);
        List<Threat> fresh = new ArrayList<>();
        for (Threat threat : threats(spider, range, scale, everyHostile)) {
            if (fresh.size() < MAX_THREATS && !state.warned.containsKey(threat.entity().getId())) {
                state.warned.put(threat.entity().getId(), now);
                fresh.add(threat);
            }
        }
        return fresh;
    }

    /**
     * A hook for other powers: warns a Spider-Man of a danger the scan cannot see (a mind-reading threat), as if his
     * spider-sense had found it. Does nothing if he is not Spider-Man or has switched the sense off.
     */
    public static void warn(ServerPlayer spider, Entity source, boolean strong) {
        SpiderServer.warnOf(spider, source, strong);
    }

    private SpiderSense() {
    }
}
