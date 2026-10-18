package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.AnimMath;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.network.WardSyncPayload;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.vishanti.Ward;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The domes of the wards of the Book of the Vishanti (3.2): a faint veil and glowing hexagons on a sphere round each
 * ward, in the colour of its sorcerer (the Scarlet Witch's crimson, Billy's blue, Loki's green), drawn the way the Hex's
 * wall is. Light on purpose: a ward is only drawn while its edge is within {@link #REACH} blocks of the camera, with a
 * mesh no finer than the radius needs, and only wards the server sent for this dimension ({@link WardSyncPayload}) exist
 * here. With <em>Reduced motion</em> the hexagons do not drift and the veil does not breathe.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class WardFx {
    /** A ward is drawn while its edge is closer to the camera than this. */
    private static final double REACH = 64.0;
    /** Over the last blocks of its reach a ward fades out (3.3). */
    private static final float FADE = 16.0F;
    private static final ResourceLocation HEXAGONS = Enhanced.id("textures/entity/hex_wall.png");

    private static final Map<ResourceKey<Level>, List<WardSyncPayload.Dome>> DOMES = new HashMap<>();

    /** The server's list of the wards of a dimension (called from the payload, on the client's thread). */
    public static void accept(WardSyncPayload payload) {
        DOMES.put(payload.dimension(), payload.domes());
        // 3.3: the walls of the common code know them too (a dark beam is drawn to where a ward stops it).
        Shelters.acceptWards(payload.dimension(), payload.domes().stream()
                .map(d -> new Ward(Util.NIL_UUID, "", new Vec3(d.x(), d.y(), d.z()), d.radius(), d.color())).toList());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DOMES.clear();
        Shelters.forgetWards();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        List<WardSyncPayload.Dome> domes = DOMES.get(level.dimension());
        if (domes == null || domes.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean still = EnhancedClientConfig.reducedMotion();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (int pass = 0; pass < 2; pass++) {
            boolean lines = pass == 1;
            RenderType type = lines ? EnhancedRenderTypes.energyField(HEXAGONS) : EnhancedRenderTypes.ENERGY_VEIL;
            VertexConsumer vc = null;
            for (WardSyncPayload.Dome dome : domes) {
                Vec3 center = new Vec3(dome.x(), dome.y(), dome.z()).subtract(camera);
                float radius = dome.radius();
                double edge = Math.abs(center.length() - radius);
                if (edge > REACH) {
                    continue;
                }
                // (3.3) It fades in over the last blocks of its reach instead of popping in and out at the limit.
                float reach = 1.0F - AnimMath.ss((float) REACH - FADE, (float) REACH, (float) edge);
                if (vc == null) {
                    vc = buffers.getBuffer(type);
                }
                int segments = Mth.clamp((int) (radius * 1.5F), 24, 96);
                float breath = (still ? 1.0F : 0.85F + 0.15F * Mth.sin(time * 0.05F)) * reach;
                if (lines) {
                    float tilesU = Math.max(6, Math.round(Mth.TWO_PI * radius / 8F));
                    float tilesV = Math.max(3, Math.round(Mth.PI * radius / 8F));
                    EnergyGeometry.sphere(pose, vc, center, radius, segments, segments / 2, tilesU, tilesV, still ? 0F : time * 0.001F,
                            dome.color(), 0.05F * breath, 0.45F * breath);
                } else {
                    EnergyGeometry.veil(pose, vc, center, radius, segments, segments / 2, dome.color(), 0.03F * breath, 0.22F * breath);
                }
            }
            if (vc != null) {
                buffers.endBatch(type);
            }
        }
    }

    private WardFx() {
    }
}
