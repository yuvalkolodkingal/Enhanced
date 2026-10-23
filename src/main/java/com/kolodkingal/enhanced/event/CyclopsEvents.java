package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import com.kolodkingal.enhanced.platform.event.LivingFallEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.world.entity.player.Player;

/**
 * Cyclops's own game event (3.3), so that {@code CommonEvents} needs no edit: he lands an Optic Leap without fall damage.
 * (His life, death and leaving reach him through {@code HeroLifecycle}.) Not ordered against any other handler.
 */
@Subscriber
public final class CyclopsEvents {
    /** The fall that ends an Optic Leap does him no harm (a fall from higher than the leap took him still would not). */
    @Subscribe
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && CyclopsAbilities.leaping(player)) {
            event.setDamageMultiplier(0.0F);
        }
    }

    private CyclopsEvents() {
    }
}
