package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Cyclops's own game event (3.3), so that {@code CommonEvents} needs no edit: he lands an Optic Leap without fall damage.
 * (His life, death and leaving reach him through {@code HeroLifecycle}.) Not ordered against any other handler.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class CyclopsEvents {
    /** The fall that ends an Optic Leap does him no harm (a fall from higher than the leap took him still would not). */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && CyclopsAbilities.leaping(player)) {
            event.setDamageMultiplier(0.0F);
        }
    }

    private CyclopsEvents() {
    }
}
