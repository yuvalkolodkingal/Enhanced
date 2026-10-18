package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.FenrisEntity;
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
 * Fenris in the world (3.5): {@link FenrisModel} at 1.5 times its size (2.4 blocks high), his scarred grey-black fur, and
 * over it the green glow of his eyes and of the scars on his muzzle (full-bright, breathing slowly). He climbs out of the
 * floor when he comes (drawn sunk into it, rising with {@link FenrisEntity#risen}).
 */
public class FenrisRenderer extends MobRenderer<FenrisEntity, FenrisModel> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/fenris.png");
    public static final ResourceLocation GLOW = Enhanced.id("textures/entity/fenris_eyes.png");
    public static final float SCALE = 1.5F;

    public FenrisRenderer(EntityRendererProvider.Context context) {
        super(context, new FenrisModel(context.bakeLayer(FenrisModel.LAYER)), 1.3F);
        addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(FenrisEntity wolf) {
        return TEXTURE;
    }

    @Override
    protected void scale(FenrisEntity wolf, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
        float risen = wolf.risen(wolf.level().getGameTime() + partialTick);
        if (risen < 1.0F) {
            // (In the model's frame, y down: sunk into the floor by what is still to rise.)
            poseStack.translate(0.0F, (1.0F - risen) * 1.3F, 0.0F);
        }
    }

    @Override
    protected float getFlipDegrees(FenrisEntity wolf) {
        return 0.0F; // (he falls to dust, not on his side)
    }

    /** His eyes and scars, green, full-bright. */
    static final class Eyes extends RenderLayer<FenrisEntity, FenrisModel> {
        Eyes(RenderLayerParent<FenrisEntity, FenrisModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FenrisEntity wolf, float limbSwing, float limbSwingAmount,
                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (wolf.isInvisible()) {
                return;
            }
            float breathe = 0.75F + 0.25F * Mth.sin(ageInTicks * 0.15F);
            int tint = EnergyGeometry.argb(0xFFFFFF, wolf.action() == FenrisEntity.ACT_HOWL ? 1.0F : breathe);
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), 0xF00000, OverlayTexture.NO_OVERLAY, tint);
        }
    }
}
