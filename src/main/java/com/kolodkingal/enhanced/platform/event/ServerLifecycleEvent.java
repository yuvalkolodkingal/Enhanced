package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.MinecraftServer;

/** A step of the server's life (NeoForge's ServerLifecycleEvent). */
public abstract class ServerLifecycleEvent {
    private final MinecraftServer server;

    protected ServerLifecycleEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
