package com.kolodkingal.enhanced.platform.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

/** A stage of the level's drawing: the place for the mod's world effects. */
public class RenderLevelStageEvent {
    private final Stage stage;
    private final LevelRenderer levelRenderer;
    private final PoseStack poseStack;
    private final Matrix4f modelViewMatrix;
    private final Matrix4f projectionMatrix;
    private final int renderTick;
    private final DeltaTracker partialTick;
    private final Camera camera;
    private final Frustum frustum;

    public RenderLevelStageEvent(
            Stage stage, LevelRenderer levelRenderer, PoseStack poseStack, Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix, int renderTick, DeltaTracker partialTick, Camera camera, Frustum frustum) {
        this.stage = stage;
        this.levelRenderer = levelRenderer;
        this.poseStack = poseStack;
        this.modelViewMatrix = modelViewMatrix;
        this.projectionMatrix = projectionMatrix;
        this.renderTick = renderTick;
        this.partialTick = partialTick;
        this.camera = camera;
        this.frustum = frustum;
    }

    public Stage getStage() {
        return stage;
    }

    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public int getRenderTick() {
        return renderTick;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    public Camera getCamera() {
        return camera;
    }

    public Frustum getFrustum() {
        return frustum;
    }

    /** The points of the level's drawing where the event fires, in order (NeoForge's RenderLevelStageEvent.Stage). */
    public enum Stage {
        AFTER_SKY,
        AFTER_SOLID_BLOCKS,
        AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS,
        AFTER_CUTOUT_BLOCKS,
        AFTER_ENTITIES,
        AFTER_BLOCK_ENTITIES,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_TRIPWIRE_BLOCKS,
        AFTER_PARTICLES,
        AFTER_WEATHER,
        AFTER_LEVEL
    }
}
