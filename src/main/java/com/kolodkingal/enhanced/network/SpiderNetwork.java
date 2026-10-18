package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The messages Spider-Man sends to the server (what he clings to; since 3.3 Web Assist's swings), registered by his own lane (the other messages of 3.0 are the server's
 * to the clients and were all registered with {@link ModNetwork}, which no lane edits). It joins the same protocol
 * version.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class SpiderNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(ModNetwork.PROTOCOL_VERSION).playToServer(SpiderClingPayload.TYPE, SpiderClingPayload.STREAM_CODEC,
                SpiderClingPayload::handle);
        // 3.3: Web Assist's swings.
        event.registrar(ModNetwork.PROTOCOL_VERSION).playToServer(SpiderSwingPayload.TYPE, SpiderSwingPayload.STREAM_CODEC,
                SpiderSwingPayload::handle);
    }

    private SpiderNetwork() {
    }
}
