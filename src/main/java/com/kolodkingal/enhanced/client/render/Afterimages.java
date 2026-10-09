package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Quicksilver's afterimages: fading silver-blue copies of him trail behind while he runs in Quicksilver Time. */
public final class Afterimages {
    private static final int LENGTH = 6;
    private static final Map<Integer, Deque<Vec3>> HISTORY = new HashMap<>();

    /** Records where each running Quicksilver was this tick. */
    public static void tick(ClientLevel level) {
        HISTORY.keySet().removeIf(id -> !(level.getEntity(id) instanceof AbstractClientPlayer player) || !running(player));
        for (AbstractClientPlayer player : level.players()) {
            if (running(player)) {
                Deque<Vec3> trail = HISTORY.computeIfAbsent(player.getId(), id -> new ArrayDeque<>());
                trail.addFirst(player.position());
                while (trail.size() > LENGTH) {
                    trail.removeLast();
                }
            }
        }
    }

    private static boolean running(AbstractClientPlayer player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.QUICKSILVER && data.slowMo() && !player.isInvisible();
    }

    public static void render(RenderPlayerEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        Deque<Vec3> trail = HISTORY.get(player.getId());
        if (trail == null || trail.size() < 2) {
            return;
        }
        float partialTick = event.getPartialTick();
        Vec3 now = player.getPosition(partialTick);
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.itemEntityTranslucentCull(event.getRenderer().getTextureLocation(player)));
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        int i = 0;
        for (Vec3 old : trail) {
            i++;
            Vec3 offset = old.subtract(now);
            if (offset.lengthSqr() < 0.09) {
                continue;
            }
            // A ghost right in front of the camera (e.g. behind Quicksilver in third person) would fill the screen.
            double toCamera = old.add(0, 1.0, 0).distanceTo(camera);
            float near = (float) Mth.clamp((toCamera - 2.0) / 3.0, 0.0, 1.0);
            float fade = (1.0F - i / (float) (LENGTH + 1)) * near;
            int alpha = (int) (120 * fade);
            if (alpha < 4) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            poseStack.scale(-0.9375F, -0.9375F, 0.9375F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            model.renderToBuffer(poseStack, vc, event.getPackedLight(), OverlayTexture.NO_OVERLAY, alpha << 24 | 0xB8D8FF);
            poseStack.popPose();
        }
    }

    public static void clear() {
        HISTORY.clear();
    }

    private Afterimages() {
    }
}
