package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.CalculateDetachedCameraDistanceEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderPlayerEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Where the Hulk's body takes the place of the player's (3.6): a player in a Hulk look is drawn by {@link
 * HulkPlayerRenderer} (his own model and skin) instead of the player renderer, and his arm in first person is the
 * Hulk's forearm. Loki's tricks come first (they run at a higher priority: a hidden Loki is not drawn, a shapeshifted
 * one is drawn as his creature). The inner render fires the event again, for the Hulk renderer: it is let through, so
 * the other listeners of {@code Post} see him once.
 */
@Subscriber(side = Side.CLIENT)
public final class HulkRenderEvents {

    @Subscribe
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

    /**
     * The third-person camera of one growing into the Hulk (3.6, integration): vanilla keeps it 4 x his scale away, and
     * his scale is still Banner's when the hunched change begins, so in the front view the camera sat inside the
     * hunched body for the first half second. While he changes, it stays as far as for the Hulk he is becoming.
     */
    @Subscribe
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (!(event.getCamera().getEntity() instanceof Player player)) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.HULK || !data.is(Live.HULK_TRANSFORMING)) {
            return;
        }
        float full = (float) (1.0 + HulkAbilities.scaleOf(HulkAbilities.look(data)));
        float now = event.getEntityScalingFactor();
        if (now > 0.0F && full > now) {
            event.setDistance(event.getDistance() * full / now);
        }
    }

    @Subscribe
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
