package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Render types for energy (beams, shields, the Hex wall, Build Magic's box) and for Build Magic's ghost. */
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

    /**
     * Real block models seen through (D-B 6.2): the block atlas, normal alpha blending, hidden by the world in front of
     * it but never hiding what is behind it (no depth write), double-sided, and pushed a little towards the camera so
     * the ghost of a block that replaces another wins over that block's faces. Build Magic's ghost, drawn from buffers
     * of its own with the terrain's translucent shader (its chunk offset puts the ghost where it stands).
     */
    public static final RenderType BUILD_GHOST = RenderType.create("enhanced_build_ghost",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 4096, false, true, ghost(RenderStateShard.BLOCK_SHEET_MIPPED,
                    RenderStateShard.TRANSLUCENT_TRANSPARENCY));

    /** The game's plain white texture: a ghost's blocks drawn with it keep their shape and shade, not their look. */
    private static final RenderStateShard.TextureStateShard WHITE = new RenderStateShard.TextureStateShard(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"), false, false);

    /** As {@link #BUILD_GHOST}, but flat white (the shader colour greys it): the ghost of a spell that is refused. */
    public static final RenderType BUILD_GHOST_FLAT = RenderType.create("enhanced_build_ghost_flat",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 4096, false, true, ghost(WHITE, RenderStateShard.TRANSLUCENT_TRANSPARENCY));

    /** As {@link #BUILD_GHOST}, but flat white and additive: the hero's glow laid over a ghost's blocks. */
    public static final RenderType BUILD_GHOST_GLOW = RenderType.create("enhanced_build_ghost_glow",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 4096, false, true, ghost(WHITE, RenderStateShard.LIGHTNING_TRANSPARENCY));

    private static RenderType.CompositeState ghost(RenderStateShard.EmptyTextureStateShard texture,
                                                   RenderStateShard.TransparencyStateShard transparency) {
        return RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setShaderState(RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER)
                .setTextureState(texture)
                .setTransparencyState(transparency)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setCullState(RenderStateShard.NO_CULL)
                .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                .createCompositeState(false);
    }

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
