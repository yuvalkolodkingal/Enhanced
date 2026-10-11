package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * The mod-bus side of Ultron: when the mod loads, his lane is registered with the boss framework, so that the Scepter's
 * ritual and {@code /enhanced boss start ultron} can wake him.
 */
@EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class UltronSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BossFights.register(BossKind.ULTRON, new UltronLane()));
    }

    private UltronSetup() {
    }
}
