package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.screen.ConfigScreen;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar;
import com.kolodkingal.enhanced.platform.neoforge.client.NeoClientRegistrar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * The client half of the mod's entry point (only loaded on a client): gives the Mods screen a Config button for the client
 * options {@link EnhancedClientConfig} (the lang keys {@code config.enhanced.*} and their tooltips are for it).
 */
/* Started by boot.EnhancedBoot on a client. */
public final class EnhancedClient {
    public EnhancedClient(ModContainer container, IEventBus modBus) {
        // Port P0: the platform's client registrar, and every client registration of the mod through it.
        ClientSetup.register(NeoClientRegistrar.install(container, modBus));
        //#if >=1.21.4
        //$$ modBus.addListener(com.kolodkingal.enhanced.compat.RenderBridge::register);
        //#endif
        // Port P0 L7: the mod's own settings screen (NeoForge's Mods list opens it).
        ClientRegistrar.get().configScreen(ConfigScreen::new);
    }
}
