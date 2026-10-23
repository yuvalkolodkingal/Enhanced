package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityTravelToDimensionEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.Horse;

/**
 * The Asgardians' part of the game's events (3.5): the keeper's beat in Asgard, the refusal of duplicates as they join,
 * the city's people never leaving their realm, and the talks forgotten when a player leaves or the server stops.
 */
@Subscriber
public final class AsgardNpcEvents {

    private AsgardNpcEvents() {
    }

    @Subscribe
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && AsgardDimension.is(level)
                && level.getGameTime() % AsgardPopulation.PERIOD == 0 && !(level.getServer() instanceof GameTestServer)) {
            AsgardPopulation.tickNow(level);
        }
    }

    @Subscribe
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && (event.getEntity() instanceof AsgardianEntity || event.getEntity() instanceof Horse)
                && AsgardPopulation.refuses(level, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** The city's Asgardians never leave their realm (the Bifrost's circle, a portal, anything). */
    @Subscribe
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof AsgardianEntity npc && !npc.summoned()) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AsgardTalk.end(player);
            AsgardTalk.forget(player.getUUID());
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        AsgardTalk.clear();
        AsgardianGoals.SparPlayer.clear();
    }
}
