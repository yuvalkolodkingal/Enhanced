package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The shapes of the Ultron fight that are drawn as geometry: rings, discs and cracks laid over the ground (draped over its
 * bumps, so a tell on uneven ground is still a whole ring), pillars of light, a glow that faces the camera, a silhouette
 * built from small boxes, flat-shaded boxes for a pile of scrap. Everything is written in camera-relative coordinates (the
 * camera sits at 0, 0, 0), like {@link EnergyGeometry}, into a vertex consumer of {@link EnhancedRenderTypes#ENERGY_BEAM}
 * (additive, so light adds up) or {@link EnhancedRenderTypes#ENERGY_VEIL} (alpha, for the dark scrap). Colours are ARGB.
 */
final class UltronGeometry {
    /** How far above the ground a decal lies, so it is not lost in the surface. */
    private static final float LIFT = 0.05F;

    /** What the ground is at a point: the height a decal at {@code (x, z)} lies at. */
    @FunctionalInterface
    interface Ground {
        float at(double x, double z);
    }

    /**
     * The ground around a point as a decal sees it: the top of the highest solid block of the column, as long as it is
     * within a couple of blocks of {@code around} (a tree, a roof or a hole is not the ground a tell is laid on).
     */
    static Ground ground(ClientLevel level, double around) {
        return (x, z) -> {
            BlockPos pos = BlockPos.containing(x, around, z);
            if (!level.hasChunkAt(pos)) {
                return (float) around + LIFT;
            }
            double top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
            return (float) (top > around + 2.5 || top < around - 3.0 ? around : top) + LIFT;
        };
    }

    // ---- laid on the ground ----

    /** A ring of {@code radius} and {@code width} on the ground round (cx, cz), in {@code segments} pieces, one colour. */
    static void ring(Matrix4f pose, VertexConsumer vc, Vec3 camera, Ground ground, double cx, double cz, float radius, float width,
                     int segments, int colour) {
        float inner = Math.max(0.0F, radius - width * 0.5F);
        float outer = radius + width * 0.5F;
        double ax = 0;
        double az = 0;
        float ay = 0;
        double bx = 0;
        double bz = 0;
        float by = 0;
        for (int i = 0; i <= segments; i++) {
            double angle = Mth.TWO_PI * i / segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double ix = cx + cos * inner;
            double iz = cz + sin * inner;
            double ox = cx + cos * outer;
            double oz = cz + sin * outer;
            float iy = ground.at(ix, iz);
            float oy = ground.at(ox, oz);
            if (i > 0) {
                vertex(pose, vc, camera, ax, ay, az, colour);
                vertex(pose, vc, camera, bx, by, bz, colour);
                vertex(pose, vc, camera, ox, oy, oz, colour);
                vertex(pose, vc, camera, ix, iy, iz, colour);
            }
            ax = ix;
            ay = iy;
            az = iz;
            bx = ox;
            by = oy;
            bz = oz;
        }
    }

    /** A disc on the ground, brighter toward the middle (or toward the edge, with {@code edgeAlpha} the larger). */
    static void disc(Matrix4f pose, VertexConsumer vc, Vec3 camera, Ground ground, double cx, double cz, float radius, int segments,
                     int centreColour, int edgeColour) {
        float centreY = ground.at(cx, cz);
        double px = 0;
        double pz = 0;
        float py = 0;
        for (int i = 0; i <= segments; i++) {
            double angle = Mth.TWO_PI * i / segments;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            float y = ground.at(x, z);
            if (i > 0) {
                vertex(pose, vc, camera, cx, centreY, cz, centreColour);
                vertex(pose, vc, camera, px, py, pz, edgeColour);
                vertex(pose, vc, camera, x, y, z, edgeColour);
                vertex(pose, vc, camera, cx, centreY, cz, centreColour);
            }
            px = x;
            pz = z;
            py = y;
        }
    }

    /** A thin line laid on the ground from one point to another (a crack, a mark), as a ribbon of {@code width}. */
    static void line(Matrix4f pose, VertexConsumer vc, Vec3 camera, Ground ground, double x0, double z0, double x1, double z1, float width,
                     int colour0, int colour1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) {
            return;
        }
        double nx = -dz / length * width * 0.5;
        double nz = dx / length * width * 0.5;
        float y0 = ground.at(x0, z0);
        float y1 = ground.at(x1, z1);
        vertex(pose, vc, camera, x0 - nx, y0, z0 - nz, colour0);
        vertex(pose, vc, camera, x0 + nx, y0, z0 + nz, colour0);
        vertex(pose, vc, camera, x1 + nx, y1, z1 + nz, colour1);
        vertex(pose, vc, camera, x1 - nx, y1, z1 - nz, colour1);
    }

    /** A jagged crack running outward from (cx, cz) along an angle, {@code length} long, thinning to nothing. */
    static void crack(Matrix4f pose, VertexConsumer vc, Vec3 camera, Ground ground, double cx, double cz, double angle, float length,
                      float width, int colour, long seed) {
        RandomSource random = RandomSource.create(seed);
        double x = cx;
        double z = cz;
        int pieces = Math.max(2, (int) (length * 2.0));
        double heading = angle;
        for (int i = 0; i < pieces; i++) {
            heading += (random.nextDouble() - 0.5) * 0.9;
            double step = length / pieces;
            double nx = x + Math.cos(heading) * step;
            double nz = z + Math.sin(heading) * step;
            float fade = 1.0F - (float) i / pieces;
            line(pose, vc, camera, ground, x, z, nx, nz, width * fade, EnergyGeometry.argb(colour & 0xFFFFFF, alpha(colour) * fade),
                    EnergyGeometry.argb(colour & 0xFFFFFF, alpha(colour) * (fade - 1.0F / pieces)));
            x = nx;
            z = nz;
        }
    }

    // ---- standing in the air ----

    /** A column of light: a cylinder of {@code radius} and {@code height} standing on (x, y, z), brightest at its foot. */
    static void pillar(Matrix4f pose, VertexConsumer vc, Vec3 camera, double x, double y, double z, float radius, float height, int sides,
                       int footColour, int topColour) {
        for (int i = 0; i < sides; i++) {
            double a0 = Mth.TWO_PI * i / sides;
            double a1 = Mth.TWO_PI * (i + 1) / sides;
            double x0 = x + Math.cos(a0) * radius;
            double z0 = z + Math.sin(a0) * radius;
            double x1 = x + Math.cos(a1) * radius;
            double z1 = z + Math.sin(a1) * radius;
            vertex(pose, vc, camera, x0, y, z0, footColour);
            vertex(pose, vc, camera, x1, y, z1, footColour);
            vertex(pose, vc, camera, x1, y + height, z1, topColour);
            vertex(pose, vc, camera, x0, y + height, z0, topColour);
        }
    }

    /**
     * A glow that faces the camera: rings of the halo and the body colour, thicker toward the middle, a core, and four thin
     * spikes through it. {@code centre} is relative to the camera.
     */
    static void flare(Matrix4f pose, VertexConsumer vc, Vec3 centre, float radius, float alpha, int halo, int body, int core) {
        if (radius <= 0.0F || alpha <= 0.0F) {
            return;
        }
        Vec3 toCamera = centre.scale(-1.0);
        Vec3 side = toCamera.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(toCamera.normalize()).normalize();
        EnergyGeometry.ring(pose, vc, centre, toCamera, radius * 0.8F, radius * 0.45F, 16, EnergyGeometry.argb(halo, 0.16F * alpha));
        EnergyGeometry.ring(pose, vc, centre, toCamera, radius * 0.5F, radius * 0.4F, 16, EnergyGeometry.argb(body, 0.3F * alpha));
        EnergyGeometry.ring(pose, vc, centre, toCamera, radius * 0.22F, radius * 0.3F, 12, EnergyGeometry.argb(core, 0.85F * alpha));
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 4.0;
            Vec3 spike = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).scale(radius * (i % 2 == 0 ? 1.5 : 1.0));
            EnergyGeometry.ribbon(pose, vc, centre.subtract(spike), centre.add(spike), radius * 0.07F, EnergyGeometry.argb(body, 0.6F * alpha),
                    EnergyGeometry.argb(body, 0.6F * alpha));
        }
    }

    /** A small square facing the camera (a data mote): {@code size} across. */
    static void square(Matrix4f pose, VertexConsumer vc, Vec3 centre, float size, int colour) {
        Vec3 toCamera = centre.scale(-1.0);
        Vec3 side = toCamera.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(toCamera.normalize()).normalize();
        Vec3 u = side.scale(size * 0.5);
        Vec3 v = up.scale(size * 0.5);
        vertex(pose, vc, centre.subtract(u).subtract(v), colour);
        vertex(pose, vc, centre.add(u).subtract(v), colour);
        vertex(pose, vc, centre.add(u).add(v), colour);
        vertex(pose, vc, centre.subtract(u).add(v), colour);
    }

    /** A dotted line from {@code a} to {@code b}: dashes of {@code dash} blocks with gaps of the same, moving by {@code phase}. */
    static void dotted(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, float dash, float phase, int colour) {
        Vec3 delta = b.subtract(a);
        double length = delta.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / length);
        double pitch = dash * 2.0;
        for (double from = -((phase % 1.0 + 1.0) % 1.0) * pitch; from < length; from += pitch) {
            double start = Math.max(0.0, from);
            double end = Math.min(length, from + dash);
            if (end > start) {
                EnergyGeometry.ribbon(pose, vc, a.add(dir.scale(start)), a.add(dir.scale(end)), width, colour, colour);
            }
        }
    }

    // ---- boxes ----

    /**
     * A box (a part of the silhouette that assembles, a piece of scrap): {@code centre} and half sizes in camera-relative
     * coordinates, turned by {@code yaw}, each face its own shade of the colour (the top lightest) so it reads as solid.
     */
    static void box(Matrix4f pose, VertexConsumer vc, Vec3 centre, float hx, float hy, float hz, float yaw, int colour) {
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        for (int face = 0; face < 6; face++) {
            float shade = switch (face) {
                case 0 -> 1.0F;
                case 1 -> 0.45F;
                case 2, 3 -> 0.8F;
                default -> 0.65F;
            };
            int c = shadeColour(colour, shade);
            for (int corner = 0; corner < 4; corner++) {
                float[] p = faceCorner(face, corner);
                float x = p[0] * hx;
                float y = p[1] * hy;
                float z = p[2] * hz;
                vertex(pose, vc, centre.x + x * cos - z * sin, centre.y + y, centre.z + x * sin + z * cos, c);
            }
        }
    }

    private static final float[][][] FACES = {
            {{-1, 1, -1}, {1, 1, -1}, {1, 1, 1}, {-1, 1, 1}},
            {{-1, -1, 1}, {1, -1, 1}, {1, -1, -1}, {-1, -1, -1}},
            {{-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}},
            {{1, -1, 1}, {-1, -1, 1}, {-1, 1, 1}, {1, 1, 1}},
            {{-1, -1, 1}, {-1, -1, -1}, {-1, 1, -1}, {-1, 1, 1}},
            {{1, -1, -1}, {1, -1, 1}, {1, 1, 1}, {1, 1, -1}}};

    private static float[] faceCorner(int face, int corner) {
        return FACES[face][corner];
    }

    /**
     * A standing figure of boxes, in the proportions of the model (legs, torso, head, two arms), built from the feet up to
     * {@code built} of its {@code height} (0 to 1): a body being assembled. {@code feet} is relative to the camera.
     */
    static void silhouette(Matrix4f pose, VertexConsumer vc, Vec3 feet, float height, float yaw, float built, int colour) {
        float unit = height / 29.0F;
        float limit = built * height;
        // (x centre, y bottom, z centre, half width, height, half depth) in model units, y up from the feet.
        float[][] boxes = {
                {-2.5F, 0, 0, 2, 11, 2}, {2.5F, 0, 0, 2, 11, 2},
                {0, 11, 0, 5, 10, 2.5F},
                {0, 21, 0, 3.5F, 1, 1.5F},
                {0, 22, 0, 3.5F, 7, 3.5F},
                {-6.5F, 9, 0, 1.5F, 12, 1.5F}, {6.5F, 9, 0, 1.5F, 12, 1.5F}};
        for (float[] b : boxes) {
            float bottom = b[1] * unit;
            if (bottom >= limit) {
                continue;
            }
            float top = Math.min(limit, (b[1] + b[4]) * unit);
            float halfHeight = (top - bottom) * 0.5F;
            float cos = Mth.cos(yaw);
            float sin = Mth.sin(yaw);
            double x = b[0] * unit * cos - b[2] * unit * sin;
            double z = b[0] * unit * sin + b[2] * unit * cos;
            box(pose, vc, new Vec3(feet.x + x, feet.y + bottom + halfHeight, feet.z + z), b[3] * unit, halfHeight, b[5] * unit, yaw, colour);
        }
    }

    // ---- helpers ----

    private static float alpha(int argb) {
        return ((argb >>> 24) & 0xFF) / 255.0F;
    }

    private static int shadeColour(int argb, float shade) {
        int a = argb >>> 24;
        int r = (int) (((argb >> 16) & 0xFF) * shade);
        int g = (int) (((argb >> 8) & 0xFF) * shade);
        int b = (int) ((argb & 0xFF) * shade);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 camera, double x, double y, double z, int colour) {
        vc.addVertex(pose, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z)).setColor(colour);
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 p, int colour) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(colour);
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, double x, double y, double z, int colour) {
        vc.addVertex(pose, (float) x, (float) y, (float) z).setColor(colour);
    }

    private UltronGeometry() {
    }
}
