package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.MinecraftServer;

/** The server is stopping (its levels are still there, about to be saved). */
public class ServerStoppingEvent extends ServerLifecycleEvent {
    public ServerStoppingEvent(MinecraftServer server) {
        super(server);
    }
}
