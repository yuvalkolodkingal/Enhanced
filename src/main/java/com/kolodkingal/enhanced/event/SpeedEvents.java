package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.ability.SpeedSurfaces;
import com.kolodkingal.enhanced.platform.event.LivingFallEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.world.entity.player.Player;

/**
 * The speedsters' 3.3 rule that needs a game event: a speedster who ran on a wall or a ceiling takes no fall damage
 * until he lands (dropping off when he slows down never hurts; see {@link SpeedSurfaces}). The tick's own reset of the
 * fall distance would not catch a fall faster than a few blocks a tick.
 */
@Subscriber
public final class SpeedEvents {
    @Subscribe
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && SpeedSurfaces.fallSafe(player)) {
            event.setDamageMultiplier(0.0F);
            event.setCanceled(true);
        }
    }

    private SpeedEvents() {
    }
}
