package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;


/**
 * The Asgardians' part of the game's events (3.5): the keeper's beat in Asgard, the refusal of duplicates as they join,
 * the city's people never leaving their realm, and the talks forgotten when a player leaves or the server stops.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class AsgardNpcEvents {

    private AsgardNpcEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && AsgardDimension.is(level)
                && level.getGameTime() % AsgardPopulation.PERIOD == 0 && !(level.getServer() instanceof GameTestServer)) {
            AsgardPopulation.tickNow(level);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && (event.getEntity() instanceof AsgardianEntity || event.getEntity() instanceof Horse)
                && AsgardPopulation.refuses(level, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** The city's Asgardians never leave their realm (the Bifrost's circle, a portal, anything). */
    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof AsgardianEntity npc && !npc.summoned()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AsgardTalk.end(player);
            AsgardTalk.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AsgardTalk.clear();
        AsgardianGoals.SparPlayer.clear();
    }
}
