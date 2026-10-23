package com.kolodkingal.enhanced.music;

import com.kolodkingal.enhanced.platform.event.OnDatapackSyncEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;

/**
 * The life of the world's music (3.5): read when the server starts and on /reload, told to each player when he joins and
 * after a /reload, and sent piece by piece with the server's ticks.
 */
@Subscriber
public final class MusicEvents {

    @Subscribe
    public static void onServerStarted(ServerStartedEvent event) {
        ServerMusic.reload(event.getServer());
    }

    /** At login (one player), and after /reload (every player, the folder read again first). */
    @Subscribe
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            ServerMusic.reload(event.getPlayerList().getServer());
        }
        event.getRelevantPlayers().forEach(ServerMusic::sendManifest);
    }

    @Subscribe
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerMusic.tick(event.getServer());
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerMusic.forget(player.getUUID());
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        ServerMusic.clear();
    }

    private MusicEvents() {
    }
}
