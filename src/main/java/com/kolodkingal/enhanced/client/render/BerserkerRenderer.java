package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.BerserkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A berserker in the world (3.5): {@link BerserkerModel} in its skin, the glowing green eyes and the cracks of its armour
 * full-bright over it, and the climb out of the floor when it rises (drawn sunk into it). A hero's ally has the eyes a
 * shade paler, so a player can tell hers from the boss's.
 */
public class BerserkerRenderer extends MobRenderer<BerserkerEntity, BerserkerModel> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/berserker.png");
    public static final ResourceLocation GLOW = Enhanced.id("textures/entity/berserker_eyes.png");

    public BerserkerRenderer(EntityRendererProvider.Context context) {
        super(context, new BerserkerModel(context.bakeLayer(BerserkerModel.LAYER)), 0.5F);
        addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(BerserkerEntity berserker) {
        return TEXTURE;
    }

    @Override
    protected void scale(BerserkerEntity berserker, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        float risen = berserker.risen(berserker.level().getGameTime() + partialTick);
        if (risen < 1.0F) {
            poseStack.translate(0.0F, (1.0F - risen) * 1.9F, 0.0F);
        }
    }

    /** Its eyes and the cracks of its armour. */
    static final class Eyes extends RenderLayer<BerserkerEntity, BerserkerModel> {
        Eyes(RenderLayerParent<BerserkerEntity, BerserkerModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, BerserkerEntity berserker, float limbSwing,
                           float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (berserker.isInvisible()) {
                return;
            }
            float glow = 0.8F + 0.2F * Mth.sin(ageInTicks * 0.2F + berserker.getId());
            int tint = berserker.isAlly() ? EnergyGeometry.argb(0xCFFFE0, glow) : EnergyGeometry.argb(0xFFFFFF, glow);
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), 0xF00000, OverlayTexture.NO_OVERLAY, tint);
        }
    }
}
