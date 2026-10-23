package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** The outline of what is looked at (NeoForge's RenderHighlightEvent). */
public abstract class RenderHighlightEvent implements Cancellable {
    private final LevelRenderer levelRenderer;
    private final Camera camera;
    private final HitResult target;
    private final DeltaTracker deltaTracker;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private boolean canceled;

    protected RenderHighlightEvent(
            LevelRenderer levelRenderer, Camera camera, HitResult target, DeltaTracker deltaTracker,
            PoseStack poseStack, MultiBufferSource multiBufferSource) {
        this.levelRenderer = levelRenderer;
        this.camera = camera;
        this.target = target;
        this.deltaTracker = deltaTracker;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
    }

    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    public Camera getCamera() {
        return camera;
    }

    public HitResult getTarget() {
        return target;
    }

    public DeltaTracker getDeltaTracker() {
        return deltaTracker;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getMultiBufferSource() {
        return multiBufferSource;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /** The outline of the block looked at is about to be drawn; cancelling hides it. */
    public static class Block extends RenderHighlightEvent {
        public Block(
                LevelRenderer levelRenderer, Camera camera, HitResult target, DeltaTracker deltaTracker,
                PoseStack poseStack, MultiBufferSource multiBufferSource) {
            super(levelRenderer, camera, target, deltaTracker, poseStack, multiBufferSource);
        }

        @Override
        public BlockHitResult getTarget() {
            return (BlockHitResult) super.getTarget();
        }
    }
}
