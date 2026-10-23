package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.screen.XavierChoiceScreen;
import com.kolodkingal.enhanced.network.XavierInputPayload;
import com.kolodkingal.enhanced.network.XavierPayload;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.InputEvent;
import com.kolodkingal.enhanced.platform.event.client.MovementInputUpdateEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderHandEvent;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * The client of Professor X's Take Over a Body (3.3), and of whoever he steers.
 *
 * <p><b>His client</b>: while his mind is in a body ({@code XavierPayload.LINK_START} and {@link Live#MIND_LINKED}),
 * the camera is that body's eyes, turned where his own mouse turns him (so looking around is as smooth as in his own
 * body), his hands are not drawn, his keys do not move his own body, and once a tick what they ask is sent to the server
 * ({@link XavierInputPayload}: a wish, never a position). A click is a blow of the body. The camera comes back to him
 * the moment the link ends, the body is gone, or he leaves the world.
 *
 * <p><b>The client of a player he steers</b> ({@link Live#MIND_CONTROLLED}): their keys do not move them and their
 * clicks do nothing (the server holds the same line, and moves them itself); they can still look around.
 */
@Subscriber(side = Side.CLIENT)
public final class XavierControl {
    /** How long a link the server announced is believed before the synced data says so (ticks). */
    private static final int DATA_GRACE = 10;

    private static int hostId = -1;
    private static long linkStart;
    private static int linkTicks;
    private static boolean cameraTaken;
    private static float forward;
    private static float strafe;
    private static boolean jump;
    private static boolean sneak;
    private static boolean attack;
    /** The game time a stronger telepath threw him out (the red flash of {@link XavierOverlay}). */
    private static long thrownAt = Long.MIN_VALUE / 2;

    /** A message of the server ({@link XavierPayload}). */
    public static void handle(int kind, int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        switch (kind) {
            case XavierPayload.CHOOSE -> XavierChoiceScreen.open();
            case XavierPayload.LINK_START -> {
                hostId = entityId;
                linkStart = now;
                linkTicks = ticks;
            }
            case XavierPayload.LINK_END -> release();
            case XavierPayload.THROWN_OUT -> thrownAt = now;
            default -> {
            }
        }
    }

    /** Whether the local player's mind is in another body right now. */
    public static boolean linked() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || hostId < 0) {
            return false;
        }
        return Powers.get(player).is(Live.MIND_LINKED) || mc.level.getGameTime() - linkStart < DATA_GRACE;
    }

    /** The body he steers, if his client knows it. */
    @Nullable
    public static Entity host() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || hostId < 0 ? null : mc.level.getEntity(hostId);
    }

    /** The ticks his link has left, as far as his client knows. */
    public static long ticksLeft() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : Math.max(0, linkStart + linkTicks - mc.level.getGameTime());
    }

    /** Whether Professor X steers the local player's body right now. */
    public static boolean controlled() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && Powers.get(player).is(Live.MIND_CONTROLLED);
    }

    /** The ticks since he was last thrown out (for the flash). */
    public static long sinceThrown() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? Long.MAX_VALUE : mc.level.getGameTime() - thrownAt;
    }

    /** Whether the camera is in the body he steers. */
    public static boolean inBody() {
        return cameraTaken;
    }

    private static void release() {
        hostId = -1;
        Minecraft mc = Minecraft.getInstance();
        if (cameraTaken && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        cameraTaken = false;
        attack = false;
    }

    private static void stop(Input input) {
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
    }

    /** His keys are read for the body he steers, then stopped; a steered player's keys are only stopped. */
    @Subscribe
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Input input = event.getInput();
        if (linked()) {
            forward = input.forwardImpulse;
            strafe = input.leftImpulse;
            jump = input.jumping;
            sneak = input.shiftKeyDown;
            stop(input);
        } else if (controlled()) {
            stop(input);
        }
    }

    /** His clicks are the body's blows; a steered player's clicks do nothing. Neither swings an arm here. */
    @Subscribe
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (linked()) {
            if (event.isAttack()) {
                attack = true;
            }
            event.setSwingHand(false);
            event.setCanceled(true);
        } else if (controlled()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            hostId = -1;
            cameraTaken = false;
            return;
        }
        if (!linked()) {
            if (hostId >= 0 && mc.level.getGameTime() - linkStart >= DATA_GRACE) {
                release();
            } else if (cameraTaken) {
                release();
            }
            return;
        }
        Entity host = host();
        if (host != null && !host.isRemoved()) {
            if (mc.getCameraEntity() != host) {
                mc.setCameraEntity(host);
            }
            cameraTaken = true;
        } else if (cameraTaken) {
            mc.setCameraEntity(player);
            cameraTaken = false;
        }
        Net.sendToServer(new XavierInputPayload(forward, strafe, jump, sneak, player.getYRot(), player.getXRot(), attack));
        attack = false;
    }

    /** In the body he steers, the camera turns as his own mouse turns him: smooth, with no wait for the server. */
    @Subscribe
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (cameraTaken && player != null && event.getCamera().getEntity() != player) {
            float pt = (float) event.getPartialTick();
            event.setYaw(player.getViewYRot(pt));
            event.setPitch(player.getViewXRot(pt));
        }
    }

    /** His own hands are not drawn in another body's eyes. */
    @Subscribe
    public static void onRenderHand(RenderHandEvent event) {
        if (cameraTaken) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        hostId = -1;
        cameraTaken = false;
        thrownAt = Long.MIN_VALUE / 2;
    }

    private XavierControl() {
    }
}
