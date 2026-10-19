package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.block.StonePedestalBlock;
import com.kolodkingal.enhanced.block.StonePedestalBlockEntity;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * The stone over a stone pedestal (3.6, the plan's 4.6): it floats above the plate, bobbing and turning, at full
 * brightness, for every player who may still take it (the pedestal's block entity knows who took it, and whether it is
 * gone for everyone). The Reality Stone is the Aether: no gem but a slowly swirling orb of red fluid, drawn here in
 * quads (dark translucent bands that flow round it, bright streams that wind over it, a soft halo), no shader.
 */
public class StonePedestalRenderer implements BlockEntityRenderer<StonePedestalBlockEntity> {
    /** Where the stone floats (blocks above the pedestal's foot). */
    private static final double HEIGHT = 1.35;
    /** A gem's model stands a little above its origin, so it is put lower. */
    private static final double GEM_HEIGHT = 1.12;
    private static final float ORB = 0.24F;
    private static final int LAT = 8;
    private static final int LON = 14;

    public StonePedestalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StonePedestalBlockEntity pedestal, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (pedestal.getLevel() == null) {
            return;
        }
        StoneKind kind = StonePedestalBlock.kind(pedestal.getBlockState());
        Minecraft mc = Minecraft.getInstance();
        UUID viewer = mc.player == null ? null : mc.player.getUUID();
        if (kind == null || !pedestal.shows(viewer, kind)) {
            return;
        }
        float time = (pedestal.getLevel().getGameTime() % 24000L) + partialTick;
        pose.pushPose();
        boolean aether = kind == StoneKind.REALITY;
        pose.translate(0.5, (aether ? HEIGHT : GEM_HEIGHT) + Mth.sin(time * 0.06F) * 0.05, 0.5);
        if (aether) {
            aether(pose, buffers, time);
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(time * 2.0F));
            pose.scale(0.75F, 0.75F, 0.75F);
            mc.getItemRenderer().renderStatic(new ItemStack(ModItems.stone(kind)), ItemDisplayContext.GROUND,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, pedestal.getLevel(), 0);
        }
        pose.popPose();
    }

    /** The Aether: a red fluid orb that never holds still. */
    static void aether(PoseStack pose, MultiBufferSource buffers, float time) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(time * 0.7F));
        Matrix4f m = pose.last().pose();
        VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        // The body: a sphere whose bands of colour flow round it, between a dark blood red and a bright red.
        for (int i = 0; i < LAT; i++) {
            float t0 = Mth.PI * i / LAT;
            float t1 = Mth.PI * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                float p0 = Mth.TWO_PI * j / LON;
                float p1 = Mth.TWO_PI * (j + 1) / LON;
                vertex(veil, m, t0, p0, ORB, time);
                vertex(veil, m, t0, p1, ORB, time);
                vertex(veil, m, t1, p1, ORB, time);
                vertex(veil, m, t1, p0, ORB, time);
            }
        }
        // The streams: three bright ribbons winding round it on tilted paths.
        VertexConsumer glow = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (int s = 0; s < 3; s++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(35.0F + 50.0F * s));
            pose.mulPose(Axis.YP.rotationDegrees(time * (2.2F + s * 0.9F) + s * 120.0F));
            Matrix4f r = pose.last().pose();
            int segments = 18;
            for (int k = 0; k < segments; k++) {
                float a0 = Mth.TWO_PI * k / segments;
                float a1 = Mth.TWO_PI * (k + 1) / segments;
                float fade0 = 0.15F + 0.55F * (k / (float) segments);
                float fade1 = 0.15F + 0.55F * ((k + 1) / (float) segments);
                float radius = ORB * 1.12F;
                float wob0 = 0.025F * Mth.sin(a0 * 3.0F + time * 0.2F);
                float wob1 = 0.025F * Mth.sin(a1 * 3.0F + time * 0.2F);
                ribbon(glow, r, a0, a1, radius, wob0, wob1, fade0, fade1);
            }
            pose.popPose();
        }
        // The halo: a soft red glow that breathes.
        float breath = 0.75F + 0.25F * Mth.sin(time * 0.09F);
        int core = EnergyGeometry.argb(0xE5253B, 0.22F * breath);
        for (int plane = 0; plane < 3; plane++) {
            halo(glow, m, plane, ORB * 1.9F, core);
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float theta, float phi, float r, float time) {
        float x = r * Mth.sin(theta) * Mth.cos(phi);
        float y = r * Mth.cos(theta);
        float z = r * Mth.sin(theta) * Mth.sin(phi);
        float flow = 0.5F + 0.5F * Mth.sin(phi * 2.0F + theta * 3.0F - time * 0.12F)
                * Mth.cos(theta * 2.0F + time * 0.05F);
        int red = (int) (110 + 140 * flow);
        int green = (int) (8 + 40 * flow * flow);
        int blue = (int) (18 + 30 * flow * flow);
        vc.addVertex(m, x, y, z).setColor(red, green, blue, 215);
    }

    private static void ribbon(VertexConsumer vc, Matrix4f m, float a0, float a1, float r, float w0, float w1,
                               float f0, float f1) {
        float half = 0.018F;
        int c0 = EnergyGeometry.argb(0xFF5A4A, f0);
        int c1 = EnergyGeometry.argb(0xFF5A4A, f1);
        vc.addVertex(m, Mth.cos(a0) * r, w0 - half, Mth.sin(a0) * r).setColor(c0);
        vc.addVertex(m, Mth.cos(a1) * r, w1 - half, Mth.sin(a1) * r).setColor(c1);
        vc.addVertex(m, Mth.cos(a1) * r, w1 + half, Mth.sin(a1) * r).setColor(c1);
        vc.addVertex(m, Mth.cos(a0) * r, w0 + half, Mth.sin(a0) * r).setColor(c0);
    }

    /** A soft disc, full in the middle and nothing at the rim, in one of three planes through the centre. */
    private static void halo(VertexConsumer vc, Matrix4f m, int plane, float r, int argb) {
        int rim = argb & 0x00FFFFFF;
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            point(vc, m, plane, 0.0F, 0.0F, argb);
            point(vc, m, plane, 0.0F, 0.0F, argb);
            point(vc, m, plane, Mth.cos(a1) * r, Mth.sin(a1) * r, rim);
            point(vc, m, plane, Mth.cos(a0) * r, Mth.sin(a0) * r, rim);
        }
    }

    private static void point(VertexConsumer vc, Matrix4f m, int plane, float u, float v, int argb) {
        switch (plane) {
            case 0 -> vc.addVertex(m, 0.0F, u, v).setColor(argb);
            case 1 -> vc.addVertex(m, u, 0.0F, v).setColor(argb);
            default -> vc.addVertex(m, u, v, 0.0F).setColor(argb);
        }
    }

    @Override
    public AABB getRenderBoundingBox(StonePedestalBlockEntity pedestal) {
        return new AABB(pedestal.getBlockPos()).expandTowards(0, 1.5, 0);
    }
}
