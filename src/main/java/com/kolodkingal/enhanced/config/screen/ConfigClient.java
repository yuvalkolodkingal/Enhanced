package com.kolodkingal.enhanced.config.screen;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.core.ConfigEditPayload;
import com.kolodkingal.enhanced.config.core.ConfigEdits;
import com.kolodkingal.enhanced.config.core.ConfigFile;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.config.core.ConfigValuePayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.client.Minecraft;

/**
 * The client's side of the server settings changed in game (3.17): what the server sends ({@link ConfigValuePayload}),
 * and what an operator's screen sends it ({@link ConfigEditPayload}).
 */
public final class ConfigClient {
    private ConfigClient() {
    }

    /** Whether the server's settings on this client are another server's: changes go to it, they are not made here. */
    public static boolean remote(ConfigFile file) {
        Minecraft mc = Minecraft.getInstance();
        return file.type() == ConfigFile.Type.SERVER && mc.level != null && !mc.hasSingleplayerServer();
    }

    /** Whether this player may change the settings of the server he is on (the server checks again, whatever is said here). */
    public static boolean operator() {
        net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
        return player != null && player.hasPermissions(2);
    }

    /** An operator changed a value on his screen: tell the server. */
    static void send(ConfigSpec.ConfigValue<?> value, Object newValue) {
        Net.sendToServer(new ConfigEditPayload(ConfigEdits.key(value), ConfigEdits.text(newValue), false));
    }

    /** The server changed a setting (or asks for the settings screen). */
    @SuppressWarnings("unchecked")
    public static void receive(ConfigValuePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.key().isEmpty()) {
            mc.setScreen(new ConfigScreen(mc.screen));
            return;
        }
        if (mc.hasSingleplayerServer()) {
            return; // (a world of his own: the server's values are the very same objects)
        }
        ConfigSpec.ConfigValue<?> value = ConfigEdits.find(payload.key());
        if (value == null || !value.owner().isLoaded()) {
            return;
        }
        try {
            Object parsed = ConfigEdits.parse(value, payload.value());
            ((ConfigSpec.ConfigValue<Object>) value).set(parsed);
            value.owner().afterReload();
            if (mc.screen instanceof ConfigSectionScreen section) {
                section.serverChanged(value, parsed);
            }
        } catch (ConfigEdits.Refused e) {
            Enhanced.LOGGER.warn("The server sent a value this client does not accept: {} = {}", payload.key(), payload.value());
        }
    }
}
