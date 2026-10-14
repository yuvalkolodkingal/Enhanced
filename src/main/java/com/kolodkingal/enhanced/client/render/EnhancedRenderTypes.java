package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Glowing, additive render types for energy: beams, shields, the Hex wall and Build Magic's box. */
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

    /** Untextured, see-through (normal alpha blending), double-sided tint: the red veil of the Hex wall and Chaos Shield. */
    public static final RenderType ENERGY_VEIL = RenderType.create("enhanced_energy_veil",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 16384, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /**
     * Untextured, additive, double-sided, and drawn over everything (no depth test): Build Magic's box seen faintly
     * through hills and walls, so a box is never lost behind one (D-B 6.1, client option {@code seeThrough}).
     */
    public static final RenderType BUILD_XRAY = RenderType.create("enhanced_build_xray",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /**
     * Untextured, additive, double-sided, and pushed a little towards the camera so it never fights the block faces it
     * lies on: the glow on the faces of a thing Build Magic has selected (D-B 3.4), drawn from a buffer of its own.
     */
    public static final RenderType BUILD_GLOW = RenderType.create("enhanced_build_glow",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
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

    private static final Function<ResourceLocation, RenderType> WEB_DECAL = Util.memoize(texture ->
            RenderType.create("enhanced_web_decal",
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(POSITION_TEX_COLOR)
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                            .createCompositeState(false)));

    /**
     * Textured, see-through (normal alpha blending, so a white web shows on a bright day), double-sided, pushed a little
     * towards the camera so it does not fight with the block it lies on: Spider-Man's web splats.
     */
    public static RenderType webDecal(ResourceLocation texture) {
        return WEB_DECAL.apply(texture);
    }

    private EnhancedRenderTypes() {
    }
}
