package com.kolodkingal.enhanced.eternity;

import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The realm's rules (3.8, PLAN 10.4-10.5): every tick of a traveller ({@link EternityRealm#tick}: the screen, the way
 * home), the realm kept (Eternity there while someone is), nothing hurts there but {@code /kill} and the void, a
 * player who logs in inside it is sent home (on his first tick), one who dies there respawns as usual and forgets his
 * way back, and one who leaves it by any other way (a command, the Bifrost) ends his visit.
 */
@Subscriber
public final class EternityEvents {
    /** The players who logged in inside the realm: sent home on their first tick. */
    private static final Set<UUID> LOGGED_IN = new HashSet<>();

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (LOGGED_IN.remove(player.getUUID()) && EternityRealm.in(player)) {
            EternityRealm.returnHome(player);
            return;
        }
        if (EternityRealm.in(player)) {
            EternityRealm.tick(player);
        }
    }

    @Subscribe
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            EternityRealm.keep(event.getServer());
        }
    }

    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && EternityRealm.in(player)) {
            LOGGED_IN.add(player.getUUID());
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EternityRealm.left(player);
            LOGGED_IN.remove(player.getUUID());
        }
    }

    /** Nothing hurts in the realm but what bypasses invulnerability ({@code /kill}, the void). */
    @Subscribe(priority = Priority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && player.level().dimension() == EternityRealm.LEVEL
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** Death in the realm: the normal respawn, and the way back forgotten. */
    @Subscribe
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && EternityRealm.in(player)) {
            EternityRealm.left(player);
            EternityRealm.setProgress(player, EternityRealm.progress(player).withReturnTo(Optional.empty()));
        }
    }

    /** Out of the realm by any way: the visit is over (the way home kept only if he left without it). */
    @Subscribe
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getFrom() == EternityRealm.LEVEL) {
            EternityRealm.left(player);
            EternityRealm.setProgress(player, EternityRealm.progress(player).withReturnTo(Optional.empty()));
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        EternityRealm.clear();
        LOGGED_IN.clear();
    }

    private EternityEvents() {
    }
}
