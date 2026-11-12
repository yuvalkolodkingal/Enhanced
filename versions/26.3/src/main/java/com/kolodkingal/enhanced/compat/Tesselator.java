package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;

/** 26.3 only: 1.21.1's {@code Tesselator}, one reused buffer to build an immediate mesh in (for {@link Gl#draw}). */
public final class Tesselator {
    private static final Tesselator INSTANCE = new Tesselator();
    private final ByteBufferBuilder buffer = new ByteBufferBuilder(786432);

    private Tesselator() {
    }

    public static Tesselator getInstance() {
        return INSTANCE;
    }

    public BufferBuilder begin(PrimitiveTopology topology, VertexFormat format) {
        return new BufferBuilder(buffer, topology, format);
    }
}
