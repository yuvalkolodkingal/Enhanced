package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.platform.net.Net;

/**
 * The message the speedsters send to the server (3.3: what a running Quicksilver or Tommy runs on), registered by their
 * own lane like {@link SpiderNetwork}. It joins the same protocol version.
 */
public final class SpeedNetwork {
    public static void register() {
        Net.channel(ModNetwork.PROTOCOL_VERSION).serverbound(SurfaceRunPayload.TYPE, SurfaceRunPayload.STREAM_CODEC,
                SurfaceRunPayload::handle);
    }

    private SpeedNetwork() {
    }
}
