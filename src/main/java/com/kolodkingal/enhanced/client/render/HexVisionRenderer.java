package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Vision of a Hex, drawn like Vision: the player model with the Vision skin and his yellow cape, and the Mind Stone
 * glowing in his forehead (brighter while he fires the beam). He floats with a slow bob and leans into the direction he
 * flies.
 */
public class HexVisionRenderer extends LivingEntityRenderer<HexVisionEntity, HexVisionModel> {
    /** How far (degrees) he leans forward at full speed. */
    private static final float MAX_LEAN = 32.0F;

    private static final Variant LOOK = Variants.get(Power.VISION, "vision");

    public HexVisionRenderer(EntityRendererProvider.Context context) {
        super(context, new HexVisionModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new CapeLayer(this));
        addLayer(new StoneLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HexVisionEntity entity) {
        return LOOK.skin();
    }

    /** The same size as a player. */
    @Override
    protected void scale(HexVisionEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    /** He floats: a slow bob, and the lean into the flight, about the middle of his body. */
    @Override
    protected void setupRotations(HexVisionEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        float fly = HexVisionModel.flightAmount(entity, partialTick);
        poseStack.translate(0.0F, 0.06F + 0.05F * Mth.sin(bob * 0.09F) * (1.0F - fly), 0.0F);
        poseStack.translate(0.0F, 0.9F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(MAX_LEAN * fly));
        poseStack.translate(0.0F, -0.9F, 0.0F);
    }

    /** No name tag: he is the Vision of the Hex, not someone to look up. */
    @Override
    protected boolean shouldShowName(HexVisionEntity entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }

    /** His cape, trailing out behind him as he flies. */
    private static class CapeLayer extends RenderLayer<HexVisionEntity, HexVisionModel> {
        CapeLayer(RenderLayerParent<HexVisionEntity, HexVisionModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HexVisionEntity entity, float limbSwing,
                           float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible() || LOOK.cape() == null) {
                return;
            }
            float fly = HexVisionModel.flightAmount(entity, partialTicks);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.125F);
            // Hanging a little away from his back; streaming out behind him in flight, rippling as it does.
            poseStack.mulPose(Axis.XP.rotationDegrees(5.0F + fly * 55.0F + Mth.sin(ageInTicks * 0.2F) * (2.0F + 6.0F * fly)));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(LOOK.cape()));
            getParentModel().renderCloak(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    /** The Mind Stone in his forehead, glowing. */
    private static class StoneLayer extends RenderLayer<HexVisionEntity, HexVisionModel> {
        StoneLayer(RenderLayerParent<HexVisionEntity, HexVisionModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HexVisionEntity entity, float limbSwing,
                           float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            float pulse = 0.75F + 0.25F * Mth.sin(ageInTicks * 0.12F);
            MindStoneLayer.draw(poseStack, buffer, getParentModel().head, pulse, entity.isBeaming() ? 1.6F : 1.0F, false);
        }
    }
}
