package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.hela.HelaShadowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * A Shadow of Hela (3.5): smoke only. Five soft puffs of black-green smoke turning slowly round one another and breathing,
 * always facing the camera, with a faint green ember in the heart of them; no body, no name. The trailing wisps are the
 * entity's own particles.
 */
public class HelaShadowRenderer extends EntityRenderer<HelaShadowEntity> {
    public static final ResourceLocation SMOKE = Enhanced.id("textures/entity/hela_smoke.png");

    public HelaShadowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(HelaShadowEntity shadow, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = shadow.tickCount + partialTicks + shadow.getId() * 13.0F;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(SMOKE));
        for (int i = 0; i < 5; i++) {
            float a = t * 0.05F + i * Mth.TWO_PI / 5.0F;
            float r = 0.12F + 0.05F * Mth.sin(t * 0.07F + i);
            float size = 0.32F + 0.08F * Mth.sin(t * 0.11F + i * 1.7F);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, 0.3F + 0.08F * Mth.sin(t * 0.09F + i * 2.0F), Mth.sin(a) * r);
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            puff(poseStack.last().pose(), vc, size, packedLight, 0xC8FFFFFF, i);
            poseStack.popPose();
        }
        // The ember: a small green glow in the middle.
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.3F, 0.0F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        float glow = 0.5F + 0.3F * Mth.sin(t * 0.2F);
        puff(poseStack.last().pose(), buffer.getBuffer(RenderType.entityTranslucentEmissive(SMOKE)), 0.18F, 0xF000F0,
                EnergyGeometry.argb(HelaRules.PALE, glow), 0);
        poseStack.popPose();
        super.render(shadow, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One square puff of the smoke texture, turned by {@code turn} quarters (so the puffs differ). */
    private static void puff(Matrix4f m, VertexConsumer vc, float size, int light, int argb, int turn) {
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        float[][] xy = {{-size, -size}, {size, -size}, {size, size}, {-size, size}};
        for (int k = 0; k < 4; k++) {
            float[] q = uv[(k + turn) % 4];
            vc.addVertex(m, xy[k][0], xy[k][1], 0.0F).setColor(argb).setUv(q[0], q[1]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(HelaShadowEntity shadow) {
        return SMOKE;
    }
}
