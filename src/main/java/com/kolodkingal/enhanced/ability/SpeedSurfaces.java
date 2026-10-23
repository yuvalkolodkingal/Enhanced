package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SpeedConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.mixin.ServerGamePacketListenerImplAccessor;
import com.kolodkingal.enhanced.network.SurfaceRunPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of a speedster running on walls and ceilings ({@link SurfaceRun}). The movement is the player's own
 * client's, as everywhere in Minecraft (vanilla lets a client move itself and checks it: a move into a block or too far
 * is refused); the server decides everything else. His client says what he runs on ({@link SurfaceRunPayload}); the
 * server believes it only for a speedster who may run on surfaces now ({@link #mayRun}) with a block within
 * {@link #CLAIM_REACH} of that side of him, and then:
 * <ul>
 *     <li>raises {@link Live#SURFACE_WALL} or {@link Live#SURFACE_CEILING}, which every client draws (the body turned
 *     to the surface);</li>
 *     <li>resets the "flying is not enabled" kick counter (so a server without allow-flight never kicks him) and the
 *     fall distance;</li>
 *     <li>spares him the fall after the run ends, until he next stands on the ground or in water: dropping off a wall
 *     or a ceiling (when he slows down) never hurts.</li>
 * </ul>
 * A client that claims a run it may not have gets none of it: no flag, no protection, and vanilla's own kick.
 */
public final class SpeedSurfaces {
    /** What his client says counts for this many ticks (it says it again every few while he runs). */
    public static final int REPORT_TICKS = 12;
    /** How near a block must be to the side his client says he runs on (the server's copy of him lags a little). */
    public static final double CLAIM_REACH = 0.6;
    /** The protection from a fall after a run lasts at most this long (ten seconds), landed or not. */
    public static final int SAFE_FALL_TICKS = 200;

    private static final class State {
        SurfaceRun.Surface reported = SurfaceRun.Surface.NONE;
        long reportedAt = Long.MIN_VALUE / 2;
        /** Until when (game time) a fall does not count; 0 when he has landed. */
        long safeUntil;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** Whether this hero is a speedster (Quicksilver or Tommy). */
    public static boolean isSpeedster(Power power) {
        return power == Power.QUICKSILVER || power == Power.TOMMY;
    }

    /**
     * 3.4: whether this player runs on walls as a speedster: Quicksilver or Tommy, or a custom hero with Quicksilver's
     * Wall &amp; Water Run or Tommy's Wall Run (the Sneak variant of his slow time, so not when that is switched off).
     */
    public static boolean isSpeedster(PowerData data) {
        return isSpeedster(data.power()) || quicksilverWalls(data) || tommyWalls(data);
    }

    private static boolean quicksilverWalls(PowerData data) {
        return data.power() == Power.CUSTOM && data.has(HeroAbility.QUICKSILVER_WALL_RUN);
    }

    private static boolean tommyWalls(PowerData data) {
        HeroKit kit = data.kit();
        if (data.power() != Power.CUSTOM || kit == null || !kit.has(HeroAbility.TOMMY_TIME)) {
            return false;
        }
        return kit.hasVariant(HeroAbility.TOMMY_TIME); // (3.5: or Tommy's Wall Run picked as a card of its own)
    }

    /** Whether his toggle for running on walls is on: Quicksilver's Wall &amp; Water Run, Tommy's Wall Run. */
    public static boolean toggleOn(PowerData data) {
        return switch (data.power()) {
            case QUICKSILVER -> data.wallRun();
            case TOMMY -> !data.toggle(Toggle.TOMMY_WALL_RUN_OFF);
            case CUSTOM -> quicksilverWalls(data) && data.wallRun() || tommyWalls(data) && !data.toggle(Toggle.TOMMY_WALL_RUN_OFF);
            default -> false;
        };
    }

    /** Whether running on walls is on in the config. */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || SpeedConfig.SURFACE_RUN.get();
    }

    /** Whether ceilings and overhangs may be run along (the config). */
    public static boolean ceilings() {
        return !EnhancedConfig.SPEC.isLoaded() || SpeedConfig.CEILING_RUN.get();
    }

    /**
     * Whether he may run on a wall or a ceiling now: a speedster with the toggle on, the config on, and nothing that
     * holds or carries him (a vehicle, flight, an elytra, Mjolnir's pin, telekinesis, Sokovia). The speed itself is the
     * client's to keep (below the minimum he drops off by the rule); the server does not need it to check a claim.
     */
    public static boolean mayRun(Player player, PowerData data) {
        return isSpeedster(data) && toggleOn(data) && enabled() && !player.isSpectator() && !player.isPassenger()
                && !player.getAbilities().flying && !player.isFallFlying() && !data.is(Live.PINNED) && !data.is(Live.LIFTED)
                && !player.isInWaterOrBubble() && !player.isInLava();
    }

    /** The speed of a run on a surface for this player now (blocks per tick), from his movement speed and the config. */
    public static double targetSpeed(Player player) {
        double factor = EnhancedConfig.SPEC.isLoaded() ? SpeedConfig.SURFACE_SPEED_FACTOR.get() : 0.8;
        double max = EnhancedConfig.SPEC.isLoaded() ? SpeedConfig.SURFACE_MAX_SPEED.get() : 1.0;
        return SurfaceRun.targetSpeed(player.getSpeed(), factor, max);
    }

    /** Below this speed (blocks per tick) he drops off a surface. */
    public static double minSpeed() {
        return EnhancedConfig.SPEC.isLoaded() ? SpeedConfig.SURFACE_MIN_SPEED.get() : 0.22;
    }

    /**
     * His own client's word ({@code kind} 0 nothing, 1 a wall, 2 a ceiling; {@code normal} the 3D data value of the
     * direction the surface faces). Only a speedster's is heard.
     */
    public static void onReport(ServerPlayer player, int kind, int normal) {
        if (!isSpeedster(Powers.get(player))) {
            return;
        }
        State state = state(player);
        state.reportedAt = player.serverLevel().getGameTime();
        Direction direction = Direction.from3DDataValue(Math.floorMod(normal, 6));
        if (kind == SurfaceRunPayload.WALL && direction.getAxis().isHorizontal()) {
            state.reported = SurfaceRun.Surface.wall(direction);
        } else if (kind == SurfaceRunPayload.CEILING) {
            state.reported = SurfaceRun.Surface.CEILING;
        } else {
            state.reported = SurfaceRun.Surface.NONE;
        }
    }

    /** Once per tick of a speedster (from his hero's tick): the flags, the kick counter and the fall. */
    public static void tick(ServerPlayer player, PowerData data) {
        State state = state(player);
        long now = player.serverLevel().getGameTime();
        SurfaceRun.Surface told = now - state.reportedAt <= REPORT_TICKS ? state.reported : SurfaceRun.Surface.NONE;
        boolean on = told.touches() && mayRun(player, data) && (told.kind() != SurfaceRun.Kind.CEILING || ceilings())
                && SurfaceRun.near(player.level(), player, told, CLAIM_REACH);
        data.set(Live.SURFACE_WALL, on && told.kind() == SurfaceRun.Kind.WALL);
        data.set(Live.SURFACE_CEILING, on && told.kind() == SurfaceRun.Kind.CEILING);
        if (on) {
            state.safeUntil = now + SAFE_FALL_TICKS;
            if (player.connection != null) {
                ((ServerGamePacketListenerImplAccessor) player.connection).enhanced$setAboveGroundTickCount(0);
            }
            // His client moves him: the server's copy must not gather a fall of its own between two positions.
            Vec3 motion = player.getDeltaMovement();
            if (motion.y < 0.0) {
                player.setDeltaMovement(motion.x, 0.0, motion.z);
            }
        }
        if (state.safeUntil > 0) {
            player.resetFallDistance();
            if (!on && (player.onGround() || player.isInWaterOrBubble() || now > state.safeUntil)) {
                state.safeUntil = 0;
            }
        }
    }

    /** Whether a fall does not count for him now (he runs on a surface, or has not landed since). */
    public static boolean fallSafe(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.safeUntil > 0;
    }

    /** The power is taken away, or the player leaves: he runs on nothing. */
    public static void end(Player player, PowerData data) {
        STATES.remove(player.getUUID());
        data.set(Live.SURFACE_WALL, false);
        data.set(Live.SURFACE_CEILING, false);
    }

    public static void clear() {
        STATES.clear();
    }

    private SpeedSurfaces() {
    }
}
