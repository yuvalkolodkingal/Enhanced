package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * 1.21.11 (as 1.21.5): there are no retained vertex buffers of 1.21.1's kind; this keeps a mesh's vertices and draws them again in
 * every frame through the render type ({@link Gl#use} or the state {@link Gl} was told), as the game draws its own
 * entities.
 */
public final class VertexBuffer implements AutoCloseable {
    private ByteBuffer vertices;
    private MeshData.DrawState state;

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
        state = mesh.drawState();
        mesh.close();
    }

    public void drawWithShader(Matrix4f modelView, Matrix4f projection, Object shader) {
        MeshData mesh = copy();
        if (mesh != null) {
            Gl.draw(mesh, modelView, projection);
        }
    }

    /** (1.21.11) Draws the mesh in {@code type}, moved by ({@code x}, {@code y}, {@code z}) in the model-view. */
    public void drawAs(net.minecraft.client.renderer.rendertype.RenderType type, float x, float y, float z) {
        MeshData mesh = copy();
        if (mesh == null) {
            return;
        }
        org.joml.Matrix4fStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.translate(x, y, z);
        try {
            type.draw(mesh);
        } finally {
            stack.popMatrix();
        }
    }

    private MeshData copy() {
        if (vertices == null || state == null) {
            return null;
        }
        int size = vertices.remaining();
        ByteBufferBuilder memory = new ByteBufferBuilder(size);
        long at = memory.reserve(size);
        MemoryUtil.memCopy(MemoryUtil.memAddress(vertices), at, size);
        ByteBufferBuilder.Result built = memory.build();
        if (built == null) {
            memory.close();
            return null;
        }
        // (the builder's memory goes when the mesh is closed after its draw)
        return new MeshData(built, state);
    }

    @Override
    public void close() {
        if (vertices != null) {
            MemoryUtil.memFree(vertices);
            vertices = null;
        }
    }
}
