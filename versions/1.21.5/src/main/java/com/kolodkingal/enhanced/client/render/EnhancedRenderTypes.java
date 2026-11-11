package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.function.Function;

/**
 * 1.21.5's render types for energy and Build Magic's ghost (the 1.21.1 file says what each is for). From 1.21.5 a
 * render type is a render pipeline (shader, blending, depth, culling, offset) and the few states that are left
 * (texture, lightmap): each pipeline here is the 1.21.1 type's states as one.
 */
public final class EnhancedRenderTypes {
    private static RenderPipeline.Builder lightning(String name) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                .withLocation(Enhanced.id("pipeline/" + name))
                .withVertexShader("core/rendertype_lightning")
                .withFragmentShader("core/rendertype_lightning")
                .withBlend(BlendFunction.LIGHTNING)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS);
    }

    private static RenderType.CompositeState plain() {
        return RenderType.CompositeState.builder().createCompositeState(false);
    }

    public static final RenderType ENERGY_BEAM = RenderType.create("enhanced_energy_beam", 4096, false, true,
            lightning("energy_beam").build(), plain());

    public static final RenderType ENERGY_BEAM_OVER = RenderType.create("enhanced_energy_beam_over", 4096, false, true,
            lightning("energy_beam_over").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build(), plain());

    public static final RenderType ENERGY_VEIL = RenderType.create("enhanced_energy_veil", 16384, false, true,
            RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
                    .withLocation(Enhanced.id("pipeline/energy_veil"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .build(), plain());

    public static final RenderType BUILD_XRAY = RenderType.create("enhanced_build_xray", 4096, false, true,
            lightning("build_xray").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build(), plain());

    public static final RenderType BUILD_GLOW = RenderType.create("enhanced_build_glow", 4096, false, true,
            lightning("build_glow").withDepthBias(-1.0F, -10.0F).build(), plain());

    private static RenderPipeline.Builder ghostPipeline(String name) {
        return RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
                .withLocation(Enhanced.id("pipeline/" + name))
                .withCull(false)
                .withDepthBias(-1.0F, -10.0F)
                .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS);
    }

    private static RenderType.CompositeState ghost(RenderStateShard.EmptyTextureStateShard texture) {
        return RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setTextureState(texture)
                .createCompositeState(false);
    }

    private static final RenderPipeline GHOST_TRANSLUCENT = ghostPipeline("build_ghost")
            .withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false).build();
    private static final RenderPipeline GHOST_ADDITIVE = ghostPipeline("build_ghost_glow")
            .withBlend(BlendFunction.LIGHTNING).withDepthWrite(false).build();

    public static final RenderType BUILD_GHOST = RenderType.create("enhanced_build_ghost", 4096, false, true,
            GHOST_TRANSLUCENT, ghost(RenderStateShard.BLOCK_SHEET_MIPPED));

    public static final RenderType BUILD_GHOST_DEPTH = RenderType.create("enhanced_build_ghost_depth", 4096, false, false,
            ghostPipeline("build_ghost_depth").withoutBlend().withColorWrite(false).withDepthWrite(true).build(),
            ghost(RenderStateShard.BLOCK_SHEET_MIPPED));

    private static final RenderStateShard.TextureStateShard WHITE = new RenderStateShard.TextureStateShard(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"), TriState.FALSE, false);

    public static final RenderType BUILD_GHOST_FLAT = RenderType.create("enhanced_build_ghost_flat", 4096, false, true,
            GHOST_TRANSLUCENT, ghost(WHITE));

    public static final RenderType BUILD_GHOST_GLOW = RenderType.create("enhanced_build_ghost_glow", 4096, false, true,
            GHOST_ADDITIVE, ghost(WHITE));

    private static RenderPipeline textured(String name, BlendFunction blend, boolean cull, boolean offset) {
        RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
                .withLocation(Enhanced.id("pipeline/" + name))
                .withVertexShader("core/position_tex_color")
                .withFragmentShader("core/position_tex_color")
                .withSampler("Sampler0")
                .withBlend(blend)
                .withDepthWrite(false)
                .withCull(cull)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS);
        if (offset) {
            b.withDepthBias(-1.0F, -10.0F);
        }
        return b.build();
    }

    private static final RenderPipeline FIELD = textured("energy_field", BlendFunction.LIGHTNING, false, false);
    private static final RenderPipeline DECAL = textured("web_decal", BlendFunction.TRANSLUCENT, false, true);
    private static final RenderPipeline GLOW_ONE_SIDE = textured("one_sided_glow", BlendFunction.LIGHTNING, true, false);
    private static final RenderPipeline SHADE_ONE_SIDE = textured("one_sided_shade", BlendFunction.TRANSLUCENT, true, false);

    private static RenderType.CompositeState texture(ResourceLocation texture, boolean blur) {
        return RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(texture, blur ? TriState.TRUE : TriState.FALSE, false))
                .createCompositeState(false);
    }

    private static final Function<ResourceLocation, RenderType> ENERGY_FIELD = Util.memoize(texture ->
            RenderType.create("enhanced_energy_field", 16384, false, true, FIELD, texture(texture, false)));
    private static final Function<ResourceLocation, RenderType> WEB_DECAL = Util.memoize(texture ->
            RenderType.create("enhanced_web_decal", 1536, false, true, DECAL, texture(texture, false)));
    private static final Function<ResourceLocation, RenderType> ONE_SIDED_GLOW = Util.memoize(texture ->
            RenderType.create("enhanced_one_sided_glow", 16384, false, true, GLOW_ONE_SIDE, texture(texture, true)));
    private static final Function<ResourceLocation, RenderType> ONE_SIDED_SHADE = Util.memoize(texture ->
            RenderType.create("enhanced_one_sided_shade", 4096, false, true, SHADE_ONE_SIDE, texture(texture, true)));

    public static RenderType energyField(ResourceLocation texture) {
        return ENERGY_FIELD.apply(texture);
    }

    public static RenderType webDecal(ResourceLocation texture) {
        return WEB_DECAL.apply(texture);
    }

    public static RenderType oneSidedGlow(ResourceLocation texture) {
        return ONE_SIDED_GLOW.apply(texture);
    }

    public static RenderType oneSidedShade(ResourceLocation texture) {
        return ONE_SIDED_SHADE.apply(texture);
    }

    private EnhancedRenderTypes() {
    }
}
