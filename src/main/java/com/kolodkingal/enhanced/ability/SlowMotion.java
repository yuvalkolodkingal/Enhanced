package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.SlowedProjectilePayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.Shelters;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Quicksilver Time (and, since 3.3, Tommy's slow time): while it is on, everything near the speedster runs several times
 * slower, whether he runs or not. (Super Speed slows nothing: only {@link PowerData#slowMo()}, which just the slow time
 * of a speedster sets, counts here.)
 *
 * <ul>
 *     <li>Creatures and most other entities only get every Nth tick (on the server; clients smooth the movement).</li>
 *     <li>Arrows and thrown projectiles keep ticking but fly at 1/N speed, so they visibly hang in the air. Their air drag
 *     is spread over the slowed time, and they get their full speed back the moment they hit something, so they hit
 *     exactly as hard as they would have.</li>
 *     <li>Other players can't be slowed tick by tick (their game moves them), so they get heavy Slowness.</li>
 * </ul>
 * Works the same on both sides so clients and server agree on what is slowed.
 */
public final class SlowMotion {
    private static final String SLOWED_TAG = "enhanced:slowed";

    private record Zones(long gameTime, List<Vec3> centers, List<Player> speedsters) {
    }

    private static final Map<Level, Zones> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    /** Projectiles currently flying at reduced speed (server only). */
    private static final Set<Projectile> SLOWED = Collections.newSetFromMap(new WeakHashMap<>());

    public static int factor() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.SLOW_MOTION_FACTOR.get() : 5;
    }

    public static double radius() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.SLOW_MOTION_RADIUS.get() : 32;
    }

    /** Positions of every Quicksilver in this level whose Quicksilver Time is running (cached per tick). */
    public static List<Vec3> zones(Level level) {
        long time = level.getGameTime();
        Zones cached = CACHE.get(level);
        if (cached != null && cached.gameTime == time) {
            return cached.centers;
        }
        List<Vec3> centers = new ArrayList<>(1);
        List<Player> speedsters = new ArrayList<>(1);
        for (Player player : level.players()) {
            PowerData data = Powers.get(player);
            if (slowsTime(data) && player.isAlive()) {
                centers.add(player.position());
                speedsters.add(player);
            }
        }
        CACHE.put(level, new Zones(time, centers, speedsters));
        return centers;
    }

    /**
     * Whether this hero's slow time is running: Quicksilver Time, or (3.3) Tommy's slow time; 3.4: or a custom hero's with
     * one of the two.
     */
    public static boolean slowsTime(PowerData data) {
        return slowTimeOf(data) != Power.NONE && data.slowMo();
    }

    /** Whose slow time this player has: Quicksilver's, Tommy's (3.4: a custom hero's with it too), or {@link Power#NONE}. */
    public static Power slowTimeOf(PowerData data) {
        return data.has(HeroAbility.QUICKSILVER_TIME) ? Power.QUICKSILVER : data.has(HeroAbility.TOMMY_TIME) ? Power.TOMMY : Power.NONE;
    }

    /**
     * Whose slow time holds this point: the power of the nearest speedster whose zone it is in (the screen shade and the
     * world's tint take his colours), or {@link Power#NONE}.
     */
    /**
     * 3.5: the player whose slowed time holds {@code pos} (the nearest, as {@link #zoneOwner}), or null: the overlay takes
     * the colour his pick chose.
     */
    @Nullable
    public static Player zoneHolder(Level level, Vec3 pos) {
        double r2 = radius() * radius();
        Player holder = null;
        double best = Double.MAX_VALUE;
        for (Player player : level.players()) {
            double d = player.position().distanceToSqr(pos);
            if (slowsTime(Powers.get(player)) && player.isAlive() && d < r2 && d < best) {
                best = d;
                holder = player;
            }
        }
        return holder;
    }

    public static Power zoneOwner(Level level, Vec3 pos) {
        double r2 = radius() * radius();
        Power owner = Power.NONE;
        double best = Double.MAX_VALUE;
        for (Player player : level.players()) {
            PowerData data = Powers.get(player);
            double d = player.position().distanceToSqr(pos);
            if (slowsTime(data) && player.isAlive() && d < r2 && d < best) {
                best = d;
                owner = slowTimeOf(data);
            }
        }
        return owner;
    }

    /**
     * Whether Quicksilver Time slows what is at {@code pos}: it is within the radius of a running zone, and (3.3) not behind
     * the wall of a Hex that keeps that Quicksilver out.
     */
    public static boolean isInZone(Level level, Vec3 pos) {
        List<Vec3> zones = zones(level);
        if (zones.isEmpty()) {
            return false;
        }
        double r2 = radius() * radius();
        List<Player> speedsters = CACHE.get(level).speedsters();
        for (int i = 0; i < zones.size(); i++) {
            Vec3 center = zones.get(i);
            if (center.distanceToSqr(pos) < r2 && !Shelters.keepsOut(level, speedsters.get(i), center.add(0, 1, 0), pos, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How many times slower Quicksilver Time makes this entity: the zone's factor, unless it is a boss or a minion that
     * says otherwise ({@link BossHooks#slowFactor}: Ultron's bodies are not slowed at all, Thanos's half).
     */
    private static int factorOf(Entity entity) {
        return entity instanceof BossHooks boss ? boss.slowFactor(factor()) : factor();
    }

    /** Whether this entity is slowed by Quicksilver Time at all (players and things they ride are never tick-skipped). */
    private static boolean affected(Entity entity) {
        if (entity instanceof Player || entity instanceof PartEntity<?> || entity.getControllingPassenger() instanceof Player
                || entity.hasPassenger(e -> e instanceof Player)) {
            return false;
        }
        return isInZone(entity.level(), entity.position()) && factorOf(entity) > 1;
    }

    /** Arrows and thrown things are slowed by scaling their speed instead of skipping ticks, so they glide smoothly. */
    private static boolean slowedBySpeed(Entity entity) {
        return entity instanceof AbstractArrow || entity instanceof ThrowableProjectile;
    }

    /** A trident flying back to its owner (Loyalty) rebuilds its speed every tick, so it can't be slowed. */
    private static boolean returning(Projectile projectile) {
        return projectile instanceof AbstractArrow arrow && arrow.isNoPhysics();
    }

    /** How fast time runs for this entity: 1/N for a slowed projectile, otherwise 1. */
    public static double timeScale(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(SLOWED_TAG) ? 1.0 / Math.max(1, data.getCompound(SLOWED_TAG).getInt("Factor")) : 1.0;
    }

    /**
     * Called from {@code EntityTickEvent.Pre} on both sides.
     *
     * @return true if this tick must be skipped
     */
    public static boolean skipTick(Entity entity) {
        if (slowedBySpeed(entity) || !affected(entity)) {
            return false;
        }
        // Clients let creatures tick so their movement between server updates stays smooth.
        if (entity.level().isClientSide && entity instanceof LivingEntity) {
            return false;
        }
        return Math.floorMod(entity.tickCount + entity.getId(), factorOf(entity)) != 0;
    }

    /** Server, once per level tick: slow down projectiles entering the zones, restore the ones that left, slow players. */
    public static void serverTick(ServerLevel level) {
        List<Vec3> zones = zones(level);
        double radius = radius();
        for (Vec3 center : zones) {
            AABB box = new AABB(center, center).inflate(radius);
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box,
                    p -> slowedBySpeed(p) && !returning(p) && !p.getPersistentData().contains(SLOWED_TAG) && !MagnetoAbilities.isHeld(p)
                            && factorOf(p) > 1 && isInZone(level, p.position()))) {
                slow(projectile, factorOf(projectile));
            }
            for (Player player : level.players()) {
                PowerData data = Powers.get(player);
                boolean speedster = slowsTime(data);
                if (!speedster && !player.isSpectator() && !player.isCreative() && player.position().distanceToSqr(center) < radius * radius
                        && isInZone(level, player.position())) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, true, false, false));
                    // Whatever he rides isn't tick-skipped (he steers it), so it is slowed the same way.
                    if (player.getVehicle() instanceof LivingEntity mount) {
                        mount.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, true, false, false));
                    }
                }
            }
        }
        // Restore projectiles that left every zone (or whose Quicksilver Time ended), and let a Loyalty trident that
        // started flying home go at its own pace.
        if (!SLOWED.isEmpty()) {
            List<Projectile> leaving = new ArrayList<>();
            List<Projectile> returningHome = new ArrayList<>();
            for (Projectile projectile : SLOWED) {
                if (projectile.level() != level) {
                    continue;
                }
                if (projectile.isRemoved() || !isInZone(level, projectile.position())) {
                    leaving.add(projectile);
                } else if (returning(projectile)) {
                    returningHome.add(projectile);
                }
            }
            for (Projectile projectile : leaving) {
                SLOWED.remove(projectile);
                if (!projectile.isRemoved()) {
                    restore(projectile);
                }
            }
            for (Projectile projectile : returningHome) {
                SLOWED.remove(projectile);
                release(projectile, false);
            }
        }
    }

    /**
     * Called from {@code EntityTickEvent.Post} on both sides. Vanilla drag (x0.99) runs every tick, and a slowed
     * projectile takes N ticks per normal tick, so the extra drag is given back to keep its speed (and its hit) the same
     * as without the slow motion. It only runs when the projectile really ticked (not while the game is frozen), and the
     * same way on clients (the server tells them which projectiles are slowed), so their copy stays in step.
     */
    public static void afterTick(Entity entity) {
        if (!slowedBySpeed(entity)) {
            return;
        }
        Projectile projectile = (Projectile) entity;
        CompoundTag data = projectile.getPersistentData();
        if (!data.contains(SLOWED_TAG)) {
            return;
        }
        int n = data.getCompound(SLOWED_TAG).getInt("Factor");
        if (n <= 1 || returning(projectile) || projectile.isInWater() || projectile instanceof AbstractArrow arrow && arrow.inGround) {
            return;
        }
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(Math.pow(0.99, 1.0 / n - 1.0)));
    }

    /** Server: a player started seeing a projectile that is already slowed. */
    public static void onStartTracking(Entity entity, ServerPlayer player) {
        if (entity instanceof Projectile projectile && projectile.getPersistentData().contains(SLOWED_TAG)) {
            int factor = projectile.getPersistentData().getCompound(SLOWED_TAG).getInt("Factor");
            PacketDistributor.sendToPlayer(player, new SlowedProjectilePayload(projectile.getId(), factor));
        }
    }

    /** Client: the server says this projectile is slowed (factor > 1) or flies normally again (0). */
    public static void onSlowedSync(Entity entity, int factor) {
        if (factor > 1) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Factor", factor);
            entity.getPersistentData().put(SLOWED_TAG, tag);
        } else {
            entity.getPersistentData().remove(SLOWED_TAG);
        }
    }

    private static void slow(Projectile projectile, int factor) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("NoGravity", projectile.isNoGravity());
        tag.putInt("Factor", factor);
        projectile.getPersistentData().put(SLOWED_TAG, tag);
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(1.0 / factor));
        projectile.hasImpulse = true;
        SLOWED.add(projectile);
        PacketDistributor.sendToPlayersTrackingEntity(projectile, new SlowedProjectilePayload(projectile.getId(), factor));
    }

    /** Puts a slowed projectile back to full speed. Also used when a slowed projectile is loaded from disk. */
    public static void restore(Projectile projectile) {
        release(projectile, true);
    }

    /**
     * Server: takes a projectile out of the slow motion right away, at full speed. Called when it hits something (so
     * the hit is as strong as without the slow motion) and before something else takes over its flight (Magneto's
     * shield). If it is still in a zone afterwards, the next level tick slows it again.
     */
    public static void unslow(Projectile projectile) {
        if (projectile.getPersistentData().contains(SLOWED_TAG)) {
            SLOWED.remove(projectile);
            release(projectile, true);
        }
    }

    private static void release(Projectile projectile, boolean speedUp) {
        CompoundTag tag = projectile.getPersistentData().getCompound(SLOWED_TAG);
        projectile.getPersistentData().remove(SLOWED_TAG);
        if (!projectile.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntity(projectile, new SlowedProjectilePayload(projectile.getId(), 0));
        }
        int factor = Math.max(1, tag.getInt("Factor"));
        projectile.setNoGravity(tag.getBoolean("NoGravity"));
        if (speedUp) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(factor));
        }
        projectile.hasImpulse = true;
    }

    /** A projectile saved to disk while slowed comes back at full speed (it is slowed again if still in a zone). */
    public static void onJoin(Entity entity) {
        if (entity instanceof Projectile projectile && !SLOWED.contains(projectile) && projectile.getPersistentData().contains(SLOWED_TAG)) {
            restore(projectile);
        }
    }

    public static void clear() {
        SLOWED.clear();
        CACHE.clear();
    }

    private SlowMotion() {
    }
}
