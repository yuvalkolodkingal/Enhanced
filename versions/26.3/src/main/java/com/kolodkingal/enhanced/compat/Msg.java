package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** 26.3 only: a player's message, as 1.21.1's {@code displayClientMessage(message, overlay)} gave it. */
public final class Msg {
    private Msg() {
    }

    public static void display(Player player, Component message, boolean overlay) {
        if (overlay) {
            player.sendOverlayMessage(message);
        } else {
            player.sendSystemMessage(message);
        }
    }
}
