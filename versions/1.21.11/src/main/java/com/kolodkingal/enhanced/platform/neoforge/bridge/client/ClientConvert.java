package com.kolodkingal.enhanced.platform.neoforge.bridge.client;
//#norenames

import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.platform.neoforge.bridge.Binding;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterEntities;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueBlocks;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterParticles;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterSky;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentBlocks;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTripwireBlocks;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterWeather;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.21.11: the level's stages are NeoForge sub-events (1.21.9). The mod's stages are 1.21.1's: the solid, cutout-mipped
 * and cutout blocks are one stage now (the three are posted in order there), and the block entities are drawn with the
 * entities (posted after them).
 */
final class ClientConvert {
    static final List<Class<? extends net.neoforged.neoforge.client.event.RenderLevelStageEvent>> STAGE_EVENTS = List.of(
            AfterSky.class, AfterOpaqueBlocks.class, AfterEntities.class, AfterTranslucentBlocks.class,
            AfterTripwireBlocks.class, AfterParticles.class, AfterWeather.class, AfterLevel.class);

    private ClientConvert() {
    }

    private static List<RenderLevelStageEvent.Stage> stages(net.neoforged.neoforge.client.event.RenderLevelStageEvent n) {
        if (n instanceof AfterSky) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_SKY);
        } else if (n instanceof AfterOpaqueBlocks) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS, RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS,
                    RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS);
        } else if (n instanceof AfterEntities) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_ENTITIES, RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES);
        } else if (n instanceof AfterTranslucentBlocks) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS);
        } else if (n instanceof AfterTripwireBlocks) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS);
        } else if (n instanceof AfterParticles) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_PARTICLES);
        } else if (n instanceof AfterWeather) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_WEATHER);
        } else if (n instanceof AfterLevel) {
            return List.of(RenderLevelStageEvent.Stage.AFTER_LEVEL);
        }
        return List.of();
    }

    /** The mod's events for a NeoForge stage event. */
    static Object stageEvents(net.neoforged.neoforge.client.event.RenderLevelStageEvent n) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Matrix4f projection = mc.gameRenderer.getProjectionMatrix(mc.options.fov().get().floatValue());
        Matrix4f modelView = new Matrix4f(n.getModelViewMatrix());
        Frustum frustum = new Frustum(modelView, projection);
        frustum.prepare(camera.position().x, camera.position().y, camera.position().z);
        List<RenderLevelStageEvent> out = new ArrayList<>();
        for (RenderLevelStageEvent.Stage stage : stages(n)) {
            out.add(new RenderLevelStageEvent(stage, n.getLevelRenderer(), n.getPoseStack(), modelView, projection,
                    n.getLevelRenderer().getTicks(), mc.getDeltaTracker(), camera, frustum));
        }
        return new Binding.Many(out);
    }
}
