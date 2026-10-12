package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * The render types of the sand: sand is not light, so where the shared energy types add their colour (and a bright sky
 * washes them out), these blend it in as an ordinary see-through surface. Double-sided, full-bright, drawing no depth (so
 * two sheets of sand do not hide each other).
 */
public final class SandRenderTypes {
    private static final RenderStateShard.ShaderStateShard POSITION_TEX_COLOR =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);

    /** An untextured see-through surface: the ridges and the flat decals on the ground (drawn through the same buffer). */
    public static final RenderType FLAT = RenderType.create("enhanced_sand_flat",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 8192, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> TEXTURED = Util.memoize(texture ->
            RenderType.create("enhanced_sand_textured",
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 16384, false, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(POSITION_TEX_COLOR)
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .setCullState(RenderStateShard.NO_CULL)
                            .createCompositeState(false)));

    /** A see-through textured surface (the funnel of a tornado, the sand wall). */
    public static RenderType textured(ResourceLocation texture) {
        return TEXTURED.apply(texture);
    }

    private SandRenderTypes() {
    }
}
