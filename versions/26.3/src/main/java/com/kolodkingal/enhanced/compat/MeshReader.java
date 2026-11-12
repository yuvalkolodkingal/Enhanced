package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 26.3 only: reads a built mesh's vertices back (position, colour, uv, overlay, light, normal, as its format has them)
 * and writes them into a vertex consumer, moved by a matrix and tinted: how the mod's meshes, which 1.21.1 drew as they
 * were, are drawn into 26.3's submitted geometry.
 */
public final class MeshReader {
    private MeshReader() {
    }

    /** Writes the vertices of {@code data} ({@code format}) into {@code out}; {@code tint} multiplies the colour (ARGB). */
    public static void replay(ByteBuffer data, VertexFormat format, Matrix4f transform, int tint, VertexConsumer out) {
        ByteBuffer b = data.duplicate().order(ByteOrder.nativeOrder());
        int base = b.position();
        int size = format.getVertexSize();
        if (size <= 0) {
            return;
        }
        int count = b.remaining() / size;
        VertexFormatElement pos = format.getElement("Position");
        VertexFormatElement col = format.getElement("Color");
        VertexFormatElement uv0 = format.getElement("UV0");
        VertexFormatElement uv1 = format.getElement("UV1");
        VertexFormatElement uv2 = format.getElement("UV2");
        VertexFormatElement nor = format.getElement("Normal");
        float ta = (tint >>> 24) / 255.0F, tr = (tint >> 16 & 255) / 255.0F, tg = (tint >> 8 & 255) / 255.0F, tb = (tint & 255) / 255.0F;
        Vector4f p = new Vector4f();
        Vector3f n = new Vector3f();
        for (int k = 0; k < count; k++) {
            int v = base + k * size;
            if (pos == null) {
                return;
            }
            p.set(b.getFloat(v + pos.offset()), b.getFloat(v + pos.offset() + 4), b.getFloat(v + pos.offset() + 8), 1.0F);
            if (transform != null) {
                transform.transform(p);
            }
            out.addVertex(p.x, p.y, p.z);
            int r = 255, g = 255, bl = 255, a = 255;
            if (col != null) {
                r = b.get(v + col.offset()) & 255;
                g = b.get(v + col.offset() + 1) & 255;
                bl = b.get(v + col.offset() + 2) & 255;
                a = b.get(v + col.offset() + 3) & 255;
            }
            out.setColor(Math.round(r * tr), Math.round(g * tg), Math.round(bl * tb), Math.round(a * ta));
            if (uv0 != null) {
                out.setUv(b.getFloat(v + uv0.offset()), b.getFloat(v + uv0.offset() + 4));
            }
            if (uv1 != null) {
                out.setUv1(b.getShort(v + uv1.offset()), b.getShort(v + uv1.offset() + 2));
            }
            if (uv2 != null) {
                out.setUv2(b.getShort(v + uv2.offset()), b.getShort(v + uv2.offset() + 2));
            }
            if (nor != null) {
                n.set(b.get(v + nor.offset()) / 127.0F, b.get(v + nor.offset() + 1) / 127.0F, b.get(v + nor.offset() + 2) / 127.0F);
                if (transform != null) {
                    transform.transformDirection(n);
                    n.normalize();
                }
                out.setNormal(n.x, n.y, n.z);
            }
        }
    }
}
