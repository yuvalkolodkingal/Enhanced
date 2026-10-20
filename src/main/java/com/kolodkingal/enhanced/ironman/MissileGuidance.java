package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.ironman.nano.HomingMissiles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How a mini-missile steers (3.8, PLAN 6.2, 6.3): {@code IronMissileEntity} asks {@link #steer} every tick for its next
 * motion. A missile nobody guides flies straight (the Mark 1's arm rocket, the Mark 3's shoulder missiles, Falcon's
 * rocket). The Mark 85's homing missiles ({@link HomingMissiles}, lane L4) are {@link #guide guided}: each turns at
 * most {@code missileTurnDegrees} 12° a tick towards its locked target at its speed and picks the nearest hostile when
 * its target dies; the entity's own life ({@code IronMissileEntity.maxLife}) makes it pop after {@code
 * missileHomingTicks} 200 (10 seconds), or after {@link #UNLOCKED_RANGE} blocks for one launched with no lock, which
 * flies straight. The guidance is kept by entity id on the server, forgotten when the missile is gone ({@link #forget},
 * {@link #sweep}) and when the server stops.
 */
public final class MissileGuidance {
    /** How far a missile without a lock flies before it pops (PLAN 6.2: 32 straight). */
    public static final double UNLOCKED_RANGE = 32.0;

    /**
     * One guided missile's homing: whose it is, its target (an entity id; -1 never had one), when it left, its turn.
     */
    public static final class Homing {
        final UUID owner;
        int target;
        final long launched;
        final int homingTicks;
        final double turnRadians;
        final double speed;

        Homing(UUID owner, int target, long launched, int homingTicks, double turnDegrees, double speed) {
            this.owner = owner;
            this.target = target;
            this.launched = launched;
            this.homingTicks = homingTicks;
            this.turnRadians = Math.toRadians(turnDegrees);
            this.speed = speed;
        }

        public UUID owner() {
            return owner;
        }

        public int target() {
            return target;
        }

        public long launched() {
            return launched;
        }
    }

    private static final Map<Integer, Homing> GUIDED = new HashMap<>();

    /** The missile's next motion. Straight on unless {@link #guide guided}. */
    public static Vec3 steer(Entity missile) {
        Vec3 motion = missile.getDeltaMovement();
        Homing homing = GUIDED.get(missile.getId());
        if (homing == null || !(missile.level() instanceof ServerLevel level)) {
            return motion;
        }
        if (level.getGameTime() - homing.launched >= homing.homingTicks) {
            return motion;
        }
        LivingEntity target = target(level, homing);
        if (target == null && homing.target >= 0) {
            // Its target is gone: the nearest other hostile within reach of the missile.
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(homing.owner);
            target = owner == null ? null : HomingMissiles.retarget(owner, missile);
            homing.target = target == null ? -2 : target.getId();
        }
        if (target == null) {
            return motion.lengthSqr() < 1.0E-8 ? motion : motion.normalize().scale(homing.speed);
        }
        Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(missile.position());
        return turn(motion, aim, homing.turnRadians, homing.speed);
    }

    /** {@code motion} turned towards {@code aim} by at most {@code maxRadians}, at {@code speed} (pure: tested). */
    public static Vec3 turn(Vec3 motion, Vec3 aim, double maxRadians, double speed) {
        if (aim.lengthSqr() < 1.0E-8) {
            return motion;
        }
        Vec3 want = aim.normalize();
        if (motion.lengthSqr() < 1.0E-8) {
            return want.scale(speed);
        }
        Vec3 have = motion.normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, have.dot(want))));
        if (angle <= maxRadians) {
            return want.scale(speed);
        }
        // "have" turned towards "want" in their common plane by maxRadians.
        Vec3 axis = have.cross(want);
        if (axis.lengthSqr() < 1.0E-10) {
            axis = have.cross(new Vec3(0.0, 1.0, 0.0));
            if (axis.lengthSqr() < 1.0E-10) {
                axis = new Vec3(1.0, 0.0, 0.0);
            }
        }
        Vec3 ortho = axis.normalize().cross(have).normalize();
        return have.scale(Math.cos(maxRadians)).add(ortho.scale(Math.sin(maxRadians))).normalize().scale(speed);
    }

    /** A Mark 85 missile leaves: it homes on {@code target} (or flies straight when null) for {@code homingTicks}. */
    public static void guide(Entity missile, ServerPlayer owner, @Nullable LivingEntity target, int homingTicks,
            double turnDegrees, double speed) {
        GUIDED.put(missile.getId(), new Homing(owner.getUUID(), target == null ? -1 : target.getId(),
                missile.level().getGameTime(), homingTicks, turnDegrees, speed));
    }

    /** Whether a missile is guided (a Mark 85's). */
    public static boolean guided(Entity missile) {
        return GUIDED.containsKey(missile.getId());
    }

    /** The guided missile's homing, or null. */
    @Nullable
    public static Homing homing(Entity missile) {
        return GUIDED.get(missile.getId());
    }

    /** The creature a guided missile is after now, or null. */
    @Nullable
    public static LivingEntity target(Entity missile) {
        Homing homing = GUIDED.get(missile.getId());
        return homing == null || !(missile.level() instanceof ServerLevel level) ? null : target(level, homing);
    }

    @Nullable
    private static LivingEntity target(ServerLevel level, Homing homing) {
        if (homing.target < 0) {
            return null;
        }
        Entity e = level.getEntity(homing.target);
        return e instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    /** The missile is gone (it hit, popped, was unloaded). */
    public static void forget(Entity missile) {
        GUIDED.remove(missile.getId());
    }

    /** The guided missiles of this level that no longer exist are forgotten (called now and then). */
    public static void sweep(ServerLevel level) {
        GUIDED.entrySet().removeIf(entry -> {
            Entity e = level.getEntity(entry.getKey());
            return e == null ? level.getGameTime() - entry.getValue().launched > 400 : e.isRemoved();
        });
    }

    public static void clear() {
        GUIDED.clear();
    }

    private MissileGuidance() {
    }
}
