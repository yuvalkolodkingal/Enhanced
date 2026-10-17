package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

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
        polyline(pose, vc, boltPoints(a, b, jitter, RandomSource.create(seed)), width, color);
    }

    /**
     * A crackling bolt that never jumps (3.3): its shape flows from shape {@code floor(phase)} of {@code seed} to the
     * next as {@code phase} grows by one, so a tether or a field line writhes instead of flickering.
     *
     * @param phase the time in shapes (for example the time in ticks divided by the ticks one shape lasts)
     */
    public static void bolt(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, float jitter, int color,
            long seed, float phase) {
        long step = Mth.floor(phase);
        polyline(pose, vc, blend(boltPoints(a, b, jitter, shape(seed, step)), boltPoints(a, b, jitter, shape(seed,
                step + 1)), flow(phase)),
                width, color);
    }

    private static List<Vec3> boltPoints(Vec3 a, Vec3 b, double jitter, RandomSource random) {
        double length = a.distanceTo(b);
        int segments = Math.max(2, (int) (length * 2.0));
        Vec3 dir = b.subtract(a);
        Vec3 perp1 = dir.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 1.0E-6) {
            perp1 = dir.cross(new Vec3(1, 0, 0));
        }
        perp1 = perp1.normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();
        List<Vec3> points = new ArrayList<>(segments + 1);
        points.add(a);
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 next = a.add(dir.scale(t));
            if (i < segments) {
                double amount = jitter * Math.sin(t * Math.PI);
                next = next.add(perp1.scale((random.nextDouble() - 0.5) * 2 * amount)).add(perp2.scale((random.nextDouble() - 0.5) * 2 * amount));
            }
            points.add(next);
        }
        return points;
    }

    /**
     * The random source of one shape of a crackling effect (3.3): shape {@code step} of the effect {@code seed}. A
     * crackle
     * draws shape {@code floor(phase)} flowing or fading into the next, never jumping from one to the other.
     */
    public static RandomSource shape(long seed, long step) {
        return RandomSource.create(seed + step * 0x9E3779B97F4A7C15L);
    }

    /** How far shape {@code floor(phase)} has flowed into the next one, eased (0 to 1). */
    public static float flow(float phase) {
        float f = phase - Mth.floor(phase);
        return f * f * (3.0F - 2.0F * f);
    }

    /**
     * Arcs that appear at random places without popping (3.3): {@code draw} is called for shape {@code floor(phase)}
     * with the weight {@code 1 - f} and for the next shape with the weight {@code f} ({@link #flow}), each with its own
     * random source, so every arc fades in over one step and out over the next. Multiply the alpha of what is drawn by
     * the weight.
     */
    public static void crossfade(long seed, float phase, Shapes draw) {
        long step = Mth.floor(phase);
        float f = flow(phase);
        if (f < 1.0F) {
            draw.draw(shape(seed, step), 1.0F - f);
        }
        if (f > 0.0F) {
            draw.draw(shape(seed, step + 1), f);
        }
    }

    /** One shape of a crossfading crackle ({@link #crossfade}). */
    @FunctionalInterface
    public interface Shapes {
        void draw(RandomSource random, float weight);
    }

    /** Two paths of the same length, {@code f} of the way from the first to the second. */
    private static List<Vec3> blend(List<Vec3> from, List<Vec3> to, float f) {
        if (from.size() != to.size()) {
            return f < 0.5F ? from : to;
        }
        List<Vec3> path = new ArrayList<>(from.size());
        for (int i = 0; i < from.size(); i++) {
            path.add(from.get(i).lerp(to.get(i), f));
        }
        return path;
    }

    /**
     * A jagged lightning path that flows from one random shape to the next as {@code phase} grows by one (3.3), instead
     * of jumping to a new shape: a held beam writhes but never strobes. See {@link #lightningPath}.
     */
    public static List<Vec3> flowingPath(Vec3 a, Vec3 b, double jitter, long seed, float phase) {
        long step = Mth.floor(phase);
        return blend(lightningPath(a, b, jitter, shape(seed, step)), lightningPath(a, b, jitter, shape(seed,
                step + 1)), flow(phase));
    }

    /**
     * A jagged lightning path from {@code a} to {@code b}, by midpoint displacement: the middle is kinked sideways by up
     * to {@code jitter}, the middle of each half by half that, and so on, so it zigzags at every scale. The same random
     * source gives the same path.
     */
    public static List<Vec3> lightningPath(Vec3 a, Vec3 b, double jitter, RandomSource random) {
        List<Vec3> points = new ArrayList<>();
        points.add(a);
        int depth = Mth.clamp(Mth.ceillog2(Math.max(2, (int) (a.distanceTo(b) * 3.0))), 1, 6);
        subdivide(points, a, b, jitter, depth, random);
        return points;
    }

    private static void subdivide(List<Vec3> points, Vec3 a, Vec3 b, double jitter, int depth, RandomSource random) {
        if (depth == 0) {
            points.add(b);
            return;
        }
        Vec3 dir = b.subtract(a);
        Vec3 perp1 = dir.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 1.0E-6) {
            perp1 = dir.cross(new Vec3(1, 0, 0));
        }
        perp1 = perp1.normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();
        Vec3 mid = a.add(b).scale(0.5)
                .add(perp1.scale((random.nextDouble() - 0.5) * 2 * jitter))
                .add(perp2.scale((random.nextDouble() - 0.5) * 2 * jitter));
        subdivide(points, a, mid, jitter * 0.5, depth - 1, random);
        subdivide(points, mid, b, jitter * 0.5, depth - 1, random);
    }

    /** A line through all the given points, as camera-facing ribbons. */
    public static void polyline(Matrix4f pose, VertexConsumer vc, List<Vec3> points, float width, int color) {
        for (int i = 1; i < points.size(); i++) {
            ribbon(pose, vc, points.get(i - 1), points.get(i), width, color, color);
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
