package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The shapes Thanos's effects are built from, in camera-relative coordinates (the camera is at the origin): flat rings
 * and discs on the ground, columns and walls, textured squares lying flat, and glows that face the viewer. Every colour
 * is ARGB; where a shape has two colours the second is the far edge, so a ring can fade at one side and a column at the
 * top.
 */
final class FxShapes {
    /** A ring flat on the horizontal plane through {@code c}, from radius {@code r0} to {@code r1}. */
    static void annulus(Matrix4f m, VertexConsumer vc, Vec3 c, float r0, float r1, int segments, int colorInner, int colorOuter) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            vertex(m, vc, c.x + c0 * r0, c.y, c.z + s0 * r0, colorInner);
            vertex(m, vc, c.x + c0 * r1, c.y, c.z + s0 * r1, colorOuter);
            vertex(m, vc, c.x + c1 * r1, c.y, c.z + s1 * r1, colorOuter);
            vertex(m, vc, c.x + c1 * r0, c.y, c.z + s1 * r0, colorInner);
        }
    }

    /** A disc flat on the horizontal plane through {@code c}: {@code center} in the middle, {@code rim} at the edge. */
    static void disc(Matrix4f m, VertexConsumer vc, Vec3 c, float r, int segments, int center, int rim) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            vertex(m, vc, c.x, c.y, c.z, center);
            vertex(m, vc, c.x, c.y, c.z, center);
            vertex(m, vc, c.x + Mth.cos(a1) * r, c.y, c.z + Mth.sin(a1) * r, rim);
            vertex(m, vc, c.x + Mth.cos(a0) * r, c.y, c.z + Mth.sin(a0) * r, rim);
        }
    }

    /** The side of a cylinder standing on {@code base}: {@code bottom} at the foot, {@code top} at the head. */
    static void cylinder(Matrix4f m, VertexConsumer vc, Vec3 base, float r, float height, int segments, int bottom, int top) {
        cylinder(m, vc, base, r, r, height, segments, bottom, top);
    }

    /** A cone-like column: radius {@code r0} at the foot, {@code r1} at the head. */
    static void cylinder(Matrix4f m, VertexConsumer vc, Vec3 base, float r0, float r1, float height, int segments, int bottom, int top) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            vertex(m, vc, base.x + c0 * r0, base.y, base.z + s0 * r0, bottom);
            vertex(m, vc, base.x + c1 * r0, base.y, base.z + s1 * r0, bottom);
            vertex(m, vc, base.x + c1 * r1, base.y + height, base.z + s1 * r1, top);
            vertex(m, vc, base.x + c0 * r1, base.y + height, base.z + s0 * r1, top);
        }
    }

    /**
     * A square lying flat, turned by {@code rotation}, with the whole texture on it (for a POSITION_TEX_COLOR type).
     */
    static void flatTexture(Matrix4f m, VertexConsumer vc, Vec3 c, float half, float rotation, int argb) {
        float cos = Mth.cos(rotation) * half;
        float sin = Mth.sin(rotation) * half;
        // The corners of the square, turned about its centre.
        texVertex(m, vc, c.x - cos + sin, c.y, c.z - sin - cos, 0.0F, 0.0F, argb);
        texVertex(m, vc, c.x + cos + sin, c.y, c.z + sin - cos, 1.0F, 0.0F, argb);
        texVertex(m, vc, c.x + cos - sin, c.y, c.z + sin + cos, 1.0F, 1.0F, argb);
        texVertex(m, vc, c.x - cos - sin, c.y, c.z - sin + cos, 0.0F, 1.0F, argb);
    }

    /** A soft glow that faces the viewer: full colour in the middle, nothing at the rim, with a hot heart. */
    static void glow(Matrix4f m, VertexConsumer vc, Vec3 c, float radius, int argb, int heart) {
        Vec3 toCamera = c.scale(-1.0);
        Vec3 side = toCamera.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = side.cross(toCamera.normalize()).normalize();
        fan(m, vc, c, side, up, radius, argb, argb & 0x00FFFFFF);
        if ((heart >>> 24) > 0) {
            fan(m, vc, c, side, up, radius * 0.38F, heart, heart & 0x00FFFFFF);
        }
    }

    private static void fan(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 side, Vec3 up, float radius, int center, int rim) {
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            Vec3 p0 = c.add(side.scale(Mth.cos(a0) * radius)).add(up.scale(Mth.sin(a0) * radius));
            Vec3 p1 = c.add(side.scale(Mth.cos(a1) * radius)).add(up.scale(Mth.sin(a1) * radius));
            vertex(m, vc, c.x, c.y, c.z, center);
            vertex(m, vc, c.x, c.y, c.z, center);
            vertex(m, vc, p1.x, p1.y, p1.z, rim);
            vertex(m, vc, p0.x, p0.y, p0.z, rim);
        }
    }

    /** An axis-aligned box standing on {@code base}, each side shaded a little differently so it reads as solid. */
    static void box(Matrix4f m, VertexConsumer vc, Vec3 base, float halfWidth, float height, int rgb, float alpha) {
        float x0 = (float) base.x - halfWidth;
        float x1 = (float) base.x + halfWidth;
        float y0 = (float) base.y;
        float y1 = (float) base.y + height;
        float z0 = (float) base.z - halfWidth;
        float z1 = (float) base.z + halfWidth;
        int top = EnergyGeometry.argb(shade(rgb, 1.0F), alpha);
        int north = EnergyGeometry.argb(shade(rgb, 0.8F), alpha);
        int east = EnergyGeometry.argb(shade(rgb, 0.65F), alpha);
        quad(m, vc, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, top);
        quad(m, vc, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, north);
        quad(m, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, north);
        quad(m, vc, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, east);
        quad(m, vc, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, east);
    }

    private static int shade(int rgb, float f) {
        int r = (int) (((rgb >> 16) & 0xFF) * f);
        int g = (int) (((rgb >> 8) & 0xFF) * f);
        int b = (int) ((rgb & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    private static void quad(Matrix4f m, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2,
                             float z2, float x3, float y3, float z3, int argb) {
        vc.addVertex(m, x0, y0, z0).setColor(argb);
        vc.addVertex(m, x1, y1, z1).setColor(argb);
        vc.addVertex(m, x2, y2, z2).setColor(argb);
        vc.addVertex(m, x3, y3, z3).setColor(argb);
    }

    private static void vertex(Matrix4f m, VertexConsumer vc, double x, double y, double z, int argb) {
        vc.addVertex(m, (float) x, (float) y, (float) z).setColor(argb);
    }

    private static void texVertex(Matrix4f m, VertexConsumer vc, double x, double y, double z, float u, float v, int argb) {
        vc.addVertex(m, (float) x, (float) y, (float) z).setUv(u, v).setColor(argb);
    }

    /** {@code argb} with its alpha multiplied by {@code factor}. */
    static int fade(int argb, float factor) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0.0F, 1.0F));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private FxShapes() {
    }
}
