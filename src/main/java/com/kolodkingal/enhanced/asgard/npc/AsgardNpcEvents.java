package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Asgardians' part of the game's events (3.5): the keeper's beat in Asgard, the refusal of duplicates as they join,
 * the city's people never leaving their realm, Heimdall greeting those the Bifrost brings, and the talks forgotten when a
 * player leaves or the server stops.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class AsgardNpcEvents {
    /** How long Heimdall waits before he greets the same player again (ticks). */
    static final int GREET_COOLDOWN = 1200;
    private static final Map<UUID, Long> GREETED = new HashMap<>();
    private static boolean listening;

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
    public static void onServerStarted(ServerStartedEvent event) {
        if (!listening) {
            listening = true;
            Bifrost.ARRIVED.add(AsgardNpcEvents::arrived);
        }
    }

    /** Someone the Bifrost brought to Asgard: Heimdall, if he is near, turns to him and greets him. */
    static void arrived(ServerLevel level, Vec3 at, ServerPlayer player) {
        if (!AsgardDimension.is(level)) {
            return;
        }
        AsgardianEntity heimdall = Asgardians.fixture(level, Role.HEIMDALL);
        long now = level.getGameTime();
        Long last = GREETED.get(player.getUUID());
        if (heimdall == null || heimdall.distanceToSqr(at) > 32.0 * 32.0 || heimdall.state().still()
                || last != null && now - last < GREET_COOLDOWN) {
            return;
        }
        GREETED.put(player.getUUID(), now);
        heimdall.getLookControl().setLookAt(player, 90.0F, 90.0F);
        AsgardianGoals.face(heimdall, player.position());
        AsgardTalk.speak(heimdall, player, AsgardDialogue.variant("npc.enhanced.heimdall.arrival", heimdall.getRandom()), List.of());
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
        GREETED.clear();
    }
}
