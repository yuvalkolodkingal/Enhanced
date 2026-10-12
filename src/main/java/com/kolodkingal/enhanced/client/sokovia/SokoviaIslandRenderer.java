package com.kolodkingal.enhanced.client.sokovia;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the picture of the island ({@link SokoviaMesh}) while it rises or comes down. The game fires the stages after
 * each pass of the terrain from inside that pass, with the render state of the pass still set, so the island is drawn
 * with the terrain's own shader, textures and fog, one stage for each kind of geometry it has: solid blocks, cut-out
 * blocks that are mipped (leaves) and cut-out blocks (grass, flowers). The picture is moved by the shader's chunk
 * offset, so a frame costs three draws and no work on the CPU.
 *
 * <p>Translucent blocks (glass, ice) are not part of the picture: the island's blocks are the ground, and the ground is
 * opaque. They are real the moment the server places the island.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SokoviaIslandRenderer {

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        RenderType type;
        if (stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            type = RenderType.solid();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) {
            type = RenderType.cutoutMipped();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            type = RenderType.cutout();
        } else {
            return;
        }
        SokoviaClient.Island island = SokoviaClient.current();
        if (island == null || island.mesh == null || !island.mesh.isReady() || island.phase == SokoviaClient.HELD) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double offset = SokoviaClient.offset(island, partial);
        if (!visible(event, island, offset)) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        island.mesh.draw(type, event.getModelViewMatrix(), event.getProjectionMatrix(), camera, offset);
    }

    /** Whether any of the island is in the view (the island's box, at the height it is drawn at). */
    private static boolean visible(RenderLevelStageEvent event, SokoviaClient.Island island, double offset) {
        BlockPos c = island.centre;
        int extent = island.spec.extent();
        AABB box = new AABB(c.getX() - extent, c.getY() - island.spec.cone() - island.spec.skin() + offset, c.getZ() - extent,
                c.getX() + extent + 1, c.getY() + island.spec.cover() + island.spec.skin() + offset + 1, c.getZ() + extent + 1);
        return event.getFrustum().isVisible(box);
    }

    private SokoviaIslandRenderer() {
    }
}
