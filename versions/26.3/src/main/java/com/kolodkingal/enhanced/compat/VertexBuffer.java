package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * 26.3 (as 1.21.5's): there are no retained vertex buffers of 1.21.1's kind; this keeps a mesh's vertices and gives
 * them again in every frame to the level stage's buffers ({@link Gl#drawInStage}), in the render type {@link Gl#use}
 * named or the one {@link #drawAs} is given.
 */
public final class VertexBuffer implements AutoCloseable {
    private ByteBuffer vertices;
    private VertexFormat format;

    public VertexBuffer(Object usage) {
    }

    public void bind() {
    }

    public static void unbind() {
    }

    public void upload(MeshData mesh) {
        close();
        ByteBuffer from = mesh.vertexBuffer();
        vertices = MemoryUtil.memAlloc(from.remaining());
        MemoryUtil.memCopy(from, vertices);
        format = mesh.drawState().format();
        mesh.close();
    }

    public void drawWithShader(Matrix4f modelView, Matrix4f projection, Object shader) {
        if (vertices == null) {
            return;
        }
        if (Gl.inSky()) {
            Gl.drawNow(vertices, format, modelView);
        } else {
            Gl.drawInStage(vertices, format, modelView, 0, 0, 0);
        }
    }

    /** Draws the mesh in {@code type}, moved by ({@code x}, {@code y}, {@code z}) (the stage's space). */
    public void drawAs(net.minecraft.client.renderer.rendertype.RenderType type, float x, float y, float z) {
        if (vertices == null) {
            return;
        }
        Gl.use(type);
        try {
            Gl.drawInStage(vertices, format, null, x, y, z);
        } finally {
            Gl.use(null);
        }
    }

    @Override
    public void close() {
        if (vertices != null) {
            MemoryUtil.memFree(vertices);
            vertices = null;
        }
    }
}
