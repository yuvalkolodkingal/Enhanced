package com.kolodkingal.enhanced.platform.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** (3.17) A player sent a chat message to the server; cancelling it keeps it from everyone else. */
public class ServerChatEvent implements Cancellable {
    private final ServerPlayer player;
    private final String rawText;
    private Component message;
    private boolean canceled;

    public ServerChatEvent(ServerPlayer player, String rawText, Component message) {
        this.player = player;
        this.rawText = rawText;
        this.message = message;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getRawText() {
        return rawText;
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
