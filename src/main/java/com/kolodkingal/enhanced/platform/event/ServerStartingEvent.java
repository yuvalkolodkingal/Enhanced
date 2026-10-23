package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.MinecraftServer;

/** The server is starting (its levels are loaded, it does not tick yet). */
public class ServerStartingEvent extends ServerLifecycleEvent {
    public ServerStartingEvent(MinecraftServer server) {
        super(server);
    }
}
