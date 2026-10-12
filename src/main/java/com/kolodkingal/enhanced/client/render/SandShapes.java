package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * The shapes the effects of Apocalypse are made of, in camera-relative coordinates (the camera is at 0, 0, 0, as in {@link
 * EnergyGeometry}): flat rings, discs and lanes that lie on the ground, ridges that follow it, cones and funnels, a
 * cylinder, a sphere that can be clipped from the top, the hexagonal shards of a broken shield. Every method takes the pose
 * matrix and the buffer it is to be drawn into (a {@code POSITION_COLOR} one, or for the textured ones a {@code
 * POSITION_TEX_COLOR} one) and adds quads to it; nothing here touches the world or the clock. No other state: a shape is
 * a function of its arguments, so a game test can build one and count its quads.
 */
public final class SandShapes {
    public static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    // ---- flat things on the ground ----

    /** A ring that lies flat (a horizontal plane at the height of {@code center}), {@code width} wide. */
    public static void flatRing(Matrix4f pose, VertexConsumer vc, Vec3 center, double radius, double width, int segments, int color) {
        flatRing(pose, vc, center, radius, width, segments, color, color);
    }

    /** A flat ring whose colour runs from {@code inner} on its inside edge to {@code outer} on its outside edge. */
    public static void flatRing(Matrix4f pose, VertexConsumer vc, Vec3 center, double radius, double width, int segments, int inner, int outer) {
        double a = Math.max(0.0, radius - width * 0.5);
        double b = radius + width * 0.5;
        for (int i = 0; i < segments; i++) {
            double t0 = Mth.TWO_PI * i / segments;
            double t1 = Mth.TWO_PI * (i + 1) / segments;
            vertex(pose, vc, center.add(Math.cos(t0) * a, 0.0, Math.sin(t0) * a), inner);
            vertex(pose, vc, center.add(Math.cos(t0) * b, 0.0, Math.sin(t0) * b), outer);
            vertex(pose, vc, center.add(Math.cos(t1) * b, 0.0, Math.sin(t1) * b), outer);
            vertex(pose, vc, center.add(Math.cos(t1) * a, 0.0, Math.sin(t1) * a), inner);
        }
    }

    /** A flat disc, the colour running from {@code middle} to {@code edge}. */
    public static void flatDisc(Matrix4f pose, VertexConsumer vc, Vec3 center, double radius, int segments, int middle, int edge) {
        Vec3 c = center;
        for (int i = 0; i < segments; i++) {
            double t0 = Mth.TWO_PI * i / segments;
            double t1 = Mth.TWO_PI * (i + 1) / segments;
            vertex(pose, vc, c, middle);
            vertex(pose, vc, c, middle);
            vertex(pose, vc, center.add(Math.cos(t1) * radius, 0.0, Math.sin(t1) * radius), edge);
            vertex(pose, vc, center.add(Math.cos(t0) * radius, 0.0, Math.sin(t0) * radius), edge);
        }
    }

    /**
     * A flat lane: a rectangle from {@code from} along the unit direction {@code dir} (horizontal), {@code length} long and
     * {@code width} wide, whose colour runs from {@code near} at its start to {@code far} at its end.
     */
    public static void lane(Matrix4f pose, VertexConsumer vc, Vec3 from, Vec3 dir, double length, double width, int near, int far) {
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(width * 0.5);
        Vec3 end = from.add(dir.scale(length));
        vertex(pose, vc, from.subtract(side), near);
        vertex(pose, vc, from.add(side), near);
        vertex(pose, vc, end.add(side), far);
        vertex(pose, vc, end.subtract(side), far);
    }

    // ---- a ridge of sand ----

    /**
     * One slice of a ridge: the cross-section of a heap across a lane, at a point along it. A ridge is drawn as the quads
     * between two slices.
     *
     * @param base   the middle of the slice at the ground
     * @param side   the unit vector across the lane
     * @param width  the width of the lane
     * @param height the height of the crest in this slice
     */
    public record Slice(Vec3 base, Vec3 side, double width, double height) {
        /** The five points of the profile, from one edge over the crest to the other (a hump, steeper at the middle). */
        public Vec3[] profile() {
            double half = width * 0.5;
            return new Vec3[]{
                    base.subtract(side.scale(half)),
                    base.subtract(side.scale(half * 0.5)).add(0.0, height * 0.7, 0.0),
                    base.add(0.0, height, 0.0),
                    base.add(side.scale(half * 0.5)).add(0.0, height * 0.7, 0.0),
                    base.add(side.scale(half))};
        }
    }

