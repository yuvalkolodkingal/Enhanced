package com.kolodkingal.enhanced.compat;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.21.5: the GL state calls of 1.21.1's immediate drawing (a shader, blending, the depth test and mask, culling, the
 * bound texture) are gone; a draw is a render type, which is a render pipeline. The mod's few places that still draw
 * the old way (the skies of Asgard and Eternity, Build Magic's ghost and glow, Sokovia's mesh) tell this class the
 * state as before, and {@link #draw} draws the mesh in a render type made for that state (made once, kept).
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
    private static ResourceLocation texture;
    private static RenderType using;
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

    public static void setShaderTexture(int unit, ResourceLocation id) {
        if (unit == 0) {
            texture = id;
        }
    }

    /** (1.21.1's type.setupRenderState / clearRenderState around a retained buffer's draw) */
    public static void use(RenderType type) {
        using = type;
    }

    public static int _getTexLevelParameter(int target, int level, int name) {
        return GL11.glGetTexLevelParameteri(target, level, name);
    }

    /** Draws {@code mesh} as the state set says (or in the render type set with {@link #use}), and closes it. */
    public static void draw(MeshData mesh) {
        RenderType type = using != null ? using : typeFor(mesh);
        type.draw(mesh);
    }

    /** As {@link #draw}, with this model-view and projection instead of the current ones. */
    public static void draw(MeshData mesh, Matrix4f modelView, Matrix4f projection) {
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.set(modelView);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projection, ProjectionType.PERSPECTIVE);
        try {
            draw(mesh);
        } finally {
            RenderSystem.restoreProjectionMatrix();
            stack.popMatrix();
        }
    }

    private static RenderType typeFor(MeshData mesh) {
        var state = mesh.drawState();
        String key = shader + "/" + blend + additive + depthTest + depthWrite + cull + "/" + state.format() + state.mode()
                + "/" + (shader.textured ? texture : "");
        return TYPES.computeIfAbsent(key, k -> {
            RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                    .withLocation(Enhanced.id("pipeline/immediate_" + TYPES.size()))
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
            RenderType.CompositeState.CompositeStateBuilder composite = RenderType.CompositeState.builder();
            if (shader.textured && texture != null) {
                composite.setTextureState(new RenderStateShard.TextureStateShard(texture, TriState.FALSE, false));
            }
            return RenderType.create("enhanced_immediate", 1536, false, false, b.build(), composite.createCompositeState(false));
        });
    }

    /** (the texture a draw would bind, made sure to be loaded) */
    static void touch(ResourceLocation id) {
        Minecraft.getInstance().getTextureManager().getTexture(id);
    }
}
