package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppingEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;

/** The life of the Road: the trials run with the server's ticks, and players come and go. */
@Subscriber
public final class RoadEvents {

    @Subscribe
    public static void onServerTick(ServerTickEvent.Post event) {
        RoadTrials.serverTick(event.getServer());
        RoadMusic.tick(event.getServer());
    }

    @Subscribe
    public static void onServerStarted(ServerStartedEvent event) {
        RoadTrials.onServerStarted(event.getServer());
    }

    @Subscribe
    public static void onServerStopping(ServerStoppingEvent event) {
        RoadTrials.onServerStopping(event.getServer());
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        RoadTrials.clear();
        RoadMusic.clear();
    }

    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoadTrials.reconcile(player);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoadTrials.onLogout(player);
            RoadMusic.forget(player.getUUID());
        }
    }

    @Subscribe
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoadTrials.reconcile(player);
        }
    }

    private RoadEvents() {
    }
}
