package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The six sockets of the Infinity Gauntlet (DESIGN section 15.13.2), shared by the boss's model and the hero's layer.
 * The sockets are on the back of the hand: a row of four on the knuckles (Power, Space, Reality, Soul), the Time Stone
 * at the thumb and the large round socket of the Mind Stone, which is never lit. A stone that is not burning is a dark
 * glassy gem; a burning one is drawn full-bright in its colour with a halo, and a stone that is being used throws a
 * glow round the whole fist.
 *
 * <p>Everything is drawn in the frame of the arm the Gauntlet is on (after {@code translateAndRotate}), in the boss's
 * pixels: the hand is the box from y = 13 to 19, x = -3.5 to 3.5, z = -3.5 to 3.5 and the back of the hand is its +x
 * face. The hero's layer scales the frame down to a player's arm.
 */
public final class GauntletGeometry {
    /** The back of the hand, in the arm's pixels. */
    private static final float FACE_X = 3.5F;
    private static final float HAND_TOP = 13.0F;
    /** Where the gems stand off the face, so they never fight the texture for the depth. */
    private static final float LIFT = 0.06F;
    /** The dark glassy gem of a stone that does not burn. */
    private static final int DARK = 0xFF15131C;
    /** Each socket: its place on the face (z, y from the top of the hand) and its size, in pixels. */
    private static final float[][] SOCKET = {
            {-2.4F, 5.0F, 1.6F},
            {-0.8F, 5.0F, 1.6F},
            {0.8F, 5.0F, 1.6F},
            {2.4F, 5.0F, 1.6F},
            {-3.0F, 1.2F, 1.6F},
            {0.0F, 2.5F, 3.0F}
    };
    private static final int MIND_OUTLINE = 0xF5D77A;

    /**
     * Draws the sockets in the frame the pose stack is in.
     *
     * @param alpha  how brightly each socket burns (see {@link ThanosGlow})
     * @param age    a clock for the shimmer
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float[] alpha, float age) {
        poseStack.pushPose();
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        Matrix4f m = poseStack.last().pose();
        VertexConsumer gems = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        for (int i = 0; i < SOCKET.length; i++) {
            quad(m, gems, SOCKET[i], 1.0F, DARK, LIFT);
        }
        VertexConsumer glow = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        // The Mind Stone's socket: a thin yellow outline that is never filled, brighter in its glimmer.
        float[] mind = SOCKET[ThanosGlow.MIND];
        float outline = 0.3F + 0.7F * alpha[ThanosGlow.MIND];
        outline(m, glow, mind, EnergyGeometry.argb(MIND_OUTLINE, outline));
        if (alpha[ThanosGlow.MIND] > 0.05F) {
            quad(m, glow, mind, 1.0F, EnergyGeometry.argb(MIND_OUTLINE, 0.5F * alpha[ThanosGlow.MIND]), LIFT * 2.0F);
        }
        for (int i = 0; i < ThanosGlow.MIND; i++) {
            float a = alpha[i];
            if (a <= 0.01F) {
                continue;
            }
            StoneKind stone = StoneKind.granted().get(i);
            float flicker = 0.92F + 0.08F * Mth.sin(age * 0.7F + i * 1.7F);
            quad(m, glow, SOCKET[i], 1.0F, EnergyGeometry.argb(stone.color(), Math.min(1.0F, a * 1.4F) * flicker), LIFT * 2.0F);
            quad(m, glow, SOCKET[i], 0.6F, EnergyGeometry.argb(stone.core(), Math.min(1.0F, a * 1.6F) * flicker), LIFT * 3.0F);
            float halo = ThanosGlow.halo(a);
            if (halo > 0.0F) {
                quad(m, glow, SOCKET[i], 2.4F, EnergyGeometry.argb(stone.color(), halo), LIFT * 2.0F);
            }
        }
        poseStack.popPose();
    }

    /**
     * The glow of a stone being used, round the whole fist: three crossed discs of light, so it shows from every side.
     *
     * @param strength 0 to 1
     */
    public static void aura(PoseStack poseStack, MultiBufferSource buffers, int color, float strength, float radius) {
        if (strength <= 0.01F) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        Matrix4f m = poseStack.last().pose();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        float cx = 0.0F;
        float cy = HAND_TOP + 3.5F;
        float cz = 0.0F;
        int core = EnergyGeometry.argb(color, 0.55F * strength);
        for (int plane = 0; plane < 3; plane++) {
            disc(m, vc, plane, cx, cy, cz, radius, core);
            disc(m, vc, plane, cx, cy, cz, radius * 0.5F, EnergyGeometry.argb(0xFFFFFF, 0.35F * strength));
        }
        poseStack.popPose();
    }

