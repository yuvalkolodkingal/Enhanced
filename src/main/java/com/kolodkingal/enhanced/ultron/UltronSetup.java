package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.platform.event.FMLCommonSetupEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;

/**
 * The mod-bus side of Ultron: when the mod loads, his lane is registered with the boss framework, so that the Scepter's
 * ritual and {@code /enhanced boss start ultron} can wake him, the Sokovia lift is installed as the way his island
 * rises (until then it would only be the Tremor), and the hero's drones and the bolts they fire are made known to the
 * rest of the mod (the orphan sweep leaves them alone, an area attack of his spares them).
 */
@Subscriber
public final class UltronSetup {

    @Subscribe
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BossFights.register(BossKind.ULTRON, new UltronLane());
            LiftHooks.install(new SokoviaLift());
            BossFights.exemptFromSweep(UltronDrones::isDroneBolt);
            AreaTargets.addAllyRule(UltronDrones::isHisDrone);
        });
    }

    private UltronSetup() {
    }
}
