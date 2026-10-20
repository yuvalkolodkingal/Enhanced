package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.ironman.IronMissileEntity;
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
 * Iron Man's rockets and mini-missiles (3.8, lane L3; and Falcon's Wrist Rocket, the same entity): a slim metal body
 * along its flight (the Mark 1's arm rocket bigger and grey, the shoulder missiles silver with a red nose, the homing
 * ones gold-tipped, Falcon's dark with a red band), small fins at the back, and a blue-white flame flickering behind
 * it. Turned by its interpolated rotation (the partial tick), so its turns are smooth; its smoke is the entity's own.
 */
public class IronMissileRenderer extends EntityRenderer<IronMissileEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public IronMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(IronMissileEntity missile, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        byte kind = missile.kind();
        float size = kind == IronMissileEntity.ARM_ROCKET ? 1.4F : 1.0F;
        int body = switch (kind) {
            case IronMissileEntity.ARM_ROCKET -> 0xFF6E6E6E;
            case IronMissileEntity.FALCON -> 0xFF3A3D44;
            default -> 0xFFC9CED6;
        };
        int nose = switch (kind) {
            case IronMissileEntity.HOMING -> 0xFFE8B83A;
            case IronMissileEntity.ARM_ROCKET -> 0xFF4A4A4A;
            default -> 0xFFB3121B;
        };
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, missile.yRotO, missile.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, missile.xRotO, missile.getXRot())));
        pose.scale(size / 16.0F, size / 16.0F, size / 16.0F);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        Matrix4f m = pose.last().pose();
        PoseStack.Pose normal = pose.last();
        // The body (x along the flight), the nose, two fins.
        box(m, normal, vc, -3.0F, -0.8F, -0.8F, 3.0F, 0.8F, 0.8F, body, light);
        box(m, normal, vc, 3.0F, -0.6F, -0.6F, 4.4F, 0.6F, 0.6F, nose, light);
        box(m, normal, vc, -3.0F, -1.6F, -0.15F, -1.6F, 1.6F, 0.15F, body, light);
        box(m, normal, vc, -3.0F, -0.15F, -1.6F, -1.6F, 0.15F, 1.6F, body, light);
        if (kind == IronMissileEntity.FALCON) {
            box(m, normal, vc, 0.4F, -0.85F, -0.85F, 1.2F, 0.85F, 0.85F, 0xFFD8323C, light);
        }
        // The flame behind it.
        float flicker = 0.8F + 0.2F * Mth.sin((missile.tickCount + partialTick) * 3.1F);
        VertexConsumer glow = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        flame(m, glow, -3.0F, 1.0F, 4.0F * flicker);
        pose.popPose();
        super.render(missile, yaw, partialTick, pose, buffer, light);
    }

    private static void box(Matrix4f m, PoseStack.Pose n, VertexConsumer vc, float x0, float y0, float z0, float x1,
                            float y1, float z1, int argb, int light) {
        face(m, n, vc, argb, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(m, n, vc, argb, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(m, n, vc, argb, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(m, n, vc, argb, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        face(m, n, vc, argb, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        face(m, n, vc, argb, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
    }

    private static void face(Matrix4f m, PoseStack.Pose n, VertexConsumer vc, int argb, int light, float nx, float ny,
                             float nz, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy,
                             float cz, float dx, float dy, float dz) {
        vertex(m, n, vc, argb, light, nx, ny, nz, ax, ay, az, 0, 0);
        vertex(m, n, vc, argb, light, nx, ny, nz, bx, by, bz, 0, 1);
        vertex(m, n, vc, argb, light, nx, ny, nz, cx, cy, cz, 1, 1);
        vertex(m, n, vc, argb, light, nx, ny, nz, dx, dy, dz, 1, 0);
    }

    private static void vertex(Matrix4f m, PoseStack.Pose n, VertexConsumer vc, int argb, int light, float nx, float ny,
                               float nz, float x, float y, float z, float u, float v) {
        vc.addVertex(m, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(n, nx, ny, nz);
    }

    /** The flame: a short blue-white spike pointing back along -x. */
    private static void flame(Matrix4f m, VertexConsumer vc, float x, float r, float length) {
        int[][] layers = {{0x7F, 0xD8, 0xFF, 150}, {0xF2, 0xFC, 0xFF, 230}};
        float[] radii = {r, r * 0.45F};
        float[] lengths = {length, length * 0.6F};
        for (int l = 0; l < 2; l++) {
            int[] c = layers[l];
            float rr = radii[l];
            float[][] ring = {{-rr, -rr}, {rr, -rr}, {rr, rr}, {-rr, rr}};
            for (int i = 0; i < 4; i++) {
                float[] a = ring[i];
                float[] b = ring[(i + 1) % 4];
                vc.addVertex(m, x, a[0], a[1]).setColor(c[0], c[1], c[2], c[3]);
                vc.addVertex(m, x, b[0], b[1]).setColor(c[0], c[1], c[2], c[3]);
                vc.addVertex(m, x - lengths[l], 0.0F, 0.0F).setColor(c[0], c[1], c[2], 0);
                vc.addVertex(m, x - lengths[l], 0.0F, 0.0F).setColor(c[0], c[1], c[2], 0);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(IronMissileEntity missile) {
        return WHITE;
    }
}
