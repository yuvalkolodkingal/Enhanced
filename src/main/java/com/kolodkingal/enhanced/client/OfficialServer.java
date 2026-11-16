package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.Locale;

/**
 * (3.16.1) The official server, put on the player's Multiplayer list the first time the game reaches its title screen
 * with the mod: appended at the end, unless an entry with its address is already there, and never touching the player's
 * other servers. A flag in the client config ({@code official_server.addedOfficialServer}) makes it once only, so an entry
 * the player deletes never comes back; {@code officialServer = false} turns it off. Client only (a client subscriber).
 */
@Subscriber(side = Side.CLIENT)
public final class OfficialServer {
    public static final String NAME = "Enhanced Superhero SMP";
    public static final String ADDRESS = "superheros.exaroton.me";

    private static boolean checked;

    private OfficialServer() {
    }

    @Subscribe
    public static void onTick(ClientTickEvent.Post event) {
        if (checked) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TitleScreen) || !EnhancedClientConfig.SPEC.isLoaded()) {
            return;
        }
        checked = true;
        if (!EnhancedClientConfig.OFFICIAL_SERVER.get() || EnhancedClientConfig.ADDED_OFFICIAL_SERVER.get()) {
            return;
        }
        try {
            ensure(new ServerList(mc));
            EnhancedClientConfig.ADDED_OFFICIAL_SERVER.set(true);
            EnhancedClientConfig.ADDED_OFFICIAL_SERVER.save();
        } catch (RuntimeException e) {
            Enhanced.LOGGER.warn("Could not add the official server to the server list", e);
        }
    }

    /** Loads the list, appends the official server unless its address is there, and saves. True when it was added. */
    public static boolean ensure(ServerList list) {
        list.load();
        for (int i = 0; i < list.size(); i++) {
            ServerData entry = list.get(i);
            if (entry != null && entry.ip != null && entry.ip.trim().toLowerCase(Locale.ROOT).equals(ADDRESS)) {
                Enhanced.LOGGER.info("The official server is on the server list already");
                return false;
            }
        }
        list.add(new ServerData(NAME, ADDRESS, ServerData.Type.OTHER), false);
        list.save();
        Enhanced.LOGGER.info("Added the official server ({}) to the server list", ADDRESS);
        return true;
    }
}
