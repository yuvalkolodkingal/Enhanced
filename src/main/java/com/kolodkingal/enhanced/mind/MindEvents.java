package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;

/** The mind's ticks and goodbyes (3.12). */
@Subscriber
public final class MindEvents {
    private MindEvents() {
    }

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MindPalace.tick(player);
            if (MindRealm.in(player)) {
                MindRealm.tick(player);
                MindQuest.tick(player);
            }
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MindPalace.forget(player);
            MindTalk.forget(player);
        }
    }

    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // (someone who logs in inside his mind is sent back: the people there are not kept while no one is in)
        if (event.getEntity() instanceof ServerPlayer player && MindRealm.in(player)) {
            MindRealm.leave(player);
        }
    }

    @Subscribe
    public static void onStopped(ServerStoppedEvent event) {
        MindPalace.clear();
        MindTalk.clear();
    }
}
