package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The game-bus events of telekinesis in 3.1 (every form: Wanda, Billy, the Phoenix, Magneto), in a class of their own so
 * that they compose with the 3.0 chain of {@code CommonEvents} by priority (spec 5.6).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class TelekinesisEvents {

    /**
     * The landing guard (D-M 3.8, R16): whatever a grip let go of that its holder may not hurt (a player with PvP off, a
     * teammate, another player's pet, drone or Hex's Vision) takes no fall, no wall and no telekinetic slam until it lands
     * ({@link Telekinesis#isSpared}), while {@code magneto.telekinesisSparesFriends} is on. {@code HIGH}: after the lift's
     * carry immunity ({@code HIGHEST}), before the 3.0 chain ({@code NORMAL}); and for every creature, not players only.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !Telekinesis.isSpared(event.getEntity())
                || !EnhancedConfig.TELEKINESIS_SPARES_FRIENDS.get()) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(ModDamageTypes.TELEKINETIC_SLAM)) {
            event.setCanceled(true);
        }
    }

    private TelekinesisEvents() {
    }
}
