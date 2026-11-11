package com.kolodkingal.enhanced.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * 1.21.4 only: a model of the mod that is not an entity's (a sword, a helmet, wings) and draws its parts itself. From
 * 1.21.2 a {@code Model} draws its whole root and nothing else ({@code renderToBuffer} is final), so these models are
 * not {@code Model}s on 1.21.4: they keep the two calls their layers make, {@link #renderType} and
 * {@link #renderToBuffer}, as in 1.21.1.
 */
public abstract class LegacyPlainModel {
    private final Function<ResourceLocation, RenderType> renderType;

    protected LegacyPlainModel(Function<ResourceLocation, RenderType> renderType) {
        this.renderType = renderType;
    }

    public final RenderType renderType(ResourceLocation texture) {
        return renderType.apply(texture);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        legacyRenderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** A model whose 1.21.1 renderToBuffer drew its root (the build renames that method to this). */
    public void legacyRenderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                                     int color) {
    }

    public final void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        renderToBuffer(poseStack, buffer, packedLight, packedOverlay, -1);
    }
}
