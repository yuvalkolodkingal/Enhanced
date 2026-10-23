package com.kolodkingal.enhanced.ironman.hulkbuster;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.nano.NanoTech;
import com.kolodkingal.enhanced.platform.event.CanPlayerSleepEvent;
import com.kolodkingal.enhanced.platform.event.EntityMountEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;

/**
 * The Hulkbuster's event hooks (3.8, lane L4, PLAN 6.4). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul>
 * <li>{@link PlayerTickEvent.Post}: its body (scale, step, reach, with the phase's growth: {@link
 * Hulkbuster#applyBody}), Veronica's arm landing, the thrown creatures' landing;</li>
 * <li>{@link LivingIncomingDamageEvent}, HIGHEST: <b>explosion immunity</b> while it is worn ({@code
 * #minecraft:is_explosion} cancelled, his own booms included; {@code explosionImmune});</li>
 * <li>{@link EntityMountEvent} and {@link CanPlayerSleepEvent}: he cannot ride or sleep in it (his size);</li>
 * <li>death: he is his own size at once; a logout forgets his moves.</li>
 * </ul>
 */
@Subscriber
public final class HulkbusterEvents {

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Hulkbuster.applyBody(player);
            HulkbusterMoves.wearerTick(player, Powers.get(player), player.level().getGameTime());
        }
    }

    /** Whether he wears it now (grown or growing). */
    public static boolean wearing(Player player) {
        return Hulkbuster.onChest(player) && Hulkbuster.mayHaveBody(player);
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onExplosion(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && wearing(player) && NanoTech.cfg(IronManConfig.HULKBUSTER_EXPLOSION_IMMUNE, true)) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof Player player && wearing(player)) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onSleep(CanPlayerSleepEvent event) {
        if (wearing(event.getEntity())) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @Subscribe(priority = Priority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            Hulkbuster.shrinkNow(player);
            HulkbusterMoves.forget(player);
            Veronica.cancel(player.getUUID());
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HulkbusterMoves.forget(player);
            Veronica.cancel(player.getUUID());
            Hulkbuster.forget(player.getUUID());
        }
    }

    @Subscribe
    public static void onStopped(ServerStoppedEvent event) {
        HulkbusterMoves.clear();
        Veronica.clear();
        Hulkbuster.clear();
    }

    private HulkbusterEvents() {
    }
}
