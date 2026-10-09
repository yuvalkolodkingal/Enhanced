package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Glowing, additive render types for energy: beams, shields and the Hex wall. */
public final class EnhancedRenderTypes {

    /** Untextured, additive (light adds up), double-sided, doesn't hide what's behind it. */
    public static final RenderType ENERGY_BEAM = RenderType.create("enhanced_energy_beam",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final RenderStateShard.ShaderStateShard POSITION_TEX_COLOR =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);

    private static final Function<ResourceLocation, RenderType> ENERGY_FIELD = Util.memoize(texture ->
            RenderType.create("enhanced_energy_field",
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 16384, false, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(POSITION_TEX_COLOR)
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                            .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .setCullState(RenderStateShard.NO_CULL)
                            .createCompositeState(false)));

    /** Textured, additive, double-sided energy surface (Hex wall, Chaos Shield). */
    public static RenderType energyField(ResourceLocation texture) {
        return ENERGY_FIELD.apply(texture);
    }

    private EnhancedRenderTypes() {
    }
}
