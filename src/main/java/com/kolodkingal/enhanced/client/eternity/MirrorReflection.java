package com.kolodkingal.enhanced.client.eternity;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.eternity.EternityEntity;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

/**
 * The reflections in Eternity's still water (3.8, PLAN 10.4): after the solid blocks and before the translucent ones,
 * Eternity and every player within {@link #RANGE} blocks are drawn again, mirrored about the mirror's top plane (y 64):
 * the same renderers through a pose scaled (1, -1, 1) about the plane, the winding of the faces turned round for the
 * flip (so culling keeps the right faces). The translucent mirror blocks are drawn over them afterwards: a soft, tinted
 * reflection, as in the film. Particles and the HUD are not mirrored. Off with the client option {@code
 * eternityReflections} (for slow computers).
 */
@Subscriber(side = Side.CLIENT)
public final class MirrorReflection {
    /** How far (blocks) a player may be from the camera to be reflected. */
    public static final double RANGE = 64.0;
    /** The mirror's top plane. */
    public static final double PLANE = EternityRealm.MIRROR_Y + 1;
    private static boolean drawing;

    /** Whether the reflection pass is drawing now (Eternity's renderer maps his starfield back). */
    public static boolean drawing() {
        return drawing;
    }

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.dimension() != EternityRealm.LEVEL || !EnhancedClientConfig.eternityReflections()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        if (camera.y < PLANE) {
            return; // (under the water there is nothing to see it from)
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        buffers.endBatch();
        // (Camera-relative, as the entities are drawn: the view's turn is in the render system's model-view matrix.)
        PoseStack pose = new PoseStack();
        // Mirror about the plane, in camera space: up to the plane, flip, back down.
        pose.translate(0.0, PLANE - camera.y, 0.0);
        pose.scale(1.0F, -1.0F, 1.0F);
        pose.translate(0.0, -(PLANE - camera.y), 0.0);
        drawing = true;
        GL11.glFrontFace(GL11.GL_CW);
        try {
            for (Entity entity : level.entitiesForRendering()) {
                boolean reflected = entity instanceof EternityEntity
                        || entity instanceof Player p && !p.isSpectator() && p.distanceToSqr(camera) < RANGE * RANGE;
                if (!reflected || entity.isInvisible() || entity.getY() < PLANE - 0.5) {
                    continue;
                }
                // (The camera's own body too, in first person: you see yourself in the water.)
                Vec3 at = entity.getPosition(partialTick).subtract(camera);
                float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
                int light = dispatcher.getPackedLightCoords(entity, partialTick);
                dispatcher.render(entity, at.x, at.y, at.z, yaw, partialTick, pose, buffers, light);
            }
            buffers.endBatch();
        } catch (RuntimeException e) {
            Enhanced.LOGGER.debug("Eternity's reflection skipped a frame", e);
        } finally {
            GL11.glFrontFace(GL11.GL_CCW);
            drawing = false;
        }
    }

    private MirrorReflection() {
    }
}
