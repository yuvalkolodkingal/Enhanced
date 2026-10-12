package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Thanos's sword in flight: the same two-bladed model as in his hand, spinning flat like the blades of a helicopter as
 * it flies out, and turning the other way as it comes home, with a faint streak of gold along the circle its blades
 * cut.
 */
public class ThanosSwordRenderer extends EntityRenderer<ThanosSwordEntity> {
    /** How fast it whirls, in degrees per tick. */
    private static final float SPIN = 72.0F;
    private static final int GOLD = 0xD9C48A;

    private final ThanosSwordModel model;

    public ThanosSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ThanosSwordModel(context.bakeLayer(ThanosSwordModel.LAYER));
    }

    @Override
    public void render(ThanosSwordEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float direction = entity.isReturning() ? -1.0F : 1.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.2F, 0.0F);
        // Flat: the blade axis horizontal, whirling about the vertical.
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * age * SPIN));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        // (The model is drawn upside down in its own frame, as all models are.)
        poseStack.scale(-1.55F, -1.55F, 1.55F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(ThanosSwordLayer.TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        // The circle its blades cut: a faint ring of light that turns with it.
        poseStack.pushPose();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        Matrix4f m = poseStack.last().pose();
        float radius = 26.0F * 1.55F / 16.0F;
        int segments = 24;
        float start = direction * age * SPIN * Mth.DEG_TO_RAD;
        for (int i = 0; i < segments / 3; i++) {
            float a0 = start - 0.1F * i * direction;
            float a1 = start - 0.1F * (i + 1) * direction;
            float alpha = 0.35F * (1.0F - i / (segments / 3.0F));
            int color = EnergyGeometry.argb(GOLD, alpha);
            vc.addVertex(m, Mth.cos(a0) * radius * 0.85F, 0.2F, Mth.sin(a0) * radius * 0.85F).setColor(color);
            vc.addVertex(m, Mth.cos(a0) * radius, 0.2F, Mth.sin(a0) * radius).setColor(color);
            vc.addVertex(m, Mth.cos(a1) * radius, 0.2F, Mth.sin(a1) * radius).setColor(color);
            vc.addVertex(m, Mth.cos(a1) * radius * 0.85F, 0.2F, Mth.sin(a1) * radius * 0.85F).setColor(color);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThanosSwordEntity entity) {
        return ThanosSwordLayer.TEXTURE;
    }
}
