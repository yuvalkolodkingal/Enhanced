package com.kolodkingal.enhanced.tower;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The game's side of the Avengers Tower's armoury (3.8): each player's tick looks for old towers to refit near him
 * ({@link TowerRefit#tick}, every {@code refitIntervalTicks}); the Overworld's tick takes the refits under way a step
 * further ({@link TowerRefit#work}); a stopped server forgets the towers it knew near each chunk.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class TowerEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TowerRefit.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TowerRefit.work(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TowerRefit.clearCache();
    }

    private TowerEvents() {
    }
}
