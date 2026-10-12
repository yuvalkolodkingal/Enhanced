package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;

/**
 * The light of the Infinity Gauntlet on the Mad Titan (DESIGN section 15.5.3): the engraved lines of the forearm and
 * the cuff glow in the colour of his newest stone, the six sockets burn as {@link ThanosGlow} says, and a stone he is
 * using throws a glow round his fist that swells through the clench, which is the first thing anyone sees of what is
 * coming.
 */
public class ThanosGlowLayer<T extends Monster> extends RenderLayer<T, ThanosModel<T>> {
    /** The engraving, white on transparent: tinted by the layer. */
    public static final ResourceLocation GLOW = Enhanced.id("textures/entity/thanos_glow.png");

    public ThanosGlowLayer(RenderLayerParent<T, ThanosModel<T>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        ThanosModel<T> model = getParentModel();
        // The engraving lights in the colour of the stone he called on last, as bright as the brightest socket.
        StoneKind newest = StoneKind.nth(model.stones);
        float brightest = 0.0F;
        for (int i = 0; i < ThanosGlow.MIND; i++) {
            brightest = Math.max(brightest, model.sockets[i]);
        }
        int tint = newest == null ? StoneKind.POWER.color() : newest.color();
        if (model.auraStrength > 0.01F) {
            tint = model.auraColor;
        }
        float strength = Mth.clamp(0.2F + brightest * 0.6F, 0.0F, 1.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                EnergyGeometry.argb(tint, strength));

        poseStack.pushPose();
        model.translateToGauntlet(poseStack);
        GauntletGeometry.draw(poseStack, buffer, model.sockets, ageInTicks);
        GauntletGeometry.aura(poseStack, buffer, model.auraColor, model.auraStrength * (0.85F + 0.15F * Mth.sin(ageInTicks * 1.3F)), 7.0F);
        poseStack.popPose();
    }
}