    /**
     * The quads between two slices of a ridge, coloured by height (sand dark at the foot, light at the crest): {@code
     * fade0} and {@code fade1} are the alpha of the two slices (the tail of a ridge fades away).
     */
    public static void ridge(Matrix4f pose, VertexConsumer vc, Slice a, Slice b, float fade0, float fade1, float shimmer) {
        Vec3[] pa = a.profile();
        Vec3[] pb = b.profile();
        for (int i = 0; i < pa.length - 1; i++) {
            vertex(pose, vc, pa[i], ridgeColour(i, a, fade0, shimmer));
            vertex(pose, vc, pa[i + 1], ridgeColour(i + 1, a, fade0, shimmer));
            vertex(pose, vc, pb[i + 1], ridgeColour(i + 1, b, fade1, shimmer));
            vertex(pose, vc, pb[i], ridgeColour(i, b, fade1, shimmer));
        }
    }

    private static int ridgeColour(int profileIndex, Slice slice, float alpha, float shimmer) {
        // The foot of the heap is the shade of the ground, the crest the light of dry sand; a grain of shimmer on top.
        float up = slice.height() <= 1.0E-4 ? 0.0F : (float) ((profileIndex == 2 ? 1.0 : profileIndex == 1 || profileIndex == 3 ? 0.55 : 0.0));
        int rgb = mixRgb(SandPalette.SHADE, SandPalette.LIGHT, Mth.clamp(up + shimmer * 0.15F, 0.0F, 1.0F));
        return argb(rgb, alpha * (0.45F + 0.4F * up));
    }

    // ---- standing things ----

    /** A cone from a base circle (in the horizontal plane at {@code base}) to a point {@code height} above its middle. */
    public static void cone(Matrix4f pose, VertexConsumer vc, Vec3 base, double radius, double height, int segments, int colorBase, int colorTip) {
        Vec3 tip = base.add(0.0, height, 0.0);
        for (int i = 0; i < segments; i++) {
            double t0 = Mth.TWO_PI * i / segments;
            double t1 = Mth.TWO_PI * (i + 1) / segments;
            vertex(pose, vc, base.add(Math.cos(t0) * radius, 0.0, Math.sin(t0) * radius), colorBase);
            vertex(pose, vc, base.add(Math.cos(t1) * radius, 0.0, Math.sin(t1) * radius), colorBase);
            vertex(pose, vc, tip, colorTip);
            vertex(pose, vc, tip, colorTip);
        }
    }

