package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Mark 85's nanotech drone (3.8, PLAN 6.2, 7.1): a small red and gold hull with two gold fins, a blue eye, a blue
 * thruster. The nanotech lane's drone renderer bakes {@link #LAYER} (registered by the suit art) and calls {@link
 * #render}.
 */
public final class NanoDroneModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("nano_drone"), "main");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/iron_man/nano_drone.png");
    public static final ResourceLocation GLOW = Enhanced.id("textures/entity/iron_man/nano_drone_glow.png");
    private final ModelPart body;

    public NanoDroneModel(ModelPart root) {
        this.body = root.getChild("body");
    }

    /**
     * Draws the drone at the current origin (the entity's position, y up, blocks), facing south (turn the pose stack to
     * its yaw first), bobbing gently with {@code time} (ticks).
     */
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, float time) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.15F + 0.03F * Mth.sin(time * 0.25F), 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(0.08F * Mth.sin(time * 0.17F)));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        body.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        body.render(poseStack, buffers.getBuffer(RenderType.eyes(GLOW)), light, overlay);
        poseStack.popPose();
    }
}
