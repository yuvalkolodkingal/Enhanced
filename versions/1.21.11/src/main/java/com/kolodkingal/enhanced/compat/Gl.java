package com.kolodkingal.enhanced.compat;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * 1.21.11 (after 1.21.5's): the GL state calls of 1.21.1's immediate drawing (a shader, blending, the depth test and
 * mask, culling, the bound texture, the shader colour) are gone; a draw is a render pipeline. The mod's few places that
 * still draw the old way (the skies of Asgard and Eternity, Build Magic's ghost and glow, Sokovia's mesh) tell this
 * class the state as before, and {@link #draw} draws the mesh at once with a pipeline made for that state (made once,
 * kept), the shader colour ({@link GuiTint}) as the colour modulator.
 */
public final class Gl {
    /** The shaders the mod drew with, by their 1.21.1 names. */
    public enum Shader {
        POSITION("core/position", false), POSITION_COLOR("core/position_color", false),
        POSITION_TEX("core/position_tex", true), POSITION_TEX_COLOR("core/position_tex_color", true);

        final String program;
        final boolean textured;

        Shader(String program, boolean textured) {
            this.program = program;
            this.textured = textured;
        }
    }

    public enum SourceFactor { SRC_ALPHA, ONE, ONE_MINUS_SRC_ALPHA, ZERO }

    public enum DestFactor { ONE, ONE_MINUS_SRC_ALPHA, ZERO, SRC_ALPHA }

    /** (what getShader() gave: something that is not null) */
    public static final Object SHADER = new Object();
    public static final Uniform MODEL_OFFSET = new Uniform();

    private static Shader shader = Shader.POSITION_COLOR;
    private static boolean blend;
    private static boolean additive;
    private static boolean depthTest = true;
    private static boolean depthWrite = true;
    private static boolean cull = true;
    private static Identifier texture;
    private static Object using;
    static final Vector3f modelOffset = new Vector3f();
    private static final Map<String, RenderPipeline> PIPELINES = new HashMap<>();

    private Gl() {
    }

    public static Object setShader(Shader s) {
        shader = s;
        return SHADER;
    }

    public static Object getShader() {
        return SHADER;
    }

    public static void enableBlend() {
        blend = true;
    }

    public static void disableBlend() {
        blend = false;
    }

    public static void defaultBlendFunc() {
        additive = false;
    }

    public static void blendFunc(SourceFactor src, DestFactor dst) {
        additive = dst == DestFactor.ONE;
    }

    public static void depthMask(boolean on) {
        depthWrite = on;
    }

    public static void enableDepthTest() {
        depthTest = true;
    }

    public static void disableDepthTest() {
        depthTest = false;
    }

    public static void enableCull() {
        cull = true;
    }

    public static void disableCull() {
        cull = false;
    }

    public static void setShaderTexture(int unit, Identifier id) {
        if (unit == 0) {
            texture = id;
        }
    }

    /** (1.21.1's type.setupRenderState / clearRenderState around a retained buffer's draw: the state set here is used) */
    public static void use(Object type) {
        using = type;
    }

    public static int _getTexLevelParameter(int target, int level, int name) {
        return GL11.glGetTexLevelParameteri(target, level, name);
    }

    /** Draws {@code mesh} as the state set says, and closes it. */
    public static void draw(MeshData mesh) {
        try {
            drawNow(pipelineFor(mesh), mesh);
        } finally {
            mesh.close();
        }
    }

    /** As {@link #draw}, with this model-view (the projection is the frame's). */
    public static void draw(MeshData mesh, Matrix4f modelView, Matrix4f projection) {
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.set(modelView);
        try {
            draw(mesh);
        } finally {
            stack.popMatrix();
        }
    }

    private static void drawNow(RenderPipeline pipeline, MeshData mesh) {
        int c = GuiTint.color();
        Vector4f colour = new Vector4f((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, (c >>> 24) / 255.0F);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), colour, modelOffset, new Matrix4f());
        GpuBuffer vertices = pipeline.getVertexFormat().uploadImmediateVertexBuffer(mesh.vertexBuffer());
        GpuBuffer indices;
        VertexFormat.IndexType indexType;
        if (mesh.indexBuffer() == null) {
            RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(mesh.drawState().mode());
            indices = sequential.getBuffer(mesh.drawState().indexCount());
            indexType = sequential.type();
        } else {
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(mesh.indexBuffer());
            indexType = mesh.drawState().indexType();
        }
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView colourView = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
        GpuTextureView depthView = target.useDepth
                ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView())
                : null;
        // (a texture loads itself the first time it is asked for, which is not allowed while a render pass is open)
        AbstractTexture bound = shader.textured && texture != null ? Minecraft.getInstance().getTextureManager().getTexture(texture) : null;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "Enhanced immediate draw", colourView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertices);
            if (bound != null) {
                pass.bindTexture("Sampler0", bound.getTextureView(), bound.getSampler());
            }
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
        }
    }

    private static RenderPipeline pipelineFor(MeshData mesh) {
        var state = mesh.drawState();
        String key = shader + "/" + blend + additive + depthTest + depthWrite + cull + "/" + state.format() + state.mode();
        return PIPELINES.computeIfAbsent(key, k -> {
            RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Enhanced.id("pipeline/immediate_" + PIPELINES.size()))
                    .withVertexShader(shader.program)
                    .withFragmentShader(shader.program)
                    .withCull(cull)
                    .withDepthWrite(depthWrite)
                    .withDepthTestFunction(depthTest ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(state.format(), state.mode());
            if (shader.textured) {
                b.withSampler("Sampler0");
            }
            if (blend) {
                b.withBlend(additive ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT);
            } else {
                b.withoutBlend();
            }
            return b.build();
        });
    }

    /** (the texture a draw would bind, made sure to be loaded) */
    static void touch(Identifier id) {
        Minecraft.getInstance().getTextureManager().getTexture(id);
    }
}
