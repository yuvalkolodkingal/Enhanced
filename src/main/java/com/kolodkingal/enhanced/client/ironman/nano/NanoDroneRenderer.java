package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a Mark 85 nanotech drone (3.8, lane L4): {@link NanoDroneModel} in red and gold, its eye glowing blue, turned
 * with Iron Man's facing (interpolated), tilting a little as it follows, its eye flaring when it fires (the laser flash
 * is {@link NanoClient}'s) and flickering white when something hits it.
 */
public class NanoDroneRenderer extends EntityRenderer<NanoDroneEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    static final int RED = 0xFFB3121B;
    static final int GOLD = 0xFFE8B23A;
    static final int DARK = 0xFF3A3A40;
    static final int EYE = 0xFF9FE8FF;

    private final NanoDroneModel model;

    public NanoDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new NanoDroneModel(context.bakeLayer(NanoDroneModel.LAYER));
        shadowRadius = 0.15F;
    }

    @Override
    public void render(NanoDroneEntity drone, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        float age = drone.tickCount + partialTick;
        poseStack.translate(0.0, 0.2, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, drone.yRotO, drone.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.sin(age * 0.11F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(4.0F * Mth.sin(age * 0.07F + drone.slot())));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        float flicker = drone.flicker(partialTick);
        int tint = flicker > 0.0F && ((int) (age * 2.0F) & 1) == 0 ? 0xFFFFFFFF : 0;
        model.renderSolid(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE)), light,
                OverlayTexture.NO_OVERLAY, tint != 0 ? tint : RED, tint != 0 ? tint : GOLD, DARK);
        model.eye.render(poseStack, buffer.getBuffer(RenderType.eyes(WHITE)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, EYE);
        poseStack.popPose();
        super.render(drone, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(NanoDroneEntity drone) {
        return WHITE;
    }
}
