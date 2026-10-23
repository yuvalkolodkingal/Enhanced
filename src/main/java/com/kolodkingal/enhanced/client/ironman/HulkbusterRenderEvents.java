package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderPlayerEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Where the Hulkbuster takes the place of the player (3.8, lane L4, PLAN 6.4; the {@code HulkRenderEvents} pattern):
 * a player in the Hulkbuster (worn and grown, or shut in its frame during a suit-up or take-off) is drawn by {@link
 * HulkbusterRenderer} instead of the player renderer, and his arm in first person is its forearm. The order of the
 * three swaps on this event is pinned (PLAN 17.14): Loki's tricks first (a higher priority), then the Hulk's (normal),
 * then this one (low): a renderer that already drew him cancelled the event, and this one steps aside. The inner
 * render fires the event again, for the Hulkbuster renderer: it is let through.
 */
@Subscriber(side = Side.CLIENT)
public final class HulkbusterRenderEvents {

    @Subscribe(priority = Priority.LOW)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.isCanceled() || event.getRenderer() instanceof HulkbusterRenderer) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || LokiAbilities.isHidden(player)) {
            return;
        }
        float pt = event.getPartialTick();
        if (!HulkbusterRenderer.drawsAsHulkbuster(player, pt)) {
            return;
        }
        HulkbusterRenderer renderer = HulkbusterRenderer.get();
        if (renderer == null) {
            return;
        }
        float yaw = Mth.lerp(pt, player.yRotO, player.getYRot());
        PoseStack poseStack = event.getPoseStack();
        try {
            renderer.render(player, yaw, pt, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        } catch (RuntimeException e) {
            Enhanced.LOGGER.warn("Couldn't draw the Hulkbuster; drawing the player model", e);
            return;
        }
        event.setCanceled(true);
    }

    /** His arm in first person, before the suit layer's (which would draw his own arm with the suit's pieces). */
    @Subscribe(priority = Priority.HIGH)
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        HulkbusterRenderer renderer = HulkbusterRenderer.get();
        if (renderer == null || !HulkbusterRenderer.drawsAsHulkbuster(player, pt)) {
            return;
        }
        renderer.renderFirstPersonArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                player, event.getArm());
        event.setCanceled(true);
    }

    private HulkbusterRenderEvents() {
    }
}
