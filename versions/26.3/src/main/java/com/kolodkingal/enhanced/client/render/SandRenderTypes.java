package com.kolodkingal.enhanced.client.render;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.compat.Pipe;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

/** 1.21.11's and 26.3's render types of the sand (the 1.21.1 file says what they are): see-through, double-sided, no depth. */
public final class SandRenderTypes {
    public static final RenderType FLAT = RenderType.create("enhanced_sand_flat", RenderSetup.builder(
            Pipe.builder(Pipe.MATRICES_PROJECTION)
                    .withLocation(Enhanced.id("pipeline/sand_flat"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS)
                    .build()).sortOnUpload().createRenderSetup());

    private static final RenderPipeline TEXTURED_PIPELINE = Pipe.builder(Pipe.MATRICES_PROJECTION)
            .withLocation(Enhanced.id("pipeline/sand_textured"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
            .build();

    private static final Function<Identifier, RenderType> TEXTURED = Util.memoize(texture ->
            RenderType.create("enhanced_sand_textured", RenderSetup.builder(TEXTURED_PIPELINE)
                    .withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

    public static RenderType textured(Identifier texture) {
        return TEXTURED.apply(texture);
    }

    private SandRenderTypes() {
    }
}
