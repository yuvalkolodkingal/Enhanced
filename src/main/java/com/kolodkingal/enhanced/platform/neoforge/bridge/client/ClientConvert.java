package com.kolodkingal.enhanced.platform.neoforge.bridge.client;

import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/** NeoForge's client values that are not enums, to the common enums ({@link ClientBindings}). */
final class ClientConvert {
    private static final Map<net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage,
            RenderLevelStageEvent.Stage> STAGES = new IdentityHashMap<>();

    static {
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY,
                RenderLevelStageEvent.Stage.AFTER_SKY);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS,
                RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS,
                RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS,
                RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES,
                RenderLevelStageEvent.Stage.AFTER_ENTITIES);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,
                RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS,
                RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS,
                RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES,
                RenderLevelStageEvent.Stage.AFTER_PARTICLES);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_WEATHER,
                RenderLevelStageEvent.Stage.AFTER_WEATHER);
        stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL,
                RenderLevelStageEvent.Stage.AFTER_LEVEL);
    }

    private ClientConvert() {
    }

    private static void stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage neo,
                              RenderLevelStageEvent.Stage common) {
        STAGES.put(neo, common);
    }

    /** The common stage of a NeoForge stage (another mod's own stage has none: the listeners never see it). */
    static RenderLevelStageEvent.Stage stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage neo) {
        return STAGES.get(neo);
    }
}
