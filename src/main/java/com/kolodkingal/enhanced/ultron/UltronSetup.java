package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * The mod-bus side of Ultron: when the mod loads, his lane is registered with the boss framework, so that the Scepter's
 * ritual and {@code /enhanced boss start ultron} can wake him, and the hero's drones and the bolts they fire are made
 * known to the rest of the mod (the orphan sweep leaves them alone, an area attack of his spares them).
 */
@EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class UltronSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BossFights.register(BossKind.ULTRON, new UltronLane());
            BossFights.exemptFromSweep(UltronDrones::isDroneBolt);
            AreaTargets.addAllyRule(UltronDrones::isHisDrone);
        });
    }

    private UltronSetup() {
    }
}
