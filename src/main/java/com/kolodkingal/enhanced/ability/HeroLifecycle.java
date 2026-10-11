package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.boss.BossFights;
import net.minecraft.server.level.ServerPlayer;

/**
 * What every hero of 3.0 (and the boss fights) must do when a player's session changes: the one place
 * {@code CommonEvents} calls, once per event, so that a hero never edits that file. Each call goes to every
 * {@link HeroServer} (the hero checks whether the player is its own), after the code of the older heroes that
 * {@code CommonEvents} runs itself. It runs at the default event priority, so the chain of {@code CommonEvents} is
 * undisturbed.
 */
public final class HeroLifecycle {

    public static void onLogin(ServerPlayer player) {
        for (HeroServer server : HeroServers.all()) {
            server.onLogin(player);
        }
    }

    public static void onLogout(ServerPlayer player) {
        for (HeroServer server : HeroServers.all()) {
            server.onLogout(player);
        }
    }

    public static void onDeath(ServerPlayer player) {
        for (HeroServer server : HeroServers.all()) {
            server.onDeath(player);
        }
    }

    public static void onRespawn(ServerPlayer player) {
        for (HeroServer server : HeroServers.all()) {
            server.onRespawn(player);
        }
    }

    public static void onChangeDimension(ServerPlayer player) {
        for (HeroServer server : HeroServers.all()) {
            server.onChangeDimension(player);
        }
    }

    /** The server stopped: every hero and the boss fights forget their statics. */
    public static void onServerStopped() {
        try {
            for (HeroServer server : HeroServers.all()) {
                server.onServerStopped();
            }
        } finally {
            // (A hero that fails to forget must not leave a boss fight of the stopped server in the next world.)
            BossFights.clear();
        }
    }

    private HeroLifecycle() {
    }
}
