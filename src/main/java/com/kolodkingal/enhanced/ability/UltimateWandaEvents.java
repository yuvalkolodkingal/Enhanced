package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.MobEffectEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ultimate Wanda's events (3.19, {@link UltimateWanda}): her death refused (her body comes back a second later), the shield of
 * red light over her body while her mind is in another (and over the body that is coming back), nothing that freezes her, and
 * what she held let go when she leaves.
 */
@Subscriber
public final class UltimateWandaEvents {
    private UltimateWandaEvents() {
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player && UltimateWanda.onDeath(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (UltimateWanda.guards(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.HIGH)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (UltimateWanda.refuses(event.getEntity(), event.getEffectInstance().getEffect())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UltimateWanda.onLogout(player);
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        UltimateWanda.clear();
    }
}
