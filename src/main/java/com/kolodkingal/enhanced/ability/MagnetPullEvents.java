package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The game-bus events of Magneto's Magnetic Pull, at NORMAL priority (spec 5.6; R16: {@code CommonEvents} is not edited
 * for it):
 *
 * <ul>
 *     <li><b>Logout, death, change of dimension:</b> his pull ends at once; what it took is already his (D-M 4.8).</li>
 *     <li><b>Server stopped:</b> every pull and every grace is forgotten, so nothing of one world reaches the next.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class MagnetPullEvents {

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagnetPull.cancel(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagnetPull.cancel(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagnetPull.cancel(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MagnetPull.clear();
    }

    private MagnetPullEvents() {
    }
}
