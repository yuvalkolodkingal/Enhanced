package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A fragment of a shattered Mjolnir: a small, lopsided shard of the hammer's own metal, tumbling as it flies and lit
 * full bright. The glow, the streak behind it and the crackle are drawn with the rest of Thor's lightning
 * ({@link ThorEffects}).
 */
public class MjolnirFragmentRenderer extends EntityRenderer<MjolnirFragmentEntity> {
    private static final ResourceLocation SIDE = Enhanced.id("textures/block/mjolnir_head_side.png");
    private static final ResourceLocation END = Enhanced.id("textures/block/mjolnir_head_end.png");

    public MjolnirFragmentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MjolnirFragmentEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float spin = (entity.tickCount + partialTick) * 41.0F;
        int id = entity.getId();
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin + id * 47.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.7F + id * 29.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(id * 61.0F));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        // A chunk of the head: longer than it is wide, the ends of the head on its small faces.
        float hx = 0.07F;
        float hy = 0.12F;
        float hz = 0.09F;
        VertexConsumer side = buffer.getBuffer(RenderType.entityCutoutNoCull(SIDE));
        quad(side, pose, light, -hx, -hy, hz, hx, -hy, hz, hx, hy, hz, -hx, hy, hz, 0, 0, 1);
        quad(side, pose, light, hx, -hy, -hz, -hx, -hy, -hz, -hx, hy, -hz, hx, hy, -hz, 0, 0, -1);
        quad(side, pose, light, hx, -hy, hz, hx, -hy, -hz, hx, hy, -hz, hx, hy, hz, 1, 0, 0);
        quad(side, pose, light, -hx, -hy, -hz, -hx, -hy, hz, -hx, hy, hz, -hx, hy, -hz, -1, 0, 0);
        VertexConsumer end = buffer.getBuffer(RenderType.entityCutoutNoCull(END));
        quad(end, pose, light, -hx, hy, hz, hx, hy, hz, hx, hy, -hz, -hx, hy, -hz, 0, 1, 0);
        quad(end, pose, light, -hx, -hy, -hz, hx, -hy, -hz, hx, -hy, hz, -hx, -hy, hz, 0, -1, 0);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz) {
        vertex(vc, pose, light, x0, y0, z0, 0, 1, nx, ny, nz);
        vertex(vc, pose, light, x1, y1, z1, 1, 1, nx, ny, nz);
        vertex(vc, pose, light, x2, y2, z2, 1, 0, nx, ny, nz);
        vertex(vc, pose, light, x3, y3, z3, 0, 0, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int light, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(MjolnirFragmentEntity entity) {
        return SIDE;
    }
}
