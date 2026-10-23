package com.kolodkingal.enhanced.platform.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/** Data packs are sent to a player who joined, or to everyone after /reload. */
public class OnDatapackSyncEvent {
    private final PlayerList playerList;
    private final @Nullable ServerPlayer player;

    public OnDatapackSyncEvent(PlayerList playerList, @Nullable ServerPlayer player) {
        this.playerList = playerList;
        this.player = player;
    }

    public PlayerList getPlayerList() {
        return playerList;
    }

    @Nullable
    public ServerPlayer getPlayer() {
        return player;
    }

    /** The players to send the data to: the one who joined, or everyone after a reload. */
    public Stream<ServerPlayer> getRelevantPlayers() {
        return player == null ? playerList.getPlayers().stream() : Stream.of(player);
    }
}
