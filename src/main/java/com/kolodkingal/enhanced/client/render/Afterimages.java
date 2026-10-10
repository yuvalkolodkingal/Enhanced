package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
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

/**
 * Speedster afterimages: fading grey copies of Quicksilver trail behind him while he runs (in Quicksilver Time or Super
 * Speed), teal-green ghosts behind Tommy at super speed; and while Tommy spins a whirlwind, ghosts of him race around
 * its ring.
 */
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
        return (data.power() == Power.QUICKSILVER || data.power() == Power.TOMMY) && data.is(Live.SPEEDING)
                && !player.isInvisible();
    }

    /** Ghost colour (RGB) of each speedster. */
    private static int ghostColor(AbstractClientPlayer player) {
        // Tommy's trail is teal-green (WandaVision); Fox Quicksilver leaves no coloured glow, only a grey blur.
        return Powers.power(player) == Power.TOMMY ? 0x7FD8B8 : 0xC8D0D8;
    }

    public static void render(RenderPlayerEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() == Power.TOMMY && data.is(Live.WHIRLWIND) && !player.isInvisible()) {
            renderWhirlwind(event, player);
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
            model.renderToBuffer(poseStack, vc, event.getPackedLight(), OverlayTexture.NO_OVERLAY, alpha << 24 | ghostColor(player));
            poseStack.popPose();
        }
    }

    /** Four ghosts of Tommy race around the whirlwind's ring, leaning into the turn, each trailed by fainter ones. */
    private static void renderWhirlwind(RenderPlayerEvent.Post event, AbstractClientPlayer player) {
        float t = player.tickCount + event.getPartialTick();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.itemEntityTranslucentCull(event.getRenderer().getTextureLocation(player)));
        PoseStack poseStack = event.getPoseStack();
        // Running pose for the ghosts: the model was last set up for the real player, so set it up again, sprinting.
        model.setupAnim(player, t * 1.6F, 1.0F, t, 0.0F, 0.0F);
        float radius = 2.5F;
        Vec3 center = player.getPosition(event.getPartialTick());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (int k = 0; k < 4; k++) {
            for (int echo = 0; echo < 3; echo++) {
                float angle = t * 0.55F + k * Mth.HALF_PI - echo * 0.18F;
                // Ghosts racing past right in front of the camera fade out instead of filling the screen.
                Vec3 at = center.add(Mth.cos(angle) * radius, 1.0, Mth.sin(angle) * radius);
                float near = (float) Mth.clamp((at.distanceTo(camera) - 1.5) / 2.5, 0.0, 1.0);
                int alpha = (int) ((110 - echo * 35) * (0.85F + 0.15F * Mth.sin(t * 0.9F + k)) * near);
                if (alpha < 4) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(Mth.cos(angle) * radius, 0.05, Mth.sin(angle) * radius);
                // Facing along the ring (the direction of travel), leaning in.
                poseStack.mulPose(Axis.YP.rotation(-angle));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F));
                poseStack.scale(-0.9375F, -0.9375F, 0.9375F);
                poseStack.translate(0.0F, -1.501F, 0.0F);
                model.renderToBuffer(poseStack, vc, event.getPackedLight(), OverlayTexture.NO_OVERLAY, alpha << 24 | 0x7FD8B8);
                poseStack.popPose();
            }
        }
    }

    public static void clear() {
        HISTORY.clear();
    }

    private Afterimages() {
    }
}
