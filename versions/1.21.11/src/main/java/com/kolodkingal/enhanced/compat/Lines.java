package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;

/** 1.21.11 only: 1.21.4's {@code ShapeRenderer.renderLineBox} (gone from 1.21.11): a box's twelve edges as lines. */
public final class Lines {
    private Lines() {
    }

    public static void box(PoseStack pose, VertexConsumer vc, AABB box, float r, float g, float b, float a) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        PoseStack.Pose p = pose.last();
        line(p, vc, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(p, vc, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(p, vc, x0, y0, z0, x0, y0, z1, r, g, b, a);
        line(p, vc, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(p, vc, x1, y1, z0, x0, y1, z0, r, g, b, a);
        line(p, vc, x0, y1, z0, x0, y1, z1, r, g, b, a);
        line(p, vc, x0, y1, z1, x0, y0, z1, r, g, b, a);
        line(p, vc, x0, y0, z1, x1, y0, z1, r, g, b, a);
        line(p, vc, x1, y0, z1, x1, y0, z0, r, g, b, a);
        line(p, vc, x0, y1, z1, x1, y1, z1, r, g, b, a);
        line(p, vc, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(p, vc, x1, y1, z0, x1, y1, z1, r, g, b, a);
    }

    private static void line(PoseStack.Pose p, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz,
                             float r, float g, float b, float a) {
        float nx = bx - ax, ny = by - ay, nz = bz - az;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0F) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        vc.addVertex(p, ax, ay, az).setColor(r, g, b, a).setNormal(p, nx, ny, nz).setLineWidth(2.0F);
        vc.addVertex(p, bx, by, bz).setColor(r, g, b, a).setNormal(p, nx, ny, nz).setLineWidth(2.0F);
    }
}
