package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The client half of the mod's entry point (only loaded on a client): gives the Mods screen a Config button for the client
 * options {@link EnhancedClientConfig} (the lang keys {@code config.enhanced.*} and their tooltips are for it).
 */
@Mod(value = Enhanced.MODID, dist = Dist.CLIENT)
public final class EnhancedClient {
    public EnhancedClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
