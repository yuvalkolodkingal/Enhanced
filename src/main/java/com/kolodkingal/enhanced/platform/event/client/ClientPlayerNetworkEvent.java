package com.kolodkingal.enhanced.platform.event.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.Nullable;

/** The client's player and its connection (NeoForge's ClientPlayerNetworkEvent). */
public abstract class ClientPlayerNetworkEvent {
    private final @Nullable MultiPlayerGameMode multiPlayerGameMode;
    private final @Nullable LocalPlayer player;
    private final @Nullable Connection connection;

    protected ClientPlayerNetworkEvent(
            @Nullable MultiPlayerGameMode multiPlayerGameMode, @Nullable LocalPlayer player,
            @Nullable Connection connection) {
        this.multiPlayerGameMode = multiPlayerGameMode;
        this.player = player;
        this.connection = connection;
    }

    @Nullable
    public MultiPlayerGameMode getMultiPlayerGameMode() {
        return multiPlayerGameMode;
    }

    @Nullable
    public LocalPlayer getPlayer() {
        return player;
    }

    @Nullable
    public Connection getConnection() {
        return connection;
    }

    /** The client's player joined a server. */
    public static class LoggingIn extends ClientPlayerNetworkEvent {
        public LoggingIn(
                @Nullable MultiPlayerGameMode multiPlayerGameMode, @Nullable LocalPlayer player,
                @Nullable Connection connection) {
            super(multiPlayerGameMode, player, connection);
        }
    }

    /** The client's player left the server (or never got in): clear the client's state. */
    public static class LoggingOut extends ClientPlayerNetworkEvent {
        public LoggingOut(
                @Nullable MultiPlayerGameMode multiPlayerGameMode, @Nullable LocalPlayer player,
                @Nullable Connection connection) {
            super(multiPlayerGameMode, player, connection);
        }
    }

    /** The client's player object was replaced (respawn, dimension change). */
    public static class Clone extends ClientPlayerNetworkEvent {
        private final LocalPlayer oldPlayer;

        public Clone(
                @Nullable MultiPlayerGameMode multiPlayerGameMode, @Nullable LocalPlayer player,
                @Nullable Connection connection, LocalPlayer oldPlayer) {
            super(multiPlayerGameMode, player, connection);
            this.oldPlayer = oldPlayer;
        }

        public LocalPlayer getOldPlayer() {
            return oldPlayer;
        }

        @Nullable
        public LocalPlayer getNewPlayer() {
            return getPlayer();
        }
    }
}
