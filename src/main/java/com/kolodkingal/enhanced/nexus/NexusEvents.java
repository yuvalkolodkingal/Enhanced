package com.kolodkingal.enhanced.nexus;

import com.kolodkingal.enhanced.platform.event.EntityTravelToDimensionEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;

/** (3.19.2) The Nexus holds whom Ultimate Wanda banished ({@link Nexus}). */
@Subscriber
public final class NexusEvents {
    private NexusEvents() {
    }

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Nexus.tick(player);
        }
    }

    /** No power takes a banished player out of the Nexus (only her portal: {@link Nexus#leaveThrough}). */
    @Subscribe(priority = Priority.HIGHEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (Nexus.holds(event.getEntity(), event.getDimension())) {
            event.setCanceled(true);
            Nexus.refused((ServerPlayer) event.getEntity());
        }
    }

    /** Dead in the Nexus (or anywhere), a banished player is born again there. */
    @Subscribe
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !Nexus.in(player) && Nexus.banished(player)) {
            net.minecraft.server.level.ServerLevel nexus = Nexus.ready(player.server);
            if (nexus != null) {
                Nexus.send(player, nexus, Nexus.arrival(player.getRandom()));
            }
        }
    }
}
