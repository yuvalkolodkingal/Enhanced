package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;

/**
 * The finders' server events (3.7, lane L5): the site searches and the amplified Cerebro run on the server's tick; a
 * player who leaves or changes dimension takes his searches and his helmet with him; the server's stop forgets all.
 */
@Subscriber
public final class FindersEvents {

    @Subscribe
    public static void onServerTick(ServerTickEvent.Post event) {
        SiteSearch.tick();
        CerebroRoom.tick(event.getServer());
        if (event.getServer().getTickCount() % 20 == 0) {
            SiteSearch.forgetIdle(event.getServer().overworld().getGameTime());
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CerebroRoom.forget(player);
            SiteSearch.forget(player.getUUID());
            StoneLocator.forget(player.getUUID());
        }
    }

    @Subscribe
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CerebroRoom.forget(player);
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        SiteSearch.clear();
        CerebroRoom.clear();
        StoneLocator.clear();
    }

    private FindersEvents() {
    }
}
