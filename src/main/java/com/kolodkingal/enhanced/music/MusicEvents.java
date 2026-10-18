package com.kolodkingal.enhanced.music;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The life of the world's music (3.5): read when the server starts and on /reload, told to each player when he joins and
 * after a /reload, and sent piece by piece with the server's ticks.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class MusicEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerMusic.reload(event.getServer());
    }

    /** At login (one player), and after /reload (every player, the folder read again first). */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            ServerMusic.reload(event.getPlayerList().getServer());
        }
        event.getRelevantPlayers().forEach(ServerMusic::sendManifest);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerMusic.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerMusic.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ServerMusic.clear();
    }

    private MusicEvents() {
    }
}