    /** A soft disc, full colour in the middle and nothing at the rim, in one of the three planes through a point. */
    private static void disc(Matrix4f m, VertexConsumer vc, int plane, float cx, float cy, float cz, float r, int argb) {
        int rim = argb & 0x00FFFFFF;
        int segments = 14;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0) * r;
            float s0 = Mth.sin(a0) * r;
            float c1 = Mth.cos(a1) * r;
            float s1 = Mth.sin(a1) * r;
            point(m, vc, plane, cx, cy, cz, 0.0F, 0.0F, argb);
            point(m, vc, plane, cx, cy, cz, 0.0F, 0.0F, argb);
            point(m, vc, plane, cx, cy, cz, c1, s1, rim);
            point(m, vc, plane, cx, cy, cz, c0, s0, rim);
        }
    }

    private static void point(Matrix4f m, VertexConsumer vc, int plane, float cx, float cy, float cz, float u, float v, int argb) {
        switch (plane) {
            case 0 -> vc.addVertex(m, cx, cy + u, cz + v).setColor(argb);
            case 1 -> vc.addVertex(m, cx + u, cy, cz + v).setColor(argb);
            default -> vc.addVertex(m, cx + u, cy + v, cz).setColor(argb);
        }
    }

    /** A square on the back of the hand: {@code scale} times the socket's size, standing {@code lift} off the face. */
    private static void quad(Matrix4f m, VertexConsumer vc, float[] socket, float scale, int argb, float lift) {
        float half = socket[2] * 0.5F * scale;
        float z = socket[0];
        float y = HAND_TOP + socket[1];
        float x = FACE_X + lift;
        vc.addVertex(m, x, y - half, z - half).setColor(argb);
        vc.addVertex(m, x, y - half, z + half).setColor(argb);
        vc.addVertex(m, x, y + half, z + half).setColor(argb);
        vc.addVertex(m, x, y + half, z - half).setColor(argb);
    }

    /** The thin outline of the Mind Stone's round socket. */
    private static void outline(Matrix4f m, VertexConsumer vc, float[] socket, int argb) {
        float half = socket[2] * 0.5F + 0.35F;
        float inner = socket[2] * 0.5F - 0.1F;
        float z = socket[0];
        float y = HAND_TOP + socket[1];
        float x = FACE_X + LIFT * 2.0F;
        strip(m, vc, x, y - half, y - inner, z - half, z + half, argb);
        strip(m, vc, x, y + inner, y + half, z - half, z + half, argb);
        strip(m, vc, x, y - inner, y + inner, z - half, z - inner, argb);
        strip(m, vc, x, y - inner, y + inner, z + inner, z + half, argb);
    }

    private static void strip(Matrix4f m, VertexConsumer vc, float x, float y0, float y1, float z0, float z1, int argb) {
        vc.addVertex(m, x, y0, z0).setColor(argb);
        vc.addVertex(m, x, y0, z1).setColor(argb);
        vc.addVertex(m, x, y1, z1).setColor(argb);
        vc.addVertex(m, x, y1, z0).setColor(argb);
    }

    private GauntletGeometry() {
    }
}
