package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * What Jim Hammond and Source Code Vision kill drops cooked, like a Fire Aspect kill (3.17): the victim of their fire (every
 * {@code solar_beam} blow: Jim's beam, fireballs and Nova Flame with its explosion, Source Code Vision's Solar Beam and charged
 * beam) and of Jim's own blows is set alight as the blow lands, so if it dies the game's own loot turns its meat to cooked meat
 * (the loot tables' "on fire" rule). Nothing else changes: nobody else's kills, and not Vision's other looks.
 *
 * <p>Jim Hammond's burning body (3.17): while he flies in flames, whoever strikes him in melee is set alight. The blow still
 * hurts him (nothing is cancelled or reduced).
 */
@Subscriber
public final class JimEvents {
    private JimEvents() {
    }

    /** A blow of fire (or of Jim's hand) lights its victim, so what it kills drops cooked. */
    private static void cookWhatTheyKill(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        boolean fire = source.is(com.kolodkingal.enhanced.registry.ModDamageTypes.SOLAR_BEAM);
        boolean jimsHand = source.getEntity() instanceof ServerPlayer shooter && source.getDirectEntity() == shooter
                && Powers.get(shooter).power() == com.kolodkingal.enhanced.power.Power.JIM_HAMMOND;
        if ((fire || jimsHand) && event.getEntity() != source.getEntity()) {
            event.getEntity().igniteForSeconds(1.0F);
        }
    }

    @Subscribe
    public static void onHurt(LivingIncomingDamageEvent event) {
        cookWhatTheyKill(event);
        if (!(event.getEntity() instanceof ServerPlayer jim) || !Powers.get(jim).has(HeroAbility.JIM_FLAME_BODY)
                || !Powers.get(jim).is(Live.JIM_FLAMING)) {
            return;
        }
        // a blow struck by hand or weapon: the attacker is also the direct cause (an arrow or a beam is not melee)
        Entity direct = event.getSource().getDirectEntity();
        Entity cause = event.getSource().getEntity();
        if (direct instanceof LivingEntity attacker && direct == cause && attacker != jim) {
            BeamFire.ignite(attacker, JimConfig.ATTACKER_BURN_SECONDS.get());
        }
    }
}
