package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * 1.21.11 only: a layer of the mod written to draw (1.21.4's {@code render(pose, buffers, light, state, yRot, xRot)}).
 * From 1.21.9 a layer submits: {@link #submit} gives the layer's render a {@link SubmitBuffers} and submits what it drew.
 */
public abstract class LegacyRenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    protected LegacyRenderLayer(RenderLayerParent<S, M> parent) {
        super(parent);
    }

    /** 1.21.4's render of the layer. */
    public abstract void render(PoseStack poseStack, MultiBufferSource buffers, int light, S state, float netHeadYaw, float headPitch);

    @Override
    public final void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float netHeadYaw, float headPitch) {
        SubmitBuffers buffers = new SubmitBuffers(collector);
        buffers.outline(state.outlineColor);
        try {
            render(poseStack, buffers, light, state, netHeadYaw, headPitch);
        } finally {
            buffers.flush();
        }
    }
}
