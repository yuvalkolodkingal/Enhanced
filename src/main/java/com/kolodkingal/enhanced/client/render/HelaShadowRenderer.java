package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.hela.HelaShadowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * A Shadow of Hela (3.5): a small dark silhouette of the Goddess of Death made of smoke. Her slim body in her own skin
 * ({@code hela_goddess}), drawn almost black and see-through, with the antlers of her headdress, hovering and swaying, her
 * arms a little out; it flickers (its alpha trembles, now and then it gutters out for a moment and its outline jitters),
 * its legs fade into soft puffs of black-green smoke turning round it, and a faint green ember burns where her heart would
 * be. No name, no shadow on the ground. The wisps that trail it are the entity's own particles.
 */
public class HelaShadowRenderer extends EntityRenderer<HelaShadowEntity> {
    public static final ResourceLocation SMOKE = Enhanced.id("textures/entity/hela_smoke.png");
    /** How big the silhouette is: about 1.2 blocks high. */
    private static final float SCALE = 0.62F;
    /** Her body's tint: black with a breath of green. */
    private static final int TINT = 0x1C2A22;

    private final PlayerModel<HelaShadowEntity> body;
    private final ModelPart headdress;

    public HelaShadowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.body = new PlayerModel<>(context.bakeLayer(HelaModel.LAYER), true);
        this.headdress = context.bakeLayer(HelaHeaddressLayer.LAYER);
    }

    /** How much of her shows at {@code t} (ticks): a tremble, and a gutter now and then. */
    static float flicker(float t, int seed) {
        float a = 0.55F + 0.18F * Mth.sin(t * 0.9F + seed) + 0.1F * Mth.sin(t * 2.3F + seed * 0.7F);
        int beat = Math.floorMod((int) t + seed * 7, 37);
        if (beat == 0 || beat == 1) {
            a *= 0.25F;
        }
        return Mth.clamp(a, 0.05F, 0.85F);
    }

    @Override
    public void render(HelaShadowEntity shadow, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = shadow.tickCount + partialTicks;
        int seed = shadow.getId();
        float alpha = flicker(t, seed);
        poseStack.pushPose();
        float bob = 0.08F * Mth.sin(t * 0.12F + seed);
        poseStack.translate(0.0F, 0.15F + bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTicks, shadow.yBodyRotO, shadow.yBodyRot)));
        // The outline jitters a little as the smoke shifts.
        if (Math.floorMod((int) (t * 3) + seed, 11) == 0) {
            poseStack.translate(0.03F * Mth.sin(t * 7.0F), 0.0F, 0.03F * Mth.cos(t * 5.0F));
        }
        poseStack.scale(-SCALE, -SCALE, SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        pose(t, seed);
        int tint = EnergyGeometry.argb(TINT, alpha);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(HelaRenderer.TEXTURE));
        body.head.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, tint);
        body.hat.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, tint);
        body.body.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, tint);
        body.rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, tint);
        body.leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, tint);
        // (The legs fade into the smoke: drawn fainter.)
        int legs = EnergyGeometry.argb(TINT, alpha * 0.45F);
        body.rightLeg.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, legs);
        body.leftLeg.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, legs);
        HelaHeaddressLayer.draw(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, body.head, headdress, 1.0F,
                EnergyGeometry.argb(0x2A4A36, alpha));
        poseStack.popPose();
        smoke(t, seed, poseStack, buffer, packedLight);
        super.render(shadow, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Her pose: hovering, arms a little out and drifting, the head turning slowly. */
    private void pose(float t, int seed) {
        for (ModelPart part : new ModelPart[]{body.head, body.hat, body.body, body.rightArm, body.leftArm, body.rightLeg, body.leftLeg}) {
            part.resetPose();
        }
        body.head.yRot = 0.4F * Mth.sin(t * 0.03F + seed);
        body.head.xRot = 0.1F;
        body.rightArm.zRot = 0.35F + 0.08F * Mth.sin(t * 0.1F);
        body.leftArm.zRot = -0.35F - 0.08F * Mth.sin(t * 0.1F + 1.0F);
        body.rightArm.xRot = -0.15F;
        body.leftArm.xRot = -0.15F;
        body.rightLeg.xRot = 0.12F;
        body.leftLeg.xRot = -0.05F;
        body.hat.copyFrom(body.head);
    }

    /** Puffs of smoke turning round her lower half, and the ember. */
    private void smoke(float t, int seed, PoseStack poseStack, MultiBufferSource buffer, int light) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(SMOKE));
        for (int i = 0; i < 5; i++) {
            float a = t * 0.05F + i * Mth.TWO_PI / 5.0F;
            float r = 0.16F + 0.05F * Mth.sin(t * 0.07F + i);
            float size = 0.26F + 0.07F * Mth.sin(t * 0.11F + i * 1.7F);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, 0.2F + 0.08F * Mth.sin(t * 0.09F + i * 2.0F), Mth.sin(a) * r);
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            puff(poseStack.last().pose(), vc, size, light, 0xB0FFFFFF, i);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.95F, 0.0F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        float glow = 0.35F + 0.25F * Mth.sin(t * 0.2F + seed);
        puff(poseStack.last().pose(), buffer.getBuffer(RenderType.entityTranslucentEmissive(SMOKE)), 0.12F, 0xF000F0,
                EnergyGeometry.argb(HelaRules.PALE, glow), 0);
        poseStack.popPose();
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
        return HelaRenderer.TEXTURE;
    }
}
