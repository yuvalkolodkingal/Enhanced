package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.platform.event.AttackEntityEvent;
import com.kolodkingal.enhanced.platform.event.ExplosionEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingDropsEvent;
import com.kolodkingal.enhanced.platform.event.LivingExperienceDropEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Vormir Cliff's part of the game's events (3.6): the look at the cliffs' edges and the pets' souls every tick (and
 * the keepers' places on a real server), the pushes read from hits and blasts, the judgment of a death (last, so a
 * totem, a rebirth or any other rescue that cancels the death comes first: then nothing happens), the swallowed drops
 * and experience of a sacrifice, the message on respawn, and the records forgotten when a player leaves his level or
 * the server stops.
 */
@Subscriber
public final class VormirEvents {

    private VormirEvents() {
    }

    @Subscribe
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            VormirTracker.tick(level);
            Sacrifice.tickWisps(level);
            if (!(level.getServer() instanceof GameTestServer)) {
                VormirKeeper.tick(level);
            }
        }
    }

    /** A hit that reaches a creature is a push by whoever made it (a creature's: by no one). */
    @Subscribe(priority = Priority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity by = event.getSource().getEntity();
        if (by != null && !event.getEntity().level().isClientSide()) {
            VormirTracker.credit(event.getEntity(), by);
        }
    }

    /**
     * A player attacks a creature (before the game decides whether the blow lands: with PvP off it does not): kept, so
     * that a seeker whose blow did not land is told why his sacrifice did not count (3.6.1).
     */
    @Subscribe
    public static void onAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof LivingEntity victim) {
            VormirTracker.attempt(victim, event.getEntity());
        }
    }

    /** A blast that throws creatures (a wind charge, TNT a player lit) is a push by whoever set it off. */
    @Subscribe
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Entity by = event.getExplosion().getIndirectSourceEntity();
        if (by == null || event.getLevel().isClientSide()) {
            return;
        }
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof LivingEntity living) {
                VormirTracker.credit(living, by);
            }
        }
    }

    @Subscribe(priority = Priority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && !event.getEntity().level().isClientSide()) {
            Sacrifice.onDeath(event.getEntity(), event.getSource());
        }
    }

    @Subscribe(priority = Priority.LOWEST)
    public static void onExperience(LivingExperienceDropEvent event) {
        if (Sacrifice.swallows(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        if (Sacrifice.swallows(event.getEntity())) {
            event.setCanceled(true);
            Sacrifice.swallowed(event.getEntity());
        }
    }

    @Subscribe
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sacrifice.onRespawn(player);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        VormirTracker.forget(event.getEntity().getUUID());
    }

    @Subscribe
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        VormirTracker.forget(event.getEntity().getUUID());
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        VormirTracker.clear();
        Sacrifice.clear();
    }
}
