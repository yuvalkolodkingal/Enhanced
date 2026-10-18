package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.config.HelaConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Hela's events (3.5): what the hero's damage becomes, and what her dead are spared.
 *
 * <ul>
 *     <li><b>Strength from Asgard</b>: a hit dealt by a player with her body ({@code hela.body}) in Asgard is multiplied by
 *     {@code hela.asgardFactor} (1.25).</li>
 *     <li><b>The Goddess of Death</b>: with her form on, her blades ({@code necrosword}, {@code blade_wall}) are 30
 *     percent stronger.</li>
 *     <li>Her dead are never hurt by her or her side (a sweep of her own sword, a teammate's arrow).</li>
 * </ul>
 * At {@link EventPriority#NORMAL}: before {@code HeroDamage} (LOW) scales a custom hero's hit by his strength.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class HelaEvents {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity victim = event.getEntity();
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && victim instanceof ArmyMob && HelaArmy.sameSide(living, victim)) {
            event.setCanceled(true);
            return;
        }
        if (!(attacker instanceof Player hela) || event.getAmount() <= 0.0F) {
            return;
        }
        float factor = factor(hela, source);
        if (factor != 1.0F) {
            event.setAmount(event.getAmount() * factor);
        }
    }

    /** What a hit of {@code hela}'s is multiplied by: Asgard's strength, and her form's for her blades. */
    public static float factor(Player hela, DamageSource source) {
        return factor(Powers.get(hela), AsgardDimension.in(hela), source);
    }

    /** {@link #factor(Player, DamageSource)} for her data and whether she is in Asgard (the tests ask it directly). */
    public static float factor(PowerData data, boolean inAsgard, DamageSource source) {
        float factor = 1.0F;
        if (data.has(HeroAbility.HELA_BODY) && inAsgard) {
            factor *= HelaConfig.ASGARD_FACTOR.get().floatValue();
        }
        boolean blade = source.is(ModDamageTypes.NECROSWORD) || source.is(ModDamageTypes.BLADE_WALL);
        if (blade && HelaAbilities.formOn(data)) {
            factor *= HelaAbilities.FORM_FACTOR;
        }
        return factor;
    }

    private HelaEvents() {
    }
}
