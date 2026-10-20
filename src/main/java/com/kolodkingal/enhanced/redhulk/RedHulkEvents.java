package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Red Hulk's event hooks (3.8). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul> <li>{@link LivingIncomingDamageEvent}, HIGH: the Super Jump's one landing takes no fall damage ({@link
 * RedHulkState#FALL_SAFE}, spent by it), and while he absorbs, fire, lightning, explosions and beams do him no harm and
 * feed his anger ({@link EnergyAbsorb}); NORMAL: Fire Resistance, his body's (fire, lava, magma, a campfire, the
 * Phoenix's fire never hurt Red Hulk; Ross is not fire-proof);</li> <li>{@link LivingDamageEvent.Post}: what he took
 * (after armour) and what he dealt feed his anger ({@link Anger}).</li> </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class RedHulkEvents {
    private RedHulkEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!data.has(HeroAbility.RED_HULK_BODY)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FALL)) {
            RedHulkState state = Anger.get(player);
            if (state.has(RedHulkState.FALL_SAFE)) {
                Anger.set(player, state.with(RedHulkState.FALL_SAFE, false));
                event.setCanceled(true);
            }
            return;
        }
        if (RedHulkAbilities.hasBody(data) && EnergyAbsorb.active(player) && EnergyAbsorb.absorbs(source)) {
            EnergyAbsorb.soak(player, data, event.getAmount());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFire(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypeTags.IS_FIRE)
                && RedHulkAbilities.hasBody(Powers.get(player))
                && RedHulkAbilities.cfg(RedHulkConfig.FIRE_IMMUNE, true)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        float amount = event.getNewDamage();
        if (amount <= 0.0F) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerData data = Powers.get(player);
            if (data.has(HeroAbility.RED_HULK_BODY)) {
                Anger.hurt(player, data, amount);
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            PowerData data = Powers.get(attacker);
            if (data.has(HeroAbility.RED_HULK_BODY)) {
                Anger.dealt(attacker, data, amount);
            }
        }
    }
}
