package com.kolodkingal.enhanced.client.vormir;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.vormir.StonekeeperEntity;
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
 * The Stonekeeper in the world (3.6, lane L5): {@link StonekeeperModel} in its skin, a slow bob over the hover (the
 * entity floats {@link StonekeeperEntity#HOVER} over his floor; the bob is drawn, interpolated with the partial tick),
 * and the amber eyes and the stone-orange trim full-bright over it, breathing.
 */
public class StonekeeperRenderer extends MobRenderer<StonekeeperEntity, StonekeeperModel> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/vormir/stonekeeper.png");
    public static final ResourceLocation GLOW = Enhanced.id("textures/entity/vormir/stonekeeper_eyes.png");

    public StonekeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new StonekeeperModel(context.bakeLayer(StonekeeperModel.LAYER)), 0.35F);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(StonekeeperEntity keeper) {
        return TEXTURE;
    }

    /** The bob of the hover: up to 0.05 of a block, slow. */
    static float bob(StonekeeperEntity keeper, float partialTick) {
        return 0.05F * Mth.sin((keeper.tickCount + partialTick) * 0.05F + keeper.getId() * 0.7F);
    }

    @Override
    protected void scale(StonekeeperEntity keeper, PoseStack poseStack, float partialTick) {
        // (After the renderer's flip, up is negative.)
        poseStack.translate(0.0F, -bob(keeper, partialTick), 0.0F);
    }

    /** His eyes and the orange trim of his robe, full-bright. */
    static final class Glow extends RenderLayer<StonekeeperEntity, StonekeeperModel> {
        Glow(RenderLayerParent<StonekeeperEntity, StonekeeperModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, StonekeeperEntity keeper,
                float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                float headPitch) {
            if (keeper.isInvisible()) {
                return;
            }
            float glow = 0.75F + 0.25F * Mth.sin(ageInTicks * 0.08F + keeper.getId());
            int tint = EnergyGeometry.argb(0xFFFFFF, glow);
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), 0xF00000,
                    OverlayTexture.NO_OVERLAY, tint);
        }
    }
}
