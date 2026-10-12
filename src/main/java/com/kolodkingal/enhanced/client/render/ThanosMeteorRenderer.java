package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosMeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

/**
 * A meteor of Titan rock: a tumbling cluster of dark stone with a blue edge glow and a long streak of Space light
 * behind it, falling out of the portal its ring warned of. Drawn from vanilla's blackstone, so it needs no texture of
 * its own.
 */
public class ThanosMeteorRenderer extends EntityRenderer<ThanosMeteorEntity> {
    private static final int BLUE = StoneKind.SPACE.color();
    private static final int CORE = StoneKind.SPACE.core();

    public ThanosMeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThanosMeteorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 9.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 6.0F));
        // The great stone and three smaller ones tumbling about it.
        block(poseStack, buffer, 1.5F, 0.0F, 0.0F, 0.0F, packedLight);
        block(poseStack, buffer, 0.7F, 0.95F, 0.3F, 0.2F, packedLight);
        block(poseStack, buffer, 0.55F, -0.8F, -0.4F, 0.5F, packedLight);
        block(poseStack, buffer, 0.5F, 0.1F, 0.8F, -0.7F, packedLight);
        poseStack.popPose();
        // The blue glow round it, and the streak of light behind it.
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        Matrix4f m = poseStack.last().pose();
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.9F);
        glow(m, vc, 1.9F * pulse, EnergyGeometry.argb(BLUE, 0.35F));
        glow(m, vc, 1.1F * pulse, EnergyGeometry.argb(CORE, 0.45F));
        float tail = 9.0F;
        for (int plane = 0; plane < 2; plane++) {
            float dx = plane == 0 ? 1.0F : 0.0F;
            float dz = plane == 0 ? 0.0F : 1.0F;
            vc.addVertex(m, -dx * 0.9F, 0.0F, -dz * 0.9F).setColor(EnergyGeometry.argb(BLUE, 0.5F));
            vc.addVertex(m, dx * 0.9F, 0.0F, dz * 0.9F).setColor(EnergyGeometry.argb(BLUE, 0.5F));
            vc.addVertex(m, dx * 0.1F, tail, dz * 0.1F).setColor(EnergyGeometry.argb(BLUE, 0.0F));
            vc.addVertex(m, -dx * 0.1F, tail, -dz * 0.1F).setColor(EnergyGeometry.argb(BLUE, 0.0F));
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void block(PoseStack poseStack, MultiBufferSource buffer, float size, float x, float y, float z, int light) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(size, size, size);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.BLACKSTONE.defaultBlockState(), poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** A soft glow: three crossed discs of light. */
    private static void glow(Matrix4f m, VertexConsumer vc, float radius, int argb) {
        int rim = argb & 0x00FFFFFF;
        int segments = 12;
        for (int plane = 0; plane < 3; plane++) {
            for (int i = 0; i < segments; i++) {
                float a0 = Mth.TWO_PI * i / segments;
                float a1 = Mth.TWO_PI * (i + 1) / segments;
                float c0 = Mth.cos(a0) * radius;
                float s0 = Mth.sin(a0) * radius;
                float c1 = Mth.cos(a1) * radius;
                float s1 = Mth.sin(a1) * radius;
                vertex(m, vc, plane, 0.0F, 0.0F, argb);
                vertex(m, vc, plane, 0.0F, 0.0F, argb);
                vertex(m, vc, plane, c1, s1, rim);
                vertex(m, vc, plane, c0, s0, rim);
            }
        }
    }

    private static void vertex(Matrix4f m, VertexConsumer vc, int plane, float u, float v, int argb) {
        switch (plane) {
            case 0 -> vc.addVertex(m, u, v, 0.0F).setColor(argb);
            case 1 -> vc.addVertex(m, u, 0.0F, v).setColor(argb);
            default -> vc.addVertex(m, 0.0F, u, v).setColor(argb);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ThanosMeteorEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
