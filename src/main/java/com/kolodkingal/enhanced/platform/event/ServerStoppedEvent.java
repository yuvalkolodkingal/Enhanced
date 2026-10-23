package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.MinecraftServer;

/** The server has stopped: clear what was kept for it. */
public class ServerStoppedEvent extends ServerLifecycleEvent {
    public ServerStoppedEvent(MinecraftServer server) {
        super(server);
    }
}
