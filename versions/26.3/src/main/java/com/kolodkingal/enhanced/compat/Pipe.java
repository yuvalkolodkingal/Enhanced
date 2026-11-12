package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * 26.3 only: a render pipeline built the way 1.21.11's builder was (blend, depth test, depth write, depth bias, a
 * vertex format with its mode, a sampler by name). 26.3 states the depth and the colour target as a whole, its depth
 * is reversed (greater is nearer: a bias toward the eye is positive) and the uniforms are bind groups.
 */
public final class Pipe {
    /** 1.21.11's {@code MATRICES_PROJECTION_SNIPPET}: the projection and the transforms, no fog. */
    public static final RenderPipeline.Snippet MATRICES_PROJECTION = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .buildSnippet();

    private final RenderPipeline.Builder b;
    private boolean depthTest = true;
    private boolean depthWrite = true;
    private float biasScale;
    private float biasConstant;
    private BlendFunction blend;
    private boolean colorWrite = true;

    private Pipe(RenderPipeline.Builder b) {
        this.b = b;
    }

    public static Pipe builder(RenderPipeline.Snippet... snippets) {
        return new Pipe(RenderPipeline.builder(snippets));
    }

    public Pipe withLocation(Identifier location) {
        b.withLocation(location);
        return this;
    }

    public Pipe withLocation(String location) {
        b.withLocation(location);
        return this;
    }

    public Pipe withVertexShader(String shader) {
        b.withVertexShader(shader);
        return this;
    }

    public Pipe withFragmentShader(String shader) {
        b.withFragmentShader(shader);
        return this;
    }

    public Pipe withShaderDefine(String key) {
        b.withShaderDefine(key);
        return this;
    }

    /** A sampler uniform by its 1.21.1 name (Sampler0, Sampler1, Sampler2). */
    public Pipe withSampler(String name) {
        b.withBindGroupLayout(switch (name) {
            case "Sampler1" -> BindGroupLayouts.SAMPLER1;
            case "Sampler2" -> BindGroupLayouts.SAMPLER2;
            default -> BindGroupLayouts.SAMPLER0;
        });
        return this;
    }

    public Pipe withBlend(BlendFunction function) {
        blend = function;
        return this;
    }

    public Pipe withoutBlend() {
        blend = null;
        return this;
    }

    public Pipe withDepthWrite(boolean write) {
        depthWrite = write;
        return this;
    }

    /** Whether the depth is tested (1.21.11's LEQUAL test, or NO_DEPTH_TEST). */
    public Pipe withDepthTest(boolean test) {
        depthTest = test;
        return this;
    }

    /** 1.21.11's bias (negative toward the eye there). */
    public Pipe withDepthBias(float scale, float constant) {
        biasScale = -scale;
        biasConstant = -constant;
        return this;
    }

    public Pipe withColorWrite(boolean write) {
        colorWrite = write;
        return this;
    }

    public Pipe withCull(boolean cull) {
        b.withCull(cull);
        return this;
    }

    public Pipe withVertexFormat(VertexFormat format, PrimitiveTopology topology) {
        b.withVertexBinding(0, format);
        b.withPrimitiveTopology(topology);
        return this;
    }

    public RenderPipeline build() {
        b.withDepthStencilState(new DepthStencilState(depthTest ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS,
                depthWrite, biasScale, biasConstant));
        b.withColorTargetState(new ColorTargetState(Optional.ofNullable(blend), GpuFormat.RGBA8_UNORM,
                colorWrite ? ColorTargetState.WRITE_ALL : ColorTargetState.WRITE_NONE));
        return b.build();
    }
}
