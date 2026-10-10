package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Leans a hero's whole body into the way they move, on the render pose stack (so armour, held items, capes and every
 * overlay layer follow, and the name tag stays where it is):
 * <ul>
 *     <li>A flying hero pitches forward in proportion to horizontal speed: upright when hovering, about 60 degrees at
 *     full speed (Thor about 80, flying behind his weapon like Superman). Diving tilts a little further forward,
 *     rising a little back, strafing leans sideways, and turning banks (rolls) up to 15 degrees into the turn.</li>
 *     <li>Quicksilver in Quicksilver Time and Tommy at super speed lean about 20 degrees into the run.</li>
 * </ul>
 * Everything is eased with frame-rate independent exponential smoothing, so nothing snaps; a hero who lands or stops
 * flying eases back to upright. Only what every client knows about every player is used: the position deltas, the
 * body yaw and the synced {@link PowerData}. The delta movement is not, because other players do not have one.
 *
 * <p>{@link com.kolodkingal.enhanced.mixin.client.PlayerRendererMixin} applies the lean where vanilla applies the
 * elytra and swimming pitch; {@link HeroPoses} reads {@link #pitch} so heads and aimed arms keep pointing where the
 * hero looks.
 */
public final class BodyLean {
    /** Forward pitch at full flight speed (degrees); Thor leans further. */
    private static final float FLIGHT_PITCH = 60.0F;
    private static final float THOR_PITCH = 80.0F;
    /** Forward pitch of a speedster at a run. */
    private static final float RUN_PITCH = 20.0F;
    /** How far diving (+) or rising (-) tilts the body on top of the speed lean, at 0.5 blocks per tick. */
    private static final float CLIMB_PITCH = 12.0F;
    /** The farthest a strafing hero leans sideways. */
    private static final float STRAFE_ROLL = 30.0F;
    /** The farthest a turn banks the body. */
    private static final float MAX_BANK = 15.0F;
    /** Degrees of bank per degree of body yaw turned in one tick. */
    private static final float BANK_PER_YAW_RATE = 4.0F;
    /** Height of the pivot: the waist, so head and feet swing around it. */
    private static final float PIVOT_HEIGHT = 0.9F;
    /** Blocks moved in one tick from which it is a teleport. */
    private static final float TELEPORT = 6.0F;
    /** Horizontal blocks per tick a hero ignores (the drift of a hover). */
    private static final float DEAD_ZONE = 0.02F;
    /** Horizontal blocks per tick at which the lean reaches 63 percent of its maximum. */
    private static final float SPEED_SCALE = 0.4F;
    /**
     * Smoothing time constants, in ticks: for the measured motion (other players' positions arrive in jumps), and for
     * the lean itself leaning in, easing back, and banking.
     */
    private static final float TAU_MOTION = 2.0F;
    private static final float TAU_IN = 4.5F;
    private static final float TAU_OUT = 4.0F;
    private static final float TAU_BANK = 3.5F;
    private static final float EPSILON = 0.05F;
    /** A player whose entity is younger than this many ticks has just come into view. */
    private static final int NEW_ENTITY_TICKS = 5;

    private static final Map<UUID, State> STATES = new HashMap<>();
    /** The player the last lean was applied to, and its pitch: what the model animation that follows needs. */
    private static int appliedId = -1;
    private static float appliedPitch;

    private static final class State {
        /** Game time (ticks plus partial tick) of the last update. */
        double time;
        /**
         * The motion, smoothed: blocks per tick forwards and to the right of the body, and up; and degrees the body
         * turns per tick (to the right is positive).
         */
        float forward;
        float aside;
        float climb;
        float turn;
        /** The lean in degrees: the pitch is positive leaning forward, the roll with the right side down. */
        float pitch;
        float roll;
        float targetPitch;
        float targetRoll;
    }

    /**
     * Updates this player's lean for the frame being drawn and applies it to the pose stack, which must be in the
     * entity's frame: already turned to its body yaw, before the model is flipped upright.
     */
    public static void apply(AbstractClientPlayer player, PoseStack poseStack, float yBodyRot, float partialTick) {
        State state = update(player, yBodyRot, partialTick);
        appliedId = player.getId();
        appliedPitch = state == null ? 0.0F : state.pitch;
        if (state != null) {
            rotate(poseStack, state.pitch, state.roll);
        }
    }

    /** Leans the pose stack (in the entity's frame, like {@link #apply}) by the lean last applied to this player. */
    public static void transform(PoseStack poseStack, Player player) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            rotate(poseStack, state.pitch, state.roll);
        }
    }

    /** The forward pitch (radians) just applied to this player's body, or 0. */
    public static float pitch(Player player) {
        return player.getId() == appliedId ? appliedPitch * Mth.DEG_TO_RAD : 0.0F;
    }

    private static void rotate(PoseStack poseStack, float pitch, float roll) {
        if (Math.abs(pitch) < EPSILON && Math.abs(roll) < EPSILON) {
            return;
        }
        poseStack.translate(0.0F, PIVOT_HEIGHT, 0.0F);
        // Rolled about the way the hero faces, then pitched about the body's own sideways axis.
        poseStack.mulPose(Axis.ZP.rotationDegrees(-roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0F, -PIVOT_HEIGHT, 0.0F);
    }

    /** The state to draw this player with, or null when the body is not leaning at all. */
    private static State update(AbstractClientPlayer player, float yBodyRot, float partialTick) {
        // Not in the world: the inventory preview draws the player with a partial tick of 1 and its own made-up turn.
        if (partialTick >= 1.0F) {
            return null;
        }
        UUID id = player.getUUID();
        PowerData data = Powers.get(player);
        if (!data.power().hasPowers() || vanillaTurnsTheBody(player, partialTick)) {
            STATES.remove(id);
            return null;
        }
        double time = player.tickCount + partialTick;
        State state = STATES.get(id);
        // Time going backwards means a new world; a player whose entity has just appeared was already leaning.
        boolean snap = state != null ? time < state.time - 1.5 : player.tickCount < NEW_ENTITY_TICKS;
        boolean first = state == null || snap;
        if (first) {
            state = new State();
            state.time = time;
            STATES.put(id, state);
        }
        float dt = first ? 1.0F : (float) (time - state.time);
        // A frame no later than the last one (two draws in a frame, or the game paused) leaves the lean be.
        if (first || dt > 0.0F) {
            state.time = time;
            measure(player, yBodyRot, state, first ? 0.0F : dt);
            target(player, data, state);
            if (snap) {
                state.pitch = state.targetPitch;
                state.roll = state.targetRoll;
            } else {
                state.pitch = ease(state.pitch, state.targetPitch, dt,
                        Math.abs(state.targetPitch) < Math.abs(state.pitch) ? TAU_OUT : TAU_IN);
                state.roll = ease(state.roll, state.targetRoll, dt, TAU_BANK);
            }
        }
        if (Math.abs(state.pitch) < EPSILON && Math.abs(state.roll) < EPSILON
                && state.targetPitch == 0.0F && state.targetRoll == 0.0F) {
            STATES.remove(id);
            return null;
        }
        return state;
    }

    /** Exponential smoothing that behaves the same at any frame rate: dt and tau are in ticks. */
    private static float ease(float current, float target, float dt, float tau) {
        return current + (target - current) * (1.0F - (float) Math.exp(-dt / tau));
    }

    /** As {@link #ease}, but the first measurement (a dt of 0) is taken as it is. */
    private static float smooth(float current, float measured, float dt, float tau) {
        return dt <= 0.0F ? measured : ease(current, measured, dt, tau);
    }

    /** Vanilla already tilts the body of a dying, sleeping, swimming, gliding or riding player. */
    private static boolean vanillaTurnsTheBody(AbstractClientPlayer player, float partialTick) {
        return player.deathTime > 0 || player.isSleeping() || player.isFallFlying() || player.isAutoSpinAttack()
                || player.isPassenger() || player.getSwimAmount(partialTick) > 0.0F;
    }

    /** Takes this tick's motion of the player, from its position and body yaw, into the smoothed motion. */
    private static void measure(AbstractClientPlayer player, float yBodyRot, State state, float dt) {
        double dx = player.getX() - player.xOld;
        double dy = player.getY() - player.yOld;
        double dz = player.getZ() - player.zOld;
        if (dx * dx + dy * dy + dz * dz > TELEPORT * TELEPORT) {
            // A teleport is not a flight.
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        state.forward = smooth(state.forward, (float) (-dx * Mth.sin(yaw) + dz * Mth.cos(yaw)), dt, TAU_MOTION);
        state.aside = smooth(state.aside, (float) (-dx * Mth.cos(yaw) - dz * Mth.sin(yaw)), dt, TAU_MOTION);
        state.climb = smooth(state.climb, (float) dy, dt, TAU_MOTION);
        state.turn = smooth(state.turn, Mth.wrapDegrees(player.yBodyRot - player.yBodyRotO), dt, TAU_MOTION);
    }

    /** Works out where the body wants to lean for the motion it has, in degrees. */
    private static void target(AbstractClientPlayer player, PowerData data, State state) {
        float speed = Mth.sqrt(state.forward * state.forward + state.aside * state.aside);
        float ahead = speed < 1.0E-4F ? 0.0F : state.forward / speed;
        float side = speed < 1.0E-4F ? 0.0F : state.aside / speed;
        float pitch = 0.0F;
        float roll = 0.0F;
        if (data.flying() && !player.onGround()) {
            float max = data.power() == Power.THOR ? THOR_PITCH : FLIGHT_PITCH;
            float lean = max * (1.0F - (float) Math.exp(-Math.max(speed - DEAD_ZONE, 0.0F) / SPEED_SCALE));
            // Leaning back into a backwards flight, less than forwards.
            pitch = lean * (ahead >= 0.0F ? ahead : ahead * 0.4F);
            roll = Math.min(lean, STRAFE_ROLL) * side;
            pitch -= Mth.clamp(state.climb / 0.5F, -1.0F, 1.0F) * CLIMB_PITCH;
            pitch = Mth.clamp(pitch, -CLIMB_PITCH * 2.0F, 88.0F);
            // Turning banks into the turn, but only if the hero is going somewhere.
            float going = Mth.clamp(speed / 0.3F, 0.0F, 1.0F);
            roll += Mth.clamp(state.turn * BANK_PER_YAW_RATE, -MAX_BANK, MAX_BANK) * going;
        } else if (QuicksilverAbilities.isRunning(player) || TommyAbilities.isRunning(player)) {
            pitch = RUN_PITCH * Mth.clamp(state.forward / 0.3F, 0.0F, 1.0F);
        }
        if (data.power() == Power.THOR && (data.is(Live.CALLING_LIGHTNING) || data.is(Live.BIFROST))) {
            // His weapon is raised to the sky, or his arms spread to the Bifrost: upright.
            pitch = 0.0F;
            roll = 0.0F;
        }
        state.targetPitch = pitch;
        state.targetRoll = roll;
    }

    /** Forgets players who left or are no longer in this world. */
    public static void tick(ClientLevel level) {
        STATES.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
    }

    public static void clear() {
        STATES.clear();
        appliedId = -1;
        appliedPitch = 0.0F;
    }

    private BodyLean() {
    }
}
