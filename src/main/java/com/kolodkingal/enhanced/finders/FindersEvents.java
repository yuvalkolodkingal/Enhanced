package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The finders' server events (3.7, lane L5): the site searches and the amplified Cerebro run on the server's tick; a
 * player who leaves or changes dimension takes his searches and his helmet with him; the server's stop forgets all.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class FindersEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SiteSearch.tick();
        CerebroRoom.tick(event.getServer());
        if (event.getServer().getTickCount() % 20 == 0) {
            SiteSearch.forgetIdle(event.getServer().overworld().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CerebroRoom.forget(player);
            SiteSearch.forget(player.getUUID());
            StoneLocator.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CerebroRoom.forget(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SiteSearch.clear();
        CerebroRoom.clear();
        StoneLocator.clear();
    }

    private FindersEvents() {
    }
}
