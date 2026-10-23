package com.kolodkingal.enhanced.config.neoforge;

import com.kolodkingal.enhanced.config.core.ConfigSyncPayload;
import com.kolodkingal.enhanced.config.core.Configs;
import com.kolodkingal.enhanced.network.ModNetwork;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * The mod's config files on NeoForge (port P0, lane L7): when they are read, and how a joining client gets the
 * server's, at the moments NeoForge read and synced them for the mod until 3.8 (the server's files just before the
 * world loads, unloaded after the server stopped; sent while a client joins, in the configuration phase).
 *
 * <p>TODO(port seam): the platform has no common event for "the server is about to start" and no configuration-phase
 * message; until it does, these four listeners stay on NeoForge here (seam requests in the L7 notes). The rest of the
 * backend ({@code config/core}) is loader-neutral.
 */
public final class ConfigNeoForge {
    private ConfigNeoForge() {
    }

    /** Called once by the mod's NeoForge entry point. */
    public static void install(IEventBus modBus) {
        // TODO(port seam): ServerAboutToStartEvent (NeoForge read the server configs just before posting it).
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ServerAboutToStartEvent.class,
                event -> Configs.loadServerConfigs(event.getServer()));
        // (NeoForge dropped them after every listener had seen the server stop.)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ServerStoppedEvent.class,
                event -> Configs.unloadServerConfigs());
        // TODO(port seam): a configuration-phase message and task.
        modBus.addListener(RegisterPayloadHandlersEvent.class, ConfigNeoForge::registerPayload);
        modBus.addListener(RegisterConfigurationTasksEvent.class, ConfigNeoForge::registerTask);
    }

    private static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar(ModNetwork.PROTOCOL_VERSION).configurationToClient(ConfigSyncPayload.TYPE,
                ConfigSyncPayload.STREAM_CODEC,
                (payload, context) -> Configs.receiveSynced(payload, context.connection().isMemoryConnection()));
    }

    private static void registerTask(RegisterConfigurationTasksEvent event) {
        ServerConfigurationPacketListener listener = event.getListener();
        if (listener.hasChannel(ConfigSyncPayload.TYPE)) {
            event.register(new SyncTask(listener));
        }
    }

    /** Sends the server's config files to the joining client. */
    private record SyncTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
        private static final ConfigurationTask.Type TYPE =
                new ConfigurationTask.Type(ConfigSyncPayload.TYPE.id().toString());

        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            Configs.syncPayloads().forEach(sender);
            listener.finishCurrentTask(type());
        }

        @Override
        public ConfigurationTask.Type type() {
            return TYPE;
        }
    }
}
