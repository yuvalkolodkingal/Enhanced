package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.platform.net.Net;

/**
 * The messages Spider-Man sends to the server (what he clings to; since 3.3 Web Assist's swings), registered by his own lane (the other messages of 3.0 are the server's
 * to the clients and were all registered with {@link ModNetwork}, which no lane edits). It joins the same protocol
 * version.
 */
public final class SpiderNetwork {
    public static void register() {
        Net.channel(ModNetwork.PROTOCOL_VERSION).serverbound(SpiderClingPayload.TYPE, SpiderClingPayload.STREAM_CODEC,
                SpiderClingPayload::handle);
        // 3.3: Web Assist's swings.
        Net.channel(ModNetwork.PROTOCOL_VERSION).serverbound(SpiderSwingPayload.TYPE, SpiderSwingPayload.STREAM_CODEC,
                SpiderSwingPayload::handle);
    }

    private SpiderNetwork() {
    }
}
