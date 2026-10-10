package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Builds energy shapes in camera-relative coordinates (the camera sits at 0,0,0), so every quad can be turned to
 * face the viewer.
 */
public final class EnergyGeometry {

    /** A flat ribbon from {@code a} to {@code b} that always faces the camera. Colour is ARGB. */
    public static void ribbon(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, int colorA, int colorB) {
        ribbon(pose, vc, a, b, width, width, colorA, colorB);
    }

    /** A camera-facing ribbon whose width changes from {@code widthA} at {@code a} to {@code widthB} at {@code b}. */
    public static void ribbon(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float widthA, float widthB, int colorA, int colorB) {
        Vec3 dir = b.subtract(a);
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = dir.cross(mid.scale(-1));
        if (side.lengthSqr() < 1.0E-8) {
            side = dir.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0E-8) {
                side = new Vec3(1, 0, 0);
            }
        }
        side = side.normalize();
        Vec3 sideA = side.scale(widthA * 0.5);
        Vec3 sideB = side.scale(widthB * 0.5);
        vertex(pose, vc, a.add(sideA), colorA);
        vertex(pose, vc, a.subtract(sideA), colorA);
        vertex(pose, vc, b.subtract(sideB), colorB);
        vertex(pose, vc, b.add(sideB), colorB);
    }

    /**
     * A crackling bolt between two points: split into short segments, each jittered sideways.
     *
     * @param seed changes the shape; change it every few ticks to make the bolt flicker
     */
    public static void bolt(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, float jitter, int color, long seed) {
        RandomSource random = RandomSource.create(seed);
        double length = a.distanceTo(b);
        int segments = Math.max(2, (int) (length * 2.0));
        Vec3 dir = b.subtract(a);
        Vec3 perp1 = dir.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 1.0E-6) {
            perp1 = dir.cross(new Vec3(1, 0, 0));
        }
        perp1 = perp1.normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();
        Vec3 prev = a;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 next = a.add(dir.scale(t));
            if (i < segments) {
                double amount = jitter * Math.sin(t * Math.PI);
                next = next.add(perp1.scale((random.nextDouble() - 0.5) * 2 * amount)).add(perp2.scale((random.nextDouble() - 0.5) * 2 * amount));
            }
            ribbon(pose, vc, prev, next, width, color, color);
            prev = next;
        }
    }

    /**
     * A textured, double-sided sphere whose rim glows more than its middle (cheap fresnel), drawn with a
     * POSITION_TEX_COLOR render type.
     *
     * @param center    sphere centre relative to the camera
     * @param tilesU    texture repeats around the equator
     * @param tilesV    texture repeats from pole to pole
     * @param scroll    texture offset (animates the pattern)
     * @param baseAlpha alpha where the surface faces the camera
     * @param rimAlpha  extra alpha where the surface is seen edge-on
     */
    public static void sphere(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, int segments, int rings,
                              float tilesU, float tilesV, float scroll, int rgb, float baseAlpha, float rimAlpha) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        for (int ring = 0; ring < rings; ring++) {
            float lat0 = Mth.PI * ring / rings - Mth.HALF_PI;
            float lat1 = Mth.PI * (ring + 1) / rings - Mth.HALF_PI;
            for (int seg = 0; seg < segments; seg++) {
                float lon0 = Mth.TWO_PI * seg / segments;
                float lon1 = Mth.TWO_PI * (seg + 1) / segments;
                float u0 = tilesU * seg / segments + scroll;
                float u1 = tilesU * (seg + 1) / segments + scroll;
                float v0 = tilesV * ring / rings;
                float v1 = tilesV * (ring + 1) / rings;
                sphereVertex(pose, vc, center, radius, lat0, lon0, u0, v0, r, g, b, baseAlpha, rimAlpha);
                sphereVertex(pose, vc, center, radius, lat0, lon1, u1, v0, r, g, b, baseAlpha, rimAlpha);
                sphereVertex(pose, vc, center, radius, lat1, lon1, u1, v1, r, g, b, baseAlpha, rimAlpha);
                sphereVertex(pose, vc, center, radius, lat1, lon0, u0, v1, r, g, b, baseAlpha, rimAlpha);
            }
        }
    }

    /** Same sphere as {@link #sphere}, untextured, for a POSITION_COLOR render type. */
    public static void veil(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, int segments, int rings,
                            int rgb, float baseAlpha, float rimAlpha) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        for (int ring = 0; ring < rings; ring++) {
            float lat0 = Mth.PI * ring / rings - Mth.HALF_PI;
            float lat1 = Mth.PI * (ring + 1) / rings - Mth.HALF_PI;
            for (int seg = 0; seg < segments; seg++) {
                float lon0 = Mth.TWO_PI * seg / segments;
                float lon1 = Mth.TWO_PI * (seg + 1) / segments;
                veilVertex(pose, vc, center, radius, lat0, lon0, r, g, b, baseAlpha, rimAlpha);
                veilVertex(pose, vc, center, radius, lat0, lon1, r, g, b, baseAlpha, rimAlpha);
                veilVertex(pose, vc, center, radius, lat1, lon1, r, g, b, baseAlpha, rimAlpha);
                veilVertex(pose, vc, center, radius, lat1, lon0, r, g, b, baseAlpha, rimAlpha);
            }
        }
    }

    private static void veilVertex(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, float lat, float lon,
                                   float r, float g, float b, float baseAlpha, float rimAlpha) {
        float cosLat = Mth.cos(lat);
        float nx = cosLat * Mth.cos(lon);
        float ny = Mth.sin(lat);
        float nz = cosLat * Mth.sin(lon);
        float x = (float) center.x + nx * radius;
        float y = (float) center.y + ny * radius;
        float z = (float) center.z + nz * radius;
        vc.addVertex(pose, x, y, z).setColor(r, g, b, fresnel(nx, ny, nz, x, y, z, baseAlpha, rimAlpha));
    }

    /** Edge-on surfaces (normal perpendicular to the view direction) glow brightest. */
    private static float fresnel(float nx, float ny, float nz, float x, float y, float z, float baseAlpha, float rimAlpha) {
        float viewLen = Mth.sqrt(x * x + y * y + z * z);
        float facing = viewLen < 1.0E-4F ? 1F : Math.abs((nx * x + ny * y + nz * z) / viewLen);
        float rim = 1F - facing;
        return Mth.clamp(baseAlpha + rimAlpha * rim * rim, 0F, 1F);
    }

    private static void sphereVertex(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, float lat, float lon,
                                     float u, float v, float r, float g, float b, float baseAlpha, float rimAlpha) {
        float cosLat = Mth.cos(lat);
        float nx = cosLat * Mth.cos(lon);
        float ny = Mth.sin(lat);
        float nz = cosLat * Mth.sin(lon);
        float x = (float) center.x + nx * radius;
        float y = (float) center.y + ny * radius;
        float z = (float) center.z + nz * radius;
        vc.addVertex(pose, x, y, z).setUv(u, v).setColor(r, g, b, fresnel(nx, ny, nz, x, y, z, baseAlpha, rimAlpha));
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 p, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
    }

    /** A thin ring of the given radius round {@code center}, in the plane facing {@code normal}. */
    public static void ring(Matrix4f pose, VertexConsumer vc, Vec3 center, Vec3 normal, float radius, float width, int segments, int color) {
        Vec3 n = normal.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : normal.normalize();
        Vec3 u = n.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1.0E-6) {
            u = n.cross(new Vec3(1, 0, 0));
        }
        u = u.normalize();
        Vec3 v = n.cross(u).normalize();
        Vec3 prev = center.add(u.scale(radius));
        for (int i = 1; i <= segments; i++) {
            double a = Math.PI * 2 * i / segments;
            Vec3 next = center.add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius));
            ribbon(pose, vc, prev, next, width, color, color);
            prev = next;
        }
    }

    public static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0F, 1F) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private EnergyGeometry() {
    }
}
