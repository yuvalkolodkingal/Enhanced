package com.kolodkingal.enhanced.balance;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartingEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.server.level.ServerPlayer;

/** The SMP balance's side of the game (3.16): read with the world, sent to each player who joins, dropped at the end. */
@Subscriber
public final class BalanceEvents {
    private BalanceEvents() {
    }

    @Subscribe
    public static void onServerStarting(ServerStartingEvent event) {
        String problem = Balance.load(event.getServer());
        if (problem != null) {
            Enhanced.LOGGER.error("The SMP balance file has a problem ({}); the full preset is used", problem);
        } else if (Balance.preset() != Balance.Preset.FULL || !Balance.overrides().isEmpty()) {
            Enhanced.LOGGER.info("SMP balance: preset {}, {} overrides", Balance.preset().id(), Balance.overrides().size());
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        Balance.reset();
    }

    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Net.sendToPlayer(player, BalanceSyncPayload.now());
        }
    }
}
