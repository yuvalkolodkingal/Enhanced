package com.kolodkingal.enhanced.platform.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Connects the platform to NeoForge's mod bus: the attachments' register, the messages of every declared channel, and
 * the entity attributes given through {@code Registrars}. Called first by the mod's NeoForge entry point.
 */
public final class NeoSetup {
    public static void init(IEventBus modBus) {
        NeoPlatform platform = NeoPlatform.get();
        platform.neoAttach().register.register(modBus);
        modBus.addListener(RegisterPayloadHandlersEvent.class, platform.neoNet()::register);
        modBus.addListener(EntityAttributeCreationEvent.class, platform.neoRegistrars()::createAttributes);
        //#if >=1.21.4
        //$$ modBus.addListener(RegisterPayloadHandlersEvent.class, com.kolodkingal.enhanced.compat.AttachSync::registerPayload);
        //$$ net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.kolodkingal.enhanced.compat.AttachSync.class);
        //#endif
    }

    private NeoSetup() {
    }
}
