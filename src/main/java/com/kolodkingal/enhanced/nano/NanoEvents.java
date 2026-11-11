package com.kolodkingal.enhanced.nano;

import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.world.entity.player.Player;

/** Ultron's nano form stops physical blows (3.12). */
@Subscriber
public final class NanoEvents {
    private NanoEvents() {
    }

    @Subscribe
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && NanoUltron.shrugsOff(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NanoUltron.forget(event.getEntity());
    }
}
