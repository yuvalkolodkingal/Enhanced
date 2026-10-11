package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The game-bus events of the Sokovia lift, at the priorities of {@code docs/3.0-fragments/README.md}:
 *
 * <ul>
 *     <li><b>HIGHEST {@code LivingIncomingDamageEvent}:</b> a player the lift is carrying is immune to everything but
 *     what bypasses invulnerability (the void, {@code /kill}); and for 40 ticks after they are set down, to the fight's
 *     own creatures and bolts (the Sentries and Ultron resume after it).</li>
 *     <li><b>Death:</b> a player who dies is no longer on the island or carried; the pit's tractor takes them up when
 *     they come back.</li>
 *     <li><b>Login and logout:</b> a player who logs out in the sky is let go and remembered; a player who logs in is
 *     put back with the island, or set down on the ground where it no longer is ({@link SokoviaRecovery}).</li>
 *     <li><b>Server start:</b> the notes the lift left in the world are recovered (a crash left an island in the sky);
 *     <b>server stop:</b> a recovery that is not done is finished. (The fights themselves are finished by the boss
 *     framework, which restores the island synchronously.)</li>
 *     <li><b>Level tick:</b> a recovery works on its island, a slice a tick.</li>
 *     <li><b>Start tracking:</b> a player who comes within sight of the island's key is told what it is doing.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class SokoviaEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!SokoviaLift.any() || !(event.getEntity() instanceof ServerPlayer player) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (SokoviaLift.isCarried(player) || (SokoviaLift.inGrace(player) && SokoviaLift.fromAFight(event.getSource()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SokoviaRecovery.onLogin(player);
            SokoviaLift.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SokoviaLift.onLogout(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SokoviaLift.onDeath(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof VibraniumKeyEntity key) {
            SokoviaLift.tellNewcomer(player, key);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        SokoviaRecovery.recoverAll(event.getServer());
    }

    /** After the boss framework has finished the fights (and with them any island of a fight). */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SokoviaRecovery.finishAllNow();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SokoviaLift.clear();
        SokoviaRecovery.clear();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SokoviaRecovery.tick(level);
        }
    }

    private SokoviaEvents() {
    }
}
