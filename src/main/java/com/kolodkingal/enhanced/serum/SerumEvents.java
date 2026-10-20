package com.kolodkingal.enhanced.serum;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The serum's open choices are forgotten when their player leaves, dies or changes dimension, and when the server
 * stops (3.8, PLAN 2.1): an answer that comes after is refused.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class SerumEvents {
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SerumServer.forget(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SerumServer.forget(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SerumServer.forget(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SerumServer.clear();
    }

    private SerumEvents() {
    }
}
