package com.kolodkingal.enhanced.client.sokovia;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The shake of the camera while the island moves: a slow, low rumble (roll and pitch of under a degree) that grows with
 * the speed of the move and falls off with the distance from the island, for everybody within sight of it. It is not
 * played with the "reduced motion" option on, and it is nothing but the camera: the player is not moved.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SokoviaCamera {
    /** The shake reaches this far past the island's edge, falling off to nothing. */
    private static final double REACH = 48.0;
    /** The strongest roll or pitch, in degrees. */
    private static final float STRENGTH = 0.9F;

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        SokoviaClient.Island island = SokoviaClient.current();
        if (island == null || EnhancedClientConfig.reducedMotion() || Minecraft.getInstance().level != island.level) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float shake = SokoviaClient.shake(island, partial);
        if (shake <= 0.0F) {
            return;
        }
        BlockPos c = island.centre;
        Vec3 camera = event.getCamera().getPosition();
        double dx = camera.x - (c.getX() + 0.5);
        double dz = camera.z - (c.getZ() + 0.5);
        double beyond = Math.sqrt(dx * dx + dz * dz) - island.spec.radius();
        if (beyond > REACH) {
            return;
        }
        float near = (float) (1.0 - Math.max(0.0, beyond) / REACH);
        double time = island.level.getGameTime() + partial;
        float amount = STRENGTH * shake * near * near;
        event.setRoll(event.getRoll() + amount * (float) (Math.sin(time * 0.9) + 0.5 * Math.sin(time * 2.3 + 1.0)));
        event.setPitch(event.getPitch() + 0.6F * amount * (float) Math.sin(time * 1.3 + 2.0));
    }

    private SokoviaCamera() {
    }
}
