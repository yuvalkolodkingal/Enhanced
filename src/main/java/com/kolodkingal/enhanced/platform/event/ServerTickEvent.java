package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.MinecraftServer;

import java.util.function.BooleanSupplier;

/** The server's tick (NeoForge's ServerTickEvent). */
public abstract class ServerTickEvent {
    private final BooleanSupplier haveTime;
    private final MinecraftServer server;

    protected ServerTickEvent(BooleanSupplier haveTime, MinecraftServer server) {
        this.haveTime = haveTime;
        this.server = server;
    }

    public BooleanSupplier haveTime() {
        return haveTime;
    }

    public MinecraftServer getServer() {
        return server;
    }

    /** Whether the server has time left in this tick. */
    public boolean hasTime() {
        return haveTime.getAsBoolean();
    }

    /** Before the server's tick. */
    public static class Pre extends ServerTickEvent {
        public Pre(BooleanSupplier haveTime, MinecraftServer server) {
            super(haveTime, server);
        }
    }

    /** After the server's tick. */
    public static class Post extends ServerTickEvent {
        public Post(BooleanSupplier haveTime, MinecraftServer server) {
            super(haveTime, server);
        }
    }
}
