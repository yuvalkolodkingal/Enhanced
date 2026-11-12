package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.11 only: a block entity renderer of the mod written to draw from the block entity (1.21.4's
 * {@code render(be, partialTick, pose, buffers, light, overlay, cameraPos)}). From 1.21.9 a block entity renderer submits
 * from a render state: the state keeps the block entity, and {@link #submit} draws it into a {@link SubmitBuffers}.
 */
public interface LegacyBlockEntityRenderer<T extends BlockEntity> extends BlockEntityRenderer<T, LegacyBlockEntityRenderer.State<T>> {
    /** 1.21.4's render. */
    void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay, Vec3 cameraPos);

    /** The render state: the block entity itself, the partial tick and the camera's position. */
    final class State<T extends BlockEntity> extends BlockEntityRenderState {
        @Nullable
        public T blockEntity;
        public float partialTick;
        public Vec3 cameraPos = Vec3.ZERO;
    }

    @Override
    default State<T> createRenderState() {
        return new State<>();
    }

    @Override
    default void extractRenderState(T be, State<T> state, float partialTick, Vec3 cameraPos,
                                    ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.blockEntity = be;
        state.partialTick = partialTick;
        state.cameraPos = cameraPos;
    }

    @Override
    default void submit(State<T> state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.blockEntity == null) {
            return;
        }
        SubmitBuffers buffers = new SubmitBuffers(collector);
        try {
            render(state.blockEntity, state.partialTick, pose, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY, state.cameraPos);
        } finally {
            buffers.flush();
        }
    }
}
