package com.kolodkingal.enhanced.platform.neoforge.bridge.client;
//#norenames

import com.kolodkingal.enhanced.compat.SubmitBuffers;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.platform.neoforge.bridge.Binding;
import com.kolodkingal.enhanced.platform.neoforge.bridge.NeoForgeBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.3: the level is extracted and then drawn from what was submitted; the mod's level stages draw their geometry,
 * so all of them (1.21.1's stages from the blocks to the weather, in order) are posted while NeoForge's
 * {@code SubmitCustomGeometryEvent} is fired, into the stage's buffers ({@code compat.SubmitBuffers#stage}), which
 * are submitted once they have all run. The sky is drawn by the dimension's custom sky
 * (compat.DimensionSpecialEffects), so nothing listens to 1.21.1's sky stage.
 */
final class ClientConvert {
    private static final List<RenderLevelStageEvent.Stage> STAGES = List.of(
            RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS, RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS,
            RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS, RenderLevelStageEvent.Stage.AFTER_ENTITIES,
            RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS,
            RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS, RenderLevelStageEvent.Stage.AFTER_PARTICLES,
            RenderLevelStageEvent.Stage.AFTER_WEATHER, RenderLevelStageEvent.Stage.AFTER_LEVEL);

    private ClientConvert() {
    }

    /** The mod's events for NeoForge's custom geometry event. */
    static Object stageEvents(SubmitCustomGeometryEvent n) {
        Minecraft mc = Minecraft.getInstance();
        CameraRenderState camera = n.getLevelRenderState().cameraRenderState;
        Matrix4f modelView = new Matrix4f(camera.viewRotationMatrix);
        Matrix4f projection = new Matrix4f(camera.projectionMatrix);
        SubmitBuffers.openStage(n.getSubmitNodeCollector());
        NeoForgeBridge.afterPost(() -> SubmitBuffers.openStage(null));
        List<RenderLevelStageEvent> out = new ArrayList<>();
        int ticks = mc.level == null ? 0 : (int) mc.level.getGameTime();
        for (RenderLevelStageEvent.Stage stage : STAGES) {
            out.add(new RenderLevelStageEvent(stage, mc.levelRenderer, n.getPoseStack(), modelView, projection, ticks,
                    mc.getDeltaTracker(), mc.gameRenderer.mainCamera(), camera.cullFrustum));
        }
        return new Binding.Many(out);
    }
}
