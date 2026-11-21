package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Billy the Demiurge's star energy (3.18.1): in his powered look his whole body glows, a soft see-through white-silver
 * light a little larger than the body, full-bright, with small stars in it that twinkle (two star fields fading into each
 * other). It grows and falls back with the powered look ({@code DemiurgeFx.out}); at rest there is none.
 */
public class DemiurgeGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation STARS_A = Enhanced.id("textures/entity/suit/billy_demiurge_aura_0.png");
    private static final ResourceLocation STARS_B = Enhanced.id("textures/entity/suit/billy_demiurge_aura_1.png");
    private static final ResourceLocation WHITE = Enhanced.id("textures/misc/white.png");
    /** How much larger than the body the glow is drawn (round the middle of the body). */
    private static final float GROW = 1.07F;

    public DemiurgeGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!Demiurge.is(player) || player.isInvisible()) {
            return;
        }
        float flash = DemiurgeFx.flash(player);
        if (flash > 0.02F) {
            // (3.18.1) the transformation: his whole body floods with white light, a little larger than him
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.75F, 0.0F);
            poseStack.scale(1.02F, 1.02F, 1.02F);
            poseStack.translate(0.0F, -0.75F, 0.0F);
            draw(poseStack, buffers, WHITE, flash * 0.9F);
            poseStack.popPose();
        }
        float out = DemiurgeFx.out(player);
        if (out <= 0.02F) {
            return;
        }
        float twinkle = EnhancedClientConfig.reducedMotion() ? 0.5F : 0.5F + 0.5F * Mth.sin(ageInTicks * 0.18F);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.75F, 0.0F);
        poseStack.scale(GROW, GROW, GROW);
        poseStack.translate(0.0F, -0.75F, 0.0F);
        draw(poseStack, buffers, STARS_A, out * twinkle);
        draw(poseStack, buffers, STARS_B, out * (1.0F - twinkle));
        poseStack.popPose();
    }

    private void draw(PoseStack poseStack, MultiBufferSource buffers, ResourceLocation texture, float alpha) {
        int a = Mth.clamp((int) (255 * alpha), 0, 255);
        if (a < 4) {
            return;
        }
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (a << 24) | 0xFFFFFF);
    }
}
