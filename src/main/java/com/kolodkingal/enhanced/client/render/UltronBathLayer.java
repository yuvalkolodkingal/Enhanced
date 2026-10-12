package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

/**
 * The whole body bathed in the light of the Key, while he lifts Sokovia: the body's own skin drawn over itself, additive,
 * in cyan, as bright as the pose says ({@link UltronPose#BATH}).
 */
public class UltronBathLayer<T extends LivingEntity> extends RenderLayer<T, UltronModel<T>> {
    public UltronBathLayer(RenderLayerParent<T, UltronModel<T>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        float bath = UltronAnimation.pose(entity, partialTick).get(UltronPose.BATH);
        if (bath < 0.02F || entity.isInvisible()) {
            return;
        }
        int color = UltronGlowLayer.color(0.50F, 0.83F, 1.0F, Math.min(1.0F, bath));
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(getTextureLocation(entity))), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, color);
    }
}
