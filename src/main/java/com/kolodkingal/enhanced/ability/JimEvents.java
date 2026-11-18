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
 * Jim Hammond's burning body (3.17): while he flies in flames, whoever strikes him in melee is set alight. The blow still
 * hurts him (nothing is cancelled or reduced).
 */
@Subscriber
public final class JimEvents {
    private JimEvents() {
    }

    @Subscribe
    public static void onHurt(LivingIncomingDamageEvent event) {
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
