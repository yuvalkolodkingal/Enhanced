package com.kolodkingal.enhanced.platform.net;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Where a message came from, given to its handler (replaces NeoForge's {@code IPayloadContext}; the method names are
 * the same). Handlers run on the game thread of the side that received the message.
 */
public interface PacketContext {
    /** The player the message is about: the sender on the server, the local player on a client. */
    Player player();

    /** Runs {@code work} on the game thread (handlers already run there; kept for code that hops threads). */
    void enqueueWork(Runnable work);

    /** Whether the message travelled from the server to a client. */
    boolean isClientbound();

    /** Ends the connection with {@code reason}. */
    void disconnect(Component reason);
}
