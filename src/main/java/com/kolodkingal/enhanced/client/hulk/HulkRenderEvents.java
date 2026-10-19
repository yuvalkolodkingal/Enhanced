package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Where the Hulk's body takes the place of the player's (3.6): a player in a Hulk look is drawn by {@link
 * HulkPlayerRenderer} (his own model and skin) instead of the player renderer, and his arm in first person is the
 * Hulk's forearm. Loki's tricks come first (they run at a higher priority: a hidden Loki is not drawn, a shapeshifted
 * one is drawn as his creature). The inner render fires the event again, for the Hulk renderer: it is let through, so
 * the other listeners of {@code Post} see him once.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class HulkRenderEvents {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.isCanceled() || event.getRenderer() instanceof HulkPlayerRenderer) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !HulkPlayerRenderer.drawsAsHulk(player)
                || LokiAbilities.isHidden(player)) {
            return;
        }
        HulkPlayerRenderer hulk = HulkPlayerRenderer.get();
        if (hulk == null) {
            return;
        }
        float pt = event.getPartialTick();
        float yaw = Mth.lerp(pt, player.yRotO, player.getYRot());
        PoseStack poseStack = event.getPoseStack();
        try {
            hulk.render(player, yaw, pt, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        } catch (RuntimeException e) {
            Enhanced.LOGGER.warn("Couldn't draw the Hulk's body; drawing the player model", e);
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (!HulkPlayerRenderer.drawsAsHulk(player)) {
            return;
        }
        HulkPlayerRenderer hulk = HulkPlayerRenderer.get();
        if (hulk == null || Minecraft.getInstance().level == null) {
            return;
        }
        hulk.renderFirstPersonArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                event.getArm());
        event.setCanceled(true);
    }

    private HulkRenderEvents() {
    }
}
