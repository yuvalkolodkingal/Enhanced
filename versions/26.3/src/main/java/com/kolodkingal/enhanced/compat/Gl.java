package com.kolodkingal.enhanced.compat;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * 26.3 (after 1.21.11's): the GL state of 1.21.1's immediate drawing (a shader, blending, the depth test and mask,
 * culling, the bound texture, the shader colour) as the mod's few places that still draw that way (the skies of Asgard
 * and Eternity, Build Magic's ghost and glow, Sokovia's mesh) tell it, and {@link #draw}:
 * <ul>
 *   <li>in the sky (the dimension's custom sky, {@link #sky}), the mesh is drawn at once with a pipeline made for that
 *   state (made once, kept), the shader colour ({@link GuiTint}) as the colour modulator;</li>
 *   <li>in a level stage (26.3 draws the level from what was submitted), the mesh's vertices go into the stage's
 *   buffers ({@link SubmitBuffers#stage}) in the render type it was told ({@link #use}) or one made for the state,
 *   tinted by the shader colour, moved from the model-view it was given into the submitted geometry's space;</li>
 *   <li>anywhere else it is not drawn.</li>
 * </ul>
 */
public final class Gl {
    /** The shaders the mod drew with, by their 1.21.1 names. */
    public enum Shader {
        POSITION("core/position_color", false), POSITION_COLOR("core/position_color", false),
        POSITION_TEX("core/position_tex_color", true), POSITION_TEX_COLOR("core/position_tex_color", true);

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
    private static boolean inSky;
    static final Vector3f modelOffset = new Vector3f();
    private static final Map<String, RenderPipeline> PIPELINES = new HashMap<>();
    private static final Map<String, RenderType> TYPES = new HashMap<>();

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

    /** (1.21.1's type.setupRenderState / clearRenderState around a retained buffer's draw: drawn in that type) */
    public static void use(Object type) {
        using = type;
    }

    /** (the texture's size was read from GL; 26.3's device is not GL's) */
    public static int _getTexLevelParameter(int target, int level, int name) {
        return 0;
    }

    /** Whether the custom sky is being drawn (the meshes drawn now are drawn at once). */
    public static void sky(boolean on) {
        inSky = on;
    }

    /** Draws {@code mesh} as the state set says, and closes it. */
    public static void draw(MeshData mesh) {
        draw(mesh, null, null);
    }

    /** As {@link #draw}, with this model-view (null: the current one; the projection is the frame's). */
    public static void draw(MeshData mesh, Matrix4f modelView, Matrix4f projection) {
        try {
            if (inSky) {
                drawNow(mesh.vertexBuffer(), mesh.drawState().format(), modelView);
            } else {
                drawInStage(mesh.vertexBuffer(), mesh.drawState().format(), modelView, 0, 0, 0);
            }
        } finally {
            mesh.close();
        }
    }

    /**
     * The vertices of a mesh into the level stage's buffers: in the render type {@link #use} named or one for the
     * state, moved from {@code modelView} (null: the stage's own space) by ({@code x}, {@code y}, {@code z}) first.
     */
    static void drawInStage(java.nio.ByteBuffer vertices, VertexFormat format, Matrix4f modelView, float x, float y, float z) {
        SubmitBuffers stage = SubmitBuffers.stage();
        RenderType type = using instanceof RenderType t ? t : typeFor();
        Matrix4f transform = new Matrix4f();
        if (modelView != null) {
            // (the stage's geometry is in the camera's frame before its turn: the turn the model-view holds is undone)
            Matrix4f view = new Matrix4f(Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState
                    .cameraRenderState.viewRotationMatrix);
            transform.set(view.invert()).mul(modelView);
        }
        transform.translate(x + modelOffset.x, y + modelOffset.y, z + modelOffset.z);
        MeshReader.replay(vertices, format, transform, GuiTint.color(), stage.getBuffer(type));
    }

    private static String key() {
        return shader.textured + "/" + blend + additive + depthTest + depthWrite + cull;
    }

    private static RenderPipeline pipeline() {
        return PIPELINES.computeIfAbsent(key(), k -> {
            Pipe b = Pipe.builder(Pipe.MATRICES_PROJECTION)
                    .withLocation(Enhanced.id("pipeline/immediate_" + PIPELINES.size()))
                    .withVertexShader(shader.program)
                    .withFragmentShader(shader.program)
                    .withCull(cull)
                    .withDepthWrite(depthWrite)
                    .withDepthTest(depthTest)
                    .withVertexFormat(shader.textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR,
                            PrimitiveTopology.QUADS);
            if (shader.textured) {
                b.withSampler("Sampler0");
            }
            if (blend) {
                b.withBlend(additive ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT);
            }
            return b.build();
        });
    }

    private static RenderType typeFor() {
        Identifier tex = shader.textured && texture != null ? texture : null;
        return TYPES.computeIfAbsent(key() + "/" + tex, k -> {
            RenderSetup.RenderSetupBuilder b = RenderSetup.builder(pipeline()).bufferSize(4096);
            if (tex != null) {
                b.withTexture("Sampler0", tex);
            }
            if (blend) {
                b.sortOnUpload();
            }
            return RenderType.create("enhanced_immediate", b.createRenderSetup());
        });
    }

    /** Whether meshes are drawn at once now (in the custom sky). */
    static boolean inSky() {
        return inSky;
    }

    /** The sky's drawing: the vertices at once, onto the main target (null model-view: the current one). */
    static void drawNow(java.nio.ByteBuffer vertices, VertexFormat from, Matrix4f modelViewOrNull) {
        Matrix4f modelView = modelViewOrNull != null ? new Matrix4f(modelViewOrNull) : RenderSystem.getModelViewMatrixCopy();
        RenderPipeline pipeline = pipeline();
        VertexFormat format = shader.textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR;
        int vertexCount = from.getVertexSize() == 0 ? 0 : vertices.remaining() / from.getVertexSize();
        if (vertexCount == 0) {
            return;
        }
        // (the vertices again in the pipeline's format, tinted: 26.3 has no shader colour of 1.21.1's kind to set)
        try (ByteBufferBuilder memory = new ByteBufferBuilder(Math.max(256, vertexCount * format.getVertexSize()))) {
            BufferBuilder builder = new BufferBuilder(memory, PrimitiveTopology.QUADS, format);
            MeshReader.replay(vertices, from, new Matrix4f().translation(modelOffset),
                    GuiTint.color(), builder);
            MeshData copy = builder.build();
            if (copy == null) {
                return;
            }
            try (copy) {
                AbstractTexture bound = shader.textured && texture != null
                        ? Minecraft.getInstance().getTextureManager().getTexture(texture) : null;
                GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(modelView, new Vector4f(1, 1, 1, 1));
                int indexCount = copy.drawState().indexCount();
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
                GpuBuffer indices = sequential.getBuffer(indexCount);
                try (GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> "Enhanced immediate vertices",
                        GpuBuffer.USAGE_VERTEX, copy.vertexBuffer())) {
                    var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
                    GpuTextureView colour = target.getColorTextureView();
                    GpuTextureView depth = target.getDepthTextureView();
                    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                            .createRenderPass(() -> "Enhanced immediate draw", colour, Optional.empty(), depth, OptionalDouble.empty())) {
                        pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
                        RenderSystem.bindDefaultUniforms(pass);
                        pass.setUniform("DynamicTransforms", transforms);
                        if (bound != null) {
                            pass.setUniform("Sampler0", bound.getTextureView(), bound.getSampler());
                        }
                        pass.setVertexBuffer(0, vertices.slice());
                        pass.setIndexBuffer(indices, sequential.type());
                        pass.drawIndexed(indexCount, 1, 0, 0, 0);
                    }
                }
            }
        }
    }

    /** (the texture a draw would bind, made sure to be loaded) */
    static void touch(Identifier id) {
        Minecraft.getInstance().getTextureManager().getTexture(id);
    }
}