    /**
     * A funnel (a cone cut off at the bottom, wider at the top) of horizontal rings, textured with a sand texture that
     * scrolls up and round: {@code turns} how many times the texture goes round the funnel, {@code scroll} the offset in
     * the texture, {@code twist} the radians the upper rings are turned against the lower (a whirl). Needs a
     * {@code POSITION_TEX_COLOR} buffer.
     */
    public static void funnel(Matrix4f pose, VertexConsumer vc, Vec3 base, double bottomRadius, double topRadius, double height, int segments, int rings,
                              float turns, float scroll, float twist, int rgb, float bottomAlpha, float topAlpha) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        for (int ring = 0; ring < rings; ring++) {
            double f0 = (double) ring / rings;
            double f1 = (double) (ring + 1) / rings;
            double r0 = Mth.lerp(f0, bottomRadius, topRadius);
            double r1 = Mth.lerp(f1, bottomRadius, topRadius);
            double y0 = height * f0;
            double y1 = height * f1;
            double w0 = twist * f0;
            double w1 = twist * f1;
            float a0 = Mth.lerp((float) f0, bottomAlpha, topAlpha);
            float a1 = Mth.lerp((float) f1, bottomAlpha, topAlpha);
            for (int seg = 0; seg < segments; seg++) {
                double t0 = Mth.TWO_PI * seg / segments;
                double t1 = Mth.TWO_PI * (seg + 1) / segments;
                float u0 = turns * seg / segments;
                float u1 = turns * (seg + 1) / segments;
                float v0 = (float) f0 * 2.0F - scroll;
                float v1 = (float) f1 * 2.0F - scroll;
                texVertex(pose, vc, base.add(Math.cos(t0 + w0) * r0, y0, Math.sin(t0 + w0) * r0), u0, v0, r, g, b, a0);
                texVertex(pose, vc, base.add(Math.cos(t1 + w0) * r0, y0, Math.sin(t1 + w0) * r0), u1, v0, r, g, b, a0);
                texVertex(pose, vc, base.add(Math.cos(t1 + w1) * r1, y1, Math.sin(t1 + w1) * r1), u1, v1, r, g, b, a1);
                texVertex(pose, vc, base.add(Math.cos(t0 + w1) * r1, y1, Math.sin(t0 + w1) * r1), u0, v1, r, g, b, a1);
            }
        }
    }

    /**
     * A tall wall in the shape of a circle (the sand wall round the arena), made of vertical strips textured with a sand
     * texture. The alpha falls off with the height. Needs a {@code POSITION_TEX_COLOR} buffer.
     *
     * @param first the first segment drawn and {@code step} how many to skip, so that a far wall can be drawn coarser
     */
    public static void wall(Matrix4f pose, VertexConsumer vc, Vec3 base, double radius, double height, int segments, int rows, float tilesAround, float tilesUp,
                            float scroll, int rgb, float bottomAlpha, float topAlpha, int first, int step) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        for (int seg = first; seg < segments; seg += step) {
            double t0 = Mth.TWO_PI * seg / segments;
            double t1 = Mth.TWO_PI * (seg + step) / segments;
            float u0 = tilesAround * seg / segments;
            float u1 = tilesAround * (seg + step) / segments;
            for (int row = 0; row < rows; row++) {
                float f0 = (float) row / rows;
                float f1 = (float) (row + 1) / rows;
                float a0 = Mth.lerp(f0, bottomAlpha, topAlpha);
                float a1 = Mth.lerp(f1, bottomAlpha, topAlpha);
                double y0 = height * f0;
                double y1 = height * f1;
                float v0 = f0 * tilesUp - scroll;
                float v1 = f1 * tilesUp - scroll;
                texVertex(pose, vc, base.add(Math.cos(t0) * radius, y0, Math.sin(t0) * radius), u0 + scroll * 0.3F, v0, r, g, b, a0);
                texVertex(pose, vc, base.add(Math.cos(t1) * radius, y0, Math.sin(t1) * radius), u1 + scroll * 0.3F, v0, r, g, b, a0);
                texVertex(pose, vc, base.add(Math.cos(t1) * radius, y1, Math.sin(t1) * radius), u1 + scroll * 0.3F, v1, r, g, b, a1);
                texVertex(pose, vc, base.add(Math.cos(t0) * radius, y1, Math.sin(t0) * radius), u0 + scroll * 0.3F, v1, r, g, b, a1);
            }
        }
    }

    /** A tall column of light (a ribbon-like cylinder) from {@code base} up, colour running from bottom to top. */
    public static void column(Matrix4f pose, VertexConsumer vc, Vec3 base, double radius, double height, int segments, int bottom, int top) {
        for (int seg = 0; seg < segments; seg++) {
            double t0 = Mth.TWO_PI * seg / segments;
            double t1 = Mth.TWO_PI * (seg + 1) / segments;
            vertex(pose, vc, base.add(Math.cos(t0) * radius, 0.0, Math.sin(t0) * radius), bottom);
            vertex(pose, vc, base.add(Math.cos(t1) * radius, 0.0, Math.sin(t1) * radius), bottom);
            vertex(pose, vc, base.add(Math.cos(t1) * radius, height, Math.sin(t1) * radius), top);
            vertex(pose, vc, base.add(Math.cos(t0) * radius, height, Math.sin(t0) * radius), top);
        }
    }

    // ---- a ring that faces the viewer (a portal) ----

    /**
     * The disc of a portal, standing upright and turned to face the direction {@code facing} (a unit vector, horizontal):
     * a ring of {@code shards} small flat shards, the swirl of them turning at {@code spin} radians, drawn as quads in the
     * plane of the ring. Each shard is a thin kite; their colour and alpha come from {@code colorOf(i)}.
     */
    public static void portalRing(Matrix4f pose, VertexConsumer vc, Vec3 center, Vec3 facing, double radius, int shards, double spin, double pulse,
                                  java.util.function.IntUnaryOperator colorOf) {
        Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
        for (int i = 0; i < shards; i++) {
            double angle = spin + Mth.TWO_PI * i / shards;
            // The shards drift in and out along the ring: each its own phase, so the ring seethes.
            double along = 0.9 + 0.1 * Math.sin(pulse * 2.0 + i * 1.7);
            double r0 = radius * (along - 0.12);
            double r1 = radius * (along + 0.1);
            double w = radius * 0.11;
            Vec3 p0 = center.add(right.scale(Math.cos(angle) * r0)).add(0.0, Math.sin(angle) * r0, 0.0);
            Vec3 p1 = center.add(right.scale(Math.cos(angle) * r1)).add(0.0, Math.sin(angle) * r1, 0.0);
            Vec3 across = right.scale(-Math.sin(angle) * w).add(0.0, Math.cos(angle) * w, 0.0);
            int color = colorOf.applyAsInt(i);
            vertex(pose, vc, p0.subtract(across.scale(0.4)), color);
            vertex(pose, vc, p1.subtract(across), color);
            vertex(pose, vc, p1.add(across), color);
            vertex(pose, vc, p0.add(across.scale(0.4)), color);
        }
    }

    // ---- the sphere of a shield ----

    /**
     * A textured, double-sided sphere that can be clipped from the top ({@code maxLat}, radians from the south pole: pi is
     * the whole sphere) and that is brighter at the top ({@code topBoost} adds that much alpha at the north pole). It is
     * {@link EnergyGeometry#sphere} with the two things a shield that knits itself up from the ground needs. Needs a
     * {@code POSITION_TEX_COLOR} buffer.
     */
    public static void dome(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, int segments, int rings, float tilesU, float tilesV, float scroll,
                            int rgb, float baseAlpha, float rimAlpha, float topBoost, float maxLat) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        int used = Math.max(1, Math.min(rings, (int) Math.ceil(rings * Mth.clamp(maxLat / Mth.PI, 0.0F, 1.0F))));
        for (int ring = 0; ring < used; ring++) {
            float lat0 = Mth.PI * ring / rings - Mth.HALF_PI;
            float lat1 = Math.min(Mth.PI * (ring + 1) / rings, maxLat) - Mth.HALF_PI;
            for (int seg = 0; seg < segments; seg++) {
                float lon0 = Mth.TWO_PI * seg / segments;
                float lon1 = Mth.TWO_PI * (seg + 1) / segments;
                float u0 = tilesU * seg / segments + scroll;
                float u1 = tilesU * (seg + 1) / segments + scroll;
                float v0 = tilesV * ring / rings;
                float v1 = tilesV * (ring + 1) / rings;
                domeVertex(pose, vc, center, radius, lat0, lon0, u0, v0, r, g, b, baseAlpha, rimAlpha, topBoost);
                domeVertex(pose, vc, center, radius, lat0, lon1, u1, v0, r, g, b, baseAlpha, rimAlpha, topBoost);
                domeVertex(pose, vc, center, radius, lat1, lon1, u1, v1, r, g, b, baseAlpha, rimAlpha, topBoost);
                domeVertex(pose, vc, center, radius, lat1, lon0, u0, v1, r, g, b, baseAlpha, rimAlpha, topBoost);
            }
        }
    }

    private static void domeVertex(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, float lat, float lon, float u, float v, float r, float g,
                                   float b, float baseAlpha, float rimAlpha, float topBoost) {
        float cosLat = Mth.cos(lat);
        float nx = cosLat * Mth.cos(lon);
        float ny = Mth.sin(lat);
        float nz = cosLat * Mth.sin(lon);
        float x = (float) center.x + nx * radius;
        float y = (float) center.y + ny * radius;
        float z = (float) center.z + nz * radius;
        float viewLength = Mth.sqrt(x * x + y * y + z * z);
        float facing = viewLength < 1.0E-4F ? 1.0F : Math.abs((nx * x + ny * y + nz * z) / viewLength);
        float rim = 1.0F - facing;
        float alpha = Mth.clamp(baseAlpha + rimAlpha * rim * rim + topBoost * Math.max(0.0F, ny), 0.0F, 1.0F);
        vc.addVertex(pose, x, y, z).setUv(u, v).setColor(r, g, b, alpha);
    }

    /** A point of a sphere, from its middle, in the direction {@code dir} (a unit vector). */
    public static Vec3 onSphere(Vec3 center, Vec3 dir, double radius) {
        return center.add(dir.scale(radius));
    }

    /**
     * A circle on the surface of a sphere: the points at the angle {@code arc} (radians) from the direction {@code axis}
     * (a unit vector), going all the way round. The ripple of a blow on a shield is one of these, growing.
     */
    public static List<Vec3> circleOnSphere(Vec3 center, double radius, Vec3 axis, double arc, int points) {
        Vec3 u = axis.cross(Math.abs(axis.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : UP).normalize();
        Vec3 v = axis.cross(u).normalize();
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i <= points; i++) {
            double t = Mth.TWO_PI * i / points;
            Vec3 dir = axis.scale(Math.cos(arc)).add(u.scale(Math.sin(arc) * Math.cos(t))).add(v.scale(Math.sin(arc) * Math.sin(t)));
            out.add(center.add(dir.scale(radius)));
        }
        return out;
    }

    /**
     * A crack on the surface of a sphere: a random walk of {@code steps} short steps from the direction {@code start}, each
     * turning a little, the same for the same seed. Returns the points.
     */
    public static List<Vec3> crackOnSphere(Vec3 center, double radius, Vec3 start, int steps, double stepArc, long seed) {
        RandomSource random = RandomSource.create(seed);
        List<Vec3> out = new ArrayList<>();
        Vec3 dir = start.normalize();
        Vec3 heading = dir.cross(UP);
        if (heading.lengthSqr() < 1.0E-6) {
            heading = dir.cross(new Vec3(1.0, 0.0, 0.0));
        }
        heading = heading.normalize();
        out.add(center.add(dir.scale(radius)));
        for (int i = 0; i < steps; i++) {
            double turn = (random.nextDouble() - 0.5) * 1.6;
            Vec3 side = dir.cross(heading).normalize();
            heading = heading.scale(Math.cos(turn)).add(side.scale(Math.sin(turn))).normalize();
            dir = dir.scale(Math.cos(stepArc)).add(heading.scale(Math.sin(stepArc))).normalize();
            heading = heading.subtract(dir.scale(heading.dot(dir))).normalize();
            out.add(center.add(dir.scale(radius)));
        }
        return out;
    }

    /** A hexagonal shard (a flat six-sided plate) at {@code at}, turned by the angles, {@code size} across; four quads. */
    public static void hexShard(Matrix4f pose, VertexConsumer vc, Vec3 at, Vec3 normal, double size, double roll, int color) {
        Vec3 n = normal.lengthSqr() < 1.0E-8 ? UP : normal.normalize();
        Vec3 u = n.cross(Math.abs(n.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : UP).normalize();
        Vec3 v = n.cross(u).normalize();
        Vec3[] p = new Vec3[6];
        for (int i = 0; i < 6; i++) {
            double t = roll + Math.PI / 3.0 * i;
            p[i] = at.add(u.scale(Math.cos(t) * size)).add(v.scale(Math.sin(t) * size));
        }
        // Three quads fan out from the middle: (c, p0, p1, p2), (c, p2, p3, p4), (c, p4, p5, p0).
        for (int i = 0; i < 6; i += 2) {
            vertex(pose, vc, at, color);
            vertex(pose, vc, p[i], color);
            vertex(pose, vc, p[(i + 1) % 6], color);
            vertex(pose, vc, p[(i + 2) % 6], color);
        }
    }

    // ---- helpers ----

    /** A colour between two RGB colours. */
    public static int mixRgb(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return ((int) Mth.lerp(t, ar, br) << 16) | ((int) Mth.lerp(t, ag, bg) << 8) | (int) Mth.lerp(t, ab, bb);
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 p, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
    }

    private static void texVertex(Matrix4f pose, VertexConsumer vc, Vec3 p, float u, float v, float r, float g, float b, float a) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(r, g, b, a);
    }

    private SandShapes() {
    }
}
