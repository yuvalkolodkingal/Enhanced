package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.strange.AstralProjection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * The spirit of the Astral Projection (3.6): over the see-through body every phasing player has ({@code
 * LivingEntityRendererMixin}), a pale-blue glow of the same model, breathing slowly, seen by everyone. The body he left
 * kneels where he stood ({@code AstralBodyRenderer}).
 */
public class AstralLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public AstralLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!AstralProjection.isAstral(player)) {
            return;
        }
        float breath = EnhancedClientConfig.reducedMotion() ? 0.8F : 0.7F + 0.2F * Mth.sin(ageInTicks * 0.08F);
        int alpha = (int) (110 * breath);
        int color = (alpha << 24) | 0xA8DCFF;
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(
                getTextureLocation(player))), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
    }
}
