package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The message the speedsters send to the server (3.3: what a running Quicksilver or Tommy runs on), registered by their
 * own lane like {@link SpiderNetwork}. It joins the same protocol version.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class SpeedNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(ModNetwork.PROTOCOL_VERSION).playToServer(SurfaceRunPayload.TYPE, SurfaceRunPayload.STREAM_CODEC,
                SurfaceRunPayload::handle);
    }

    private SpeedNetwork() {
    }
}
