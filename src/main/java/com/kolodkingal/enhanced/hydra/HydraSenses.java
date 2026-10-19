package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * How a Hydra soldier notices an intruder (3.6, the plan's 15.3): he sees a player in a 120-degree cone in front of
 * his face within {@code [hydra] sightRange} (24), with nothing in the way; a sneaking player only within half of it
 * ({@code sneakFactor}), one in the dark (block light under 4 at night) within three quarters. Creative and Spectator
 * players, invisible ones, Loki's illusions and Doctor Strange's astral spirit are never seen. He also hears a block
 * broken, an arrow landing or a fight within 8 blocks and goes to look.
 */
public final class HydraSenses {
    /** Half the angle of his sight (degrees). */
    public static final double HALF_CONE = 60.0;
    /** How far he hears (blocks). */
    public static final double HEARING = 8.0;
    /** How dark it must be (block light) for the darkness to shorten his sight at night. */
    public static final int DARK_LIGHT = 4;

    /** His sight (blocks): the server's option, or its default before the config is loaded. */
    public static double sightRange() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.SIGHT_RANGE.get() : 24.0;
    }

    /** What a sneaking intruder leaves of his sight. */
    public static double sneakFactor() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.SNEAK_FACTOR.get() : 0.5;
    }

    /**
     * How far he sees an intruder who sneaks or not, in the dark or not (a pure rule, for the tests): {@code range},
     * times {@code sneakFactor} if he sneaks, times 3/4 in the dark.
     */
    public static double reach(double range, boolean sneaking, double sneakFactor, boolean dark) {
        return range * (sneaking ? sneakFactor : 1.0) * (dark ? 0.75 : 1.0);
    }

    /**
     * Whether a point lies in the cone of a face turned {@code yawDegrees} (Minecraft's yaw: 0 looks south, -90
     * east) at {@code eye}: within {@link #HALF_CONE} of the way he looks, measured flat.
     */
    public static boolean inCone(double yawDegrees, Vec3 eye, Vec3 point) {
        double dx = point.x - eye.x;
        double dz = point.z - eye.z;
        if (dx * dx + dz * dz < 1.0E-4) {
            return true;
        }
        double toward = Math.toDegrees(Math.atan2(-dx, dz));
        return Math.abs(Mth.wrapDegrees(toward - yawDegrees)) <= HALF_CONE;
    }

    /** Whether nobody may be seen there: Creative, Spectator, invisible, an illusion, an astral spirit. */
    public static boolean unseeable(LivingEntity target) {
        if (!target.isAlive() || target.isInvisible() || target instanceof LokiIllusionEntity) {
            return true;
        }
        if (target instanceof Player player) {
            return player.isCreative() || player.isSpectator() || Powers.get(player).is(Live.STRANGE_ASTRAL);
        }
        return false;
    }

    /** Whether the light where {@code target} stands is the dark that shortens a soldier's sight. */
    public static boolean inTheDark(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || level.isDay()) {
            return false;
        }
        BlockPos at = BlockPos.containing(target.getEyePosition());
        return level.getBrightness(LightLayer.BLOCK, at) < DARK_LIGHT;
    }

    /** Whether {@code looker} sees {@code target} now: in his cone, within his reach for it, nothing in the way. */
    public static boolean sees(Mob looker, LivingEntity target) {
        if (unseeable(target) || target.level() != looker.level()) {
            return false;
        }
        boolean sneaking = target.isShiftKeyDown() || target.isCrouching();
        double reach = reach(sightRange(), sneaking, sneakFactor(), inTheDark(target));
        if (looker.distanceToSqr(target) > reach * reach) {
            return false;
        }
        if (!inCone(looker.getYHeadRot(), looker.getEyePosition(), target.getEyePosition())) {
            return false;
        }
        return looker.hasLineOfSight(target);
    }

    /**
     * Something made a noise at {@code pos} (a block broken, an arrow landing, a fight): every soldier within {@link
     * #HEARING} blocks who is not fighting goes to look.
     */
    public static void heard(ServerLevel level, Vec3 pos) {
        List<HydraSoldierEntity> near = level.getEntitiesOfClass(HydraSoldierEntity.class,
                new net.minecraft.world.phys.AABB(pos, pos).inflate(HEARING),
                s -> s.distanceToSqr(pos) <= HEARING * HEARING);
        for (HydraSoldierEntity soldier : near) {
            soldier.alertTo(pos, false);
        }
    }

    private HydraSenses() {
    }
}
