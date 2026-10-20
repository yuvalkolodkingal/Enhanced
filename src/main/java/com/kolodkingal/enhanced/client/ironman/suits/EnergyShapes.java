package com.kolodkingal.enhanced.client.ironman.suits;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The suits' shapes of light (3.8, PLAN 7.1): the Mark 85's giant energy shield (the Endgame ref: a big curved wall of
 * translucent blue hexagonal facets with bright edges, about 4 blocks wide and 3 high, held out from his left side)
 * and the thrusters' flames (a frame falling from the sky). Full-bright, interpolated with the look's time.
 */
final class EnergyShapes {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    /** The shield: its curve's radius and centre (body pixels), its half width and half height (pixels). */
    private static final float RADIUS = 34.0F;
    private static final float CENTRE_X = 6.0F;
    private static final float CENTRE_Y = 5.0F;
    private static final float CENTRE_Z = 16.0F;
    private static final float HALF_WIDTH = 32.0F;
    private static final float HALF_HEIGHT = 24.0F;
    /** A facet's radius (pixels) and the gap between facets. */
    private static final float HEX = 4.6F;
    private static final float GAP = 0.45F;
    private static final int LIGHT = 0xF000F0;

    /** The Mark 85's hexagonal shield in front of him, grown out to {@code look.shield}. */
    static void hexShield(PoseStack poseStack, MultiBufferSource buffers, HumanoidModel<?> body, SuitLook look) {
        float grow = look.shield;
        poseStack.pushPose();
        body.body.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        Matrix4f m = poseStack.last().pose();
        PoseStack.Pose normal = poseStack.last();
        float w = (float) Math.sqrt(3.0) * HEX;
        float rowStep = 1.5F * HEX;
        int cols = Mth.ceil(HALF_WIDTH / w) + 1;
        int rows = Mth.ceil(HALF_HEIGHT / rowStep) + 1;
        VertexConsumer fill = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = pass == 0 ? fill : buffers.getBuffer(RenderType.eyes(WHITE));
            for (int r = -rows; r <= rows; r++) {
                for (int c = -cols; c <= cols; c++) {
                    float u = c * w + ((r & 1) != 0 ? w * 0.5F : 0.0F);
                    float v = r * rowStep;
                    float e = (u * u) / (HALF_WIDTH * HALF_WIDTH) + (v * v) / (HALF_HEIGHT * HALF_HEIGHT);
                    if (e > 1.0F) {
                        continue;
                    }
                    // it grows out from the middle; each facet shimmers on its own beat
                    float reach = (float) Math.sqrt(e);
                    float on = Mth.clamp((grow * 1.25F - reach) * 4.0F, 0.0F, 1.0F);
                    if (on <= 0.0F) {
                        continue;
                    }
                    float beat = 0.75F + 0.25F * Mth.sin(look.time * 0.3F + c * 1.7F + r * 2.3F);
                    float rim = 1.0F - 0.5F * reach;
                    if (pass == 0) {
                        int a = Math.round(70 * on * beat * rim);
                        hexagon(m, normal, vc, u, v, HEX - GAP, 0x60, 0xC8, 0xFF, a, false);
                    } else {
                        int k = Math.round(255 * on * beat);
                        hexagon(m, normal, vc, u, v, HEX - GAP, k * 70 / 255, k * 190 / 255, k, 255, true);
                    }
                }
            }
        }
        poseStack.popPose();
    }

    /** One facet: a filled hexagon, or its outline (thin bands along the six sides), on the curve. */
    private static void hexagon(Matrix4f m, PoseStack.Pose normal, VertexConsumer vc, float cu, float cv, float rad,
                                int red, int green, int blue, int alpha, boolean outline) {
        float[] us = new float[6];
        float[] vs = new float[6];
        for (int i = 0; i < 6; i++) {
            float a = (float) (Math.PI / 3.0 * i + Math.PI / 6.0);
            us[i] = cu + rad * Mth.cos(a);
            vs[i] = cv + rad * Mth.sin(a);
        }
        for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            if (outline) {
                float k = 0.78F;
                float iu0 = cu + (us[i] - cu) * k;
                float iv0 = cv + (vs[i] - cv) * k;
                float iu1 = cu + (us[j] - cu) * k;
                float iv1 = cv + (vs[j] - cv) * k;
                quad(m, normal, vc, us[i], vs[i], us[j], vs[j], iu1, iv1, iu0, iv0, red, green, blue, alpha);
            } else {
                quad(m, normal, vc, cu, cv, us[i], vs[i], us[j], vs[j], us[j], vs[j], red, green, blue, alpha);
            }
        }
    }

    /** A quad of the curve (u along it, v down), both faces. */
    private static void quad(Matrix4f m, PoseStack.Pose normal, VertexConsumer vc, float u0, float v0, float u1,
                             float v1, float u2, float v2, float u3, float v3, int r, int g, int b, int a) {
        float[][] p = {curve(u0, v0), curve(u1, v1), curve(u2, v2), curve(u3, v3)};
        for (int k = 0; k < 4; k++) {
            vertex(m, normal, vc, p[k], r, g, b, a);
        }
        for (int k = 3; k >= 0; k--) {
            vertex(m, normal, vc, p[k], r, g, b, a);
        }
    }

    private static float[] curve(float u, float v) {
        float angle = u / RADIUS;
        return new float[] {CENTRE_X + RADIUS * Mth.sin(angle), CENTRE_Y + v, CENTRE_Z - RADIUS * Mth.cos(angle)};
    }

    private static void vertex(Matrix4f m, PoseStack.Pose normal, VertexConsumer vc, float[] p, int r, int g, int b,
                               int a) {
        vc.addVertex(m, p[0], p[1], p[2]).setColor(r, g, b, a).setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(normal, 0.0F, 0.0F, -1.0F);
    }

    /**
     * A thruster's flame: a blue-white cone pointing down {@code length} pixels from the current origin (pixels),
     * flickering with {@code time}.
     */
    static void flame(PoseStack poseStack, MultiBufferSource buffers, float radius, float length, float time) {
        Matrix4f m = poseStack.last().pose();
        PoseStack.Pose normal = poseStack.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(WHITE));
        float flicker = 0.85F + 0.15F * Mth.sin(time * 2.7F);
        float tip = length * flicker;
        int sides = 8;
        for (int layer = 0; layer < 2; layer++) {
            float rad = layer == 0 ? radius : radius * 0.55F;
            float len = layer == 0 ? tip : tip * 0.6F;
            int r = layer == 0 ? 60 : 200;
            int g = layer == 0 ? 150 : 240;
            for (int i = 0; i < sides; i++) {
                float a0 = Mth.TWO_PI * i / sides;
                float a1 = Mth.TWO_PI * (i + 1) / sides;
                float[] p0 = {rad * Mth.cos(a0), 0.0F, rad * Mth.sin(a0)};
                float[] p1 = {rad * Mth.cos(a1), 0.0F, rad * Mth.sin(a1)};
                float[] t = {0.0F, len, 0.0F};
                vertex(m, normal, vc, p0, r, g, 255, 255);
                vertex(m, normal, vc, p1, r, g, 255, 255);
                vertex(m, normal, vc, t, 0, 0, 0, 255);
                vertex(m, normal, vc, t, 0, 0, 0, 255);
            }
        }
    }

    private EnergyShapes() {
    }
}
