package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.function.Function;

/** 1.21.5's render types of the sand (the 1.21.1 file says what they are): see-through, double-sided, no depth. */
public final class SandRenderTypes {
    public static final RenderType FLAT = RenderType.create("enhanced_sand_flat", 8192, false, true,
            RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
                    .withLocation(Enhanced.id("pipeline/sand_flat"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .build(), RenderType.CompositeState.builder().createCompositeState(false));

    private static final RenderPipeline TEXTURED_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
            .withLocation(Enhanced.id("pipeline/sand_textured"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private static final Function<ResourceLocation, RenderType> TEXTURED = Util.memoize(texture ->
            RenderType.create("enhanced_sand_textured", 16384, false, true, TEXTURED_PIPELINE,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, TriState.FALSE, false))
                            .createCompositeState(false)));

    public static RenderType textured(ResourceLocation texture) {
        return TEXTURED.apply(texture);
    }

    private SandRenderTypes() {
    }
}
