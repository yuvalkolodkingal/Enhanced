package com.kolodkingal.enhanced.ironman.hulkbuster;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.nano.NanoTech;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
@EventBusSubscriber(modid = Enhanced.MODID)
public final class HulkbusterEvents {

    @SubscribeEvent
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosion(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && wearing(player) && NanoTech.cfg(IronManConfig.HULKBUSTER_EXPLOSION_IMMUNE, true)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof Player player && wearing(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSleep(CanPlayerSleepEvent event) {
        if (wearing(event.getEntity())) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            Hulkbuster.shrinkNow(player);
            HulkbusterMoves.forget(player);
            Veronica.cancel(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HulkbusterMoves.forget(player);
            Veronica.cancel(player.getUUID());
            Hulkbuster.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        HulkbusterMoves.clear();
        Veronica.clear();
        Hulkbuster.clear();
    }

    private HulkbusterEvents() {
    }
}
