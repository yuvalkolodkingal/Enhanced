package com.kolodkingal.enhanced.serum;

import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;

/**
 * The serum's open choices are forgotten when their player leaves, dies or changes dimension, and when the server
 * stops (3.8, PLAN 2.1): an answer that comes after is refused.
 */
@Subscriber
public final class SerumEvents {
    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SerumServer.forget(player);
        }
    }

    @Subscribe
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SerumServer.forget(player);
        }
    }

    @Subscribe
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SerumServer.forget(player);
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        SerumServer.clear();
    }

    private SerumEvents() {
    }
}
