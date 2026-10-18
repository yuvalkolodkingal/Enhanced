package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SpeedSurfaces;
import com.kolodkingal.enhanced.ability.SurfaceRun;
import com.kolodkingal.enhanced.network.SurfaceRunPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A speedster's own run up walls and along ceilings (3.3), done by the player's own client like Spider-Man's cling,
 * because vanilla lets a client move itself and the server only checks. The rules are {@link SurfaceRun}'s, the same
 * code the server and the game tests run: at the start of every tick of the local Quicksilver or Tommy the step gives
 * the velocity of the tick (so gravity does not apply), and the keys that would move him are kept out of the game's
 * own movement while it does (the forward key only so faintly that the sprint, and with it the run, goes on). The
 * camera is turned round over a few ticks when the run reverses his way (onto a ceiling, round the lip of an
 * overhang). What he runs on is told to the server ({@link SurfaceRunPayload}), which shows it to everyone.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SpeedsterMotion {
    /** What he runs on is told to the server again this often while it lasts (it believes it for 12 ticks). */
    private static final int REPORT_TICKS = 6;
    /** The camera's turn at a reversal, in degrees per tick. */
    private static final float TURN_RATE = 30.0F;
    /** The forward impulse left in the game's input while the run owns his velocity: enough to keep the sprint. */
    private static final float KEEP_SPRINT = 1.0E-3F;

    private static final SurfaceRun.Runner RUNNER = new SurfaceRun.Runner();
    private static LocalPlayer owner;
    private static boolean jumpWas;
    private static int toldKind = -1;
    private static int toldNormal;
    private static int toldAt;

    /** The local runner's state (for the animation and the effects of the local player). */
    public static SurfaceRun.Runner runner() {
        return RUNNER;
    }

    /** What the local speedster runs on now. */
    public static SurfaceRun.Surface surface() {
        return RUNNER.surface;
    }

    public static void reset() {
        RUNNER.reset();
        toldKind = -1;
        jumpWas = false;
    }

    /** The keys that would move him are kept out of the game while the run owns his velocity. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() != owner || !RUNNER.active()) {
            return;
        }
        Input input = event.getInput();
        input.left = false;
        input.right = false;
        input.down = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.leftImpulse = 0.0F;
        input.forwardImpulse = input.up ? KEEP_SPRINT : 0.0F;
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (player != owner) {
            reset();
            owner = player;
        }
        PowerData data = Powers.get(player);
        if (!SpeedSurfaces.isSpeedster(data.power())) {
            if (RUNNER.active()) {
                RUNNER.reset();
            }
            return;
        }
        boolean free = mc.screen == null;
        boolean forward = free && mc.options.keyUp.isDown();
        boolean back = free && mc.options.keyDown.isDown();
        boolean left = free && mc.options.keyLeft.isDown();
        boolean right = free && mc.options.keyRight.isDown();
        boolean jump = free && mc.options.keyJump.isDown();
        boolean pressedJump = jump && !jumpWas;
        jumpWas = jump;
        boolean may = SpeedSurfaces.mayRun(player, data);
        boolean running = may && data.is(Live.SPEEDING) && player.isSprinting();
        if (!may && RUNNER.active()) {
            RUNNER.reset();
            return;
        }
        if (pressedJump && RUNNER.surface.touches()) {
            Vec3 leap = SurfaceRun.leap(RUNNER);
            if (leap != null) {
                player.setDeltaMovement(leap);
                player.resetFallDistance();
                return;
            }
        }
        SurfaceRun.Input input = new SurfaceRun.Input((forward ? 1 : 0) - (back ? 1 : 0), (left ? 1 : 0) - (right ? 1 : 0),
                player.getYRot(), running, SpeedSurfaces.targetSpeed(player), SpeedSurfaces.minSpeed(), SpeedSurfaces.ceilings());
        Vec3 velocity = SurfaceRun.step(player.level(), player, RUNNER, input);
        if (velocity != null) {
            player.setDeltaMovement(velocity);
            player.resetFallDistance();
            // Running into the wall would otherwise cancel the sprint (and with it his run) later this tick.
            player.minorHorizontalCollision = true;
        }
    }

    /** The camera turns round (when the run reverses his way) after the tick's start, so it is drawn turning smoothly. */
    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() != owner || !(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (RUNNER.turn > 0.0F) {
            float step = Math.min(TURN_RATE, RUNNER.turn);
            RUNNER.turn -= step;
            player.setYRot(player.getYRot() + step);
            player.setYHeadRot(player.getYHeadRot() + step);
        }
        tellTheServer(player);
    }

    /** Tells the server what he runs on when that changes, and again every few ticks while he runs on it. */
    private static void tellTheServer(LocalPlayer player) {
        if (!SpeedSurfaces.isSpeedster(Powers.get(player).power()) || player.connection == null) {
            toldKind = -1;
            return;
        }
        SurfaceRun.Surface on = RUNNER.surface;
        int kind = on.kind() == SurfaceRun.Kind.WALL ? SurfaceRunPayload.WALL
                : on.kind() == SurfaceRun.Kind.CEILING ? SurfaceRunPayload.CEILING : SurfaceRunPayload.NONE;
        int normal = on.touches() ? on.normal().get3DDataValue() : 0;
        boolean changed = kind != toldKind || normal != toldNormal;
        if (changed || (kind != SurfaceRunPayload.NONE && player.tickCount - toldAt >= REPORT_TICKS)) {
            if (changed && kind == SurfaceRunPayload.NONE && toldKind < 0) {
                toldKind = kind;
                toldNormal = normal;
                return;
            }
            PacketDistributor.sendToServer(new SurfaceRunPayload(kind, normal));
            toldKind = kind;
            toldNormal = normal;
            toldAt = player.tickCount;
        }
    }

    private SpeedsterMotion() {
    }
}
