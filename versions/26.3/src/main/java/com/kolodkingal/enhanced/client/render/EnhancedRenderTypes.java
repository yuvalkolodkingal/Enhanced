package com.kolodkingal.enhanced.client.render;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.compat.Pipe;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 1.21.11's and 26.3's render types for energy and Build Magic's ghost (the 1.21.1 file says what each is for). As from 1.21.5 a
 * render type is a render pipeline (shader, blending, depth, culling, offset); from 1.21.11 the rest (textures, the
 * lightmap, sorting, the buffer's size) is its render setup.
 */
public final class EnhancedRenderTypes {
    private static final Supplier<GpuSampler> BLUR = () -> RenderSystem.getSamplerCache()
            .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);

    private static Pipe lightning(String name) {
        return Pipe.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withLocation(Enhanced.id("pipeline/" + name))
                .withVertexShader("core/rendertype_lightning")
                .withFragmentShader("core/rendertype_lightning")
                .withBlend(BlendFunction.LIGHTNING)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS);
    }

    private static RenderType type(String name, RenderPipeline pipeline, int size) {
        return RenderType.create(name, RenderSetup.builder(pipeline).sortOnUpload().bufferSize(size).createRenderSetup());
    }

    public static final RenderType ENERGY_BEAM = type("enhanced_energy_beam", lightning("energy_beam").build(), 4096);

    public static final RenderType ENERGY_BEAM_OVER = type("enhanced_energy_beam_over",
            lightning("energy_beam_over").withDepthTest(false).build(), 4096);

    public static final RenderType ENERGY_VEIL = type("enhanced_energy_veil",
            Pipe.builder(Pipe.MATRICES_PROJECTION)
                    .withLocation(Enhanced.id("pipeline/energy_veil"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS)
                    .build(), 16384);

    public static final RenderType BUILD_XRAY = type("enhanced_build_xray",
            lightning("build_xray").withDepthTest(false).build(), 4096);

    public static final RenderType BUILD_GLOW = type("enhanced_build_glow",
            lightning("build_glow").withDepthBias(-1.0F, -10.0F).build(), 4096);

    private static Pipe ghostPipeline(String name) {
        return Pipe.builder(RenderPipelines.BLOCK_SNIPPET)
                .withLocation(Enhanced.id("pipeline/" + name))
                .withCull(false)
                .withDepthBias(-1.0F, -10.0F);
    }

    private static RenderType ghost(String name, RenderPipeline pipeline, Identifier texture, boolean sort) {
        RenderSetup.RenderSetupBuilder b = RenderSetup.builder(pipeline).useLightmap().withTexture("Sampler0", texture)
                .bufferSize(4096);
        if (sort) {
            b.sortOnUpload();
        }
        return RenderType.create(name, b.createRenderSetup());
    }

    private static final RenderPipeline GHOST_TRANSLUCENT = ghostPipeline("build_ghost")
            .withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false).build();
    private static final RenderPipeline GHOST_ADDITIVE = ghostPipeline("build_ghost_glow")
            .withBlend(BlendFunction.LIGHTNING).withDepthWrite(false).build();

    public static final RenderType BUILD_GHOST = ghost("enhanced_build_ghost", GHOST_TRANSLUCENT,
            TextureAtlas.LOCATION_BLOCKS, true);

    public static final RenderType BUILD_GHOST_DEPTH = ghost("enhanced_build_ghost_depth",
            ghostPipeline("build_ghost_depth").withoutBlend().withColorWrite(false).withDepthWrite(true).build(),
            TextureAtlas.LOCATION_BLOCKS, false);

    private static final Identifier WHITE = Identifier.withDefaultNamespace("textures/misc/white.png");

    public static final RenderType BUILD_GHOST_FLAT = ghost("enhanced_build_ghost_flat", GHOST_TRANSLUCENT, WHITE, true);

    public static final RenderType BUILD_GHOST_GLOW = ghost("enhanced_build_ghost_glow", GHOST_ADDITIVE, WHITE, true);

    private static RenderPipeline textured(String name, BlendFunction blend, boolean cull, boolean offset) {
        Pipe b = Pipe.builder(Pipe.MATRICES_PROJECTION)
                .withLocation(Enhanced.id("pipeline/" + name))
                .withVertexShader("core/position_tex_color")
                .withFragmentShader("core/position_tex_color")
                .withSampler("Sampler0")
                .withBlend(blend)
                .withDepthWrite(false)
                .withCull(cull)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS);
        if (offset) {
            b.withDepthBias(-1.0F, -10.0F);
        }
        return b.build();
    }

    private static final RenderPipeline FIELD = textured("energy_field", BlendFunction.LIGHTNING, false, false);
    private static final RenderPipeline DECAL = textured("web_decal", BlendFunction.TRANSLUCENT, false, true);
    private static final RenderPipeline GLOW_ONE_SIDE = textured("one_sided_glow", BlendFunction.LIGHTNING, true, false);
    private static final RenderPipeline SHADE_ONE_SIDE = textured("one_sided_shade", BlendFunction.TRANSLUCENT, true, false);

    private static RenderType textured(String name, RenderPipeline pipeline, Identifier texture, boolean blur, int size) {
        return RenderType.create(name, RenderSetup.builder(pipeline)
                .withTexture("Sampler0", texture, blur ? BLUR : null)
                .sortOnUpload()
                .bufferSize(size)
                .createRenderSetup());
    }

    private static final Function<Identifier, RenderType> ENERGY_FIELD = Util.memoize(texture ->
            textured("enhanced_energy_field", FIELD, texture, false, 16384));
    private static final Function<Identifier, RenderType> WEB_DECAL = Util.memoize(texture ->
            textured("enhanced_web_decal", DECAL, texture, false, 1536));
    private static final Function<Identifier, RenderType> ONE_SIDED_GLOW = Util.memoize(texture ->
            textured("enhanced_one_sided_glow", GLOW_ONE_SIDE, texture, true, 16384));
    private static final Function<Identifier, RenderType> ONE_SIDED_SHADE = Util.memoize(texture ->
            textured("enhanced_one_sided_shade", SHADE_ONE_SIDE, texture, true, 4096));

    public static RenderType energyField(Identifier texture) {
        return ENERGY_FIELD.apply(texture);
    }

    public static RenderType webDecal(Identifier texture) {
        return WEB_DECAL.apply(texture);
    }

    public static RenderType oneSidedGlow(Identifier texture) {
        return ONE_SIDED_GLOW.apply(texture);
    }

    public static RenderType oneSidedShade(Identifier texture) {
        return ONE_SIDED_SHADE.apply(texture);
    }

    private EnhancedRenderTypes() {
    }
}
