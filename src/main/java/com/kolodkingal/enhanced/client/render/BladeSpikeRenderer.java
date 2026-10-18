package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.hela.BladeSpikeEntity;
import com.kolodkingal.enhanced.hela.HelaRules;
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
 * A blade spike (3.5): a great necrosword ({@link NecroswordModel} at two and a half times its size, a ring's at two)
 * bursting point first out of the floor. Through its telegraph the floor cracks: a star of green light that grows and
 * flickers where it will come out; then it rises in four ticks with a flash at its foot, stands, and sinks back. The height
 * is {@link BladeSpikeEntity#height}, eased with the partial tick, so the rise is smooth at any frame rate.
 */
public class BladeSpikeRenderer extends EntityRenderer<BladeSpikeEntity> {
    private static final float WALL_SCALE = 2.4F;
    private static final float RING_SCALE = 1.9F;

    private final NecroswordModel model;

    public BladeSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new NecroswordModel(context.bakeLayer(NecroswordModel.LAYER));
    }

    @Override
    public void render(BladeSpikeEntity spike, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float time = spike.tickCount + partialTicks;
        float height = spike.height(time);
        int delay = spike.delay();
        if (time < delay + 2.0F) {
            cracks(poseStack, buffer, time, delay);
        }
        if (height > 0.001F) {
            float scale = spike.style() == 1 ? RING_SCALE : WALL_SCALE;
            float length = NecroswordModel.LENGTH / 16.0F * scale;
            poseStack.pushPose();
            // Out of the floor: the part still under it is pushed down (the floor hides it).
            poseStack.translate(0.0F, -length * (1.0F - height) - 0.15F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTicks, spike.yRotO, spike.getYRot())));
            poseStack.mulPose(Axis.ZP.rotationDegrees((spike.getId() % 7 - 3) * 2.5F));
            poseStack.scale(-scale, -scale, scale);
            // (The point up: the model's -y; its grip at the floor.)
            poseStack.translate(0.0F, -0.3F / scale, 0.0F);
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(NecroswordModel.TEXTURE)), packedLight,
                    OverlayTexture.NO_OVERLAY, -1);
            model.renderEdges(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(NecroswordModel.TEXTURE)), OverlayTexture.NO_OVERLAY,
                    EnergyGeometry.argb(HelaRules.PALE, 0.85F * height));
            poseStack.popPose();
        }
        super.render(spike, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The cracks: six rays of green light on the floor, growing to the spike's moment, flickering. */
    private static void cracks(PoseStack poseStack, MultiBufferSource buffer, float time, int delay) {
        float grow = delay <= 0 ? 1.0F : Mth.clamp(time / delay, 0.0F, 1.0F);
        float flash = time > delay ? Math.max(0.0F, 1.0F - (time - delay) / 2.0F) : 0.0F;
        float alpha = (0.35F + 0.4F * grow) * (0.8F + 0.2F * Mth.sin(time * 2.1F)) + flash * 0.6F;
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        Matrix4f m = poseStack.last().pose();
        int bright = EnergyGeometry.argb(HelaRules.GREEN, Math.min(1.0F, alpha));
        int dim = EnergyGeometry.argb(HelaRules.GREEN, 0.0F);
        float y = 0.03F;
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6.0F + (i % 2) * 0.3F;
            float len = (0.35F + 0.35F * (i % 3) / 2.0F) * (0.3F + 0.7F * grow);
            float w = 0.05F;
            float cx = Mth.cos(a);
            float cz = Mth.sin(a);
            float px = -cz * w;
            float pz = cx * w;
            vc.addVertex(m, px, y, pz).setColor(bright);
            vc.addVertex(m, -px, y, -pz).setColor(bright);
            vc.addVertex(m, cx * len, y, cz * len).setColor(dim);
            vc.addVertex(m, cx * len, y, cz * len).setColor(dim);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BladeSpikeEntity entity) {
        return NecroswordModel.TEXTURE;
    }
}
