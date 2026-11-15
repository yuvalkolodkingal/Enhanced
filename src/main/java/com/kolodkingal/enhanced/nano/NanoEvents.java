package com.kolodkingal.enhanced.nano;

import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.world.entity.player.Player;

/** Ultron's nano form stops physical blows (3.12), and his own (3.15). */
@Subscriber
public final class NanoEvents {
    private NanoEvents() {
    }

    @Subscribe
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && NanoUltron.shrugsOff(player, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        // (3.15) ...and, with no body to strike with, he hurts no one while in it: no blow, no shot of his lands
        if (event.getSource().getEntity() instanceof Player attacker && NanoUltron.inForm(attacker)) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NanoUltron.forget(event.getEntity());
    }
}
