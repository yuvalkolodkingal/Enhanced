package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.MinecraftServer;

/** The server has started. */
public class ServerStartedEvent extends ServerLifecycleEvent {
    public ServerStartedEvent(MinecraftServer server) {
        super(server);
    }
}
