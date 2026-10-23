package com.kolodkingal.enhanced.tower;

import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The game's side of the Avengers Tower's armoury (3.8): each player's tick looks for old towers to refit near him
 * ({@link TowerRefit#tick}, every {@code refitIntervalTicks}); the Overworld's tick takes the refits under way a step
 * further ({@link TowerRefit#work}); a stopped server forgets the towers it knew near each chunk.
 */
@Subscriber
public final class TowerEvents {

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TowerRefit.tick(player);
        }
    }

    @Subscribe
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TowerRefit.work(level);
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        TowerRefit.clearCache();
    }

    private TowerEvents() {
    }
}
