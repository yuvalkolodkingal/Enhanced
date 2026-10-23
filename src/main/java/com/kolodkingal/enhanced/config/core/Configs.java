package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The mod's config files and when they are read (port P0, lane L7), as NeoForge did it for the mod until 3.8:
 * <ul>
 *   <li>{@link ConfigFile.Type#CLIENT}: {@code config/enhanced-client.toml}, read while the mod starts on a client
 *   ({@link #loadClientConfigs()}; a dedicated server never reads it);</li>
 *   <li>{@link ConfigFile.Type#SERVER}: {@code config/enhanced-server.toml}, or the world's own copy in
 *   {@code <world>/serverconfig/} when there is one, read as a server (dedicated, integrated or GameTest) is about to
 *   start ({@link #loadServerConfigs}) and dropped when it has stopped ({@link #unloadServerConfigs()}); a client
 *   connected to another server uses that server's file instead ({@link ConfigSyncPayload}).</li>
 * </ul>
 * The loader calls these at those moments ({@code ConfigNeoForge} on NeoForge).
 */
public final class Configs {
    private static final List<ConfigFile> CONFIGS = new CopyOnWriteArrayList<>();
    /** The world's folder for server configs that win over {@code config/} (NeoForge's name). */
    public static final LevelResource SERVER_CONFIG = new LevelResource("serverconfig");
    /** The text NeoForge writes into {@code <world>/serverconfig/}: kept, as that folder still works the same. */
    private static final String SERVER_CONFIG_README = "Any server configs put in this folder will override the "
            + "corresponding server config from <instance path>/config/<config path>.\nIf the config being transferred "
            + "is in a subfolder of the base config folder make sure to include that folder here in the path to the "
            + "file you are overwriting.\nFor example if you are overwriting a config with the path <instance path>/config/"
            + "ExampleMod/config-server.toml, you would need to put it in serverconfig/ExampleMod/config-server.toml\n";

    private Configs() {
    }

    /** Registers a config file of the mod, named {@code enhanced-<type>.toml}. */
    public static ConfigFile register(ConfigFile.Type type, ConfigSpec spec) {
        return register(type, spec, Enhanced.MODID + "-" + type.extension() + ".toml");
    }

    public static ConfigFile register(ConfigFile.Type type, ConfigSpec spec, String fileName) {
        for (ConfigFile file : CONFIGS) {
            if (file.fileName().equals(fileName)) {
                throw new IllegalStateException("Config file " + fileName + " is registered twice");
            }
        }
        ConfigFile file = new ConfigFile(type, spec, fileName);
        CONFIGS.add(file);
        return file;
    }

    /** The registered files of a type, in the order they were registered. */
    public static List<ConfigFile> of(ConfigFile.Type type) {
        List<ConfigFile> files = new ArrayList<>();
        for (ConfigFile file : CONFIGS) {
            if (file.type() == type) {
                files.add(file);
            }
        }
        return files;
    }

    /** The registered file of {@code spec}, or null. */
    @Nullable
    public static ConfigFile of(ConfigSpec spec) {
        for (ConfigFile file : CONFIGS) {
            if (file.spec() == spec) {
                return file;
            }
        }
        return null;
    }

    @Nullable
    public static ConfigFile byFileName(String fileName) {
        for (ConfigFile file : CONFIGS) {
            if (file.fileName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }

    /** {@code config/} of the game folder. */
    public static Path configDir() {
        return Platform.get().configDir();
    }

    /** The game folder (the parent of {@code config/}). */
    static Path gameDir() {
        Path parent = configDir().toAbsolutePath().getParent();
        return parent == null ? configDir() : parent;
    }

    // ---- when they are read ----

    /** Reads the client's files (on a client, while the mod starts). */
    public static void loadClientConfigs() {
        for (ConfigFile file : of(ConfigFile.Type.CLIENT)) {
            file.open(configDir(), null);
        }
    }

    /** Reads the server's files, the world's own ({@code <world>/serverconfig/}) first, before the world loads. */
    public static void loadServerConfigs(MinecraftServer server) {
        Path override = serverConfigDir(server);
        for (ConfigFile file : of(ConfigFile.Type.SERVER)) {
            file.open(configDir(), override);
        }
    }

    /** Drops the server's values (the server has stopped). */
    public static void unloadServerConfigs() {
        for (ConfigFile file : of(ConfigFile.Type.SERVER)) {
            file.unload();
        }
    }

    /** {@code <world>/serverconfig/}, made with its readme if missing. */
    public static Path serverConfigDir(MinecraftServer server) {
        Path dir = server.getWorldPath(SERVER_CONFIG);
        try {
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
            }
            Path readme = dir.resolve("readme.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, SERVER_CONFIG_README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dir;
    }

    // ---- the server's files on clients ----

    /** What a joining client is sent: each server file as it is on disk. */
    public static List<ConfigSyncPayload> syncPayloads() {
        List<ConfigSyncPayload> payloads = new ArrayList<>();
        for (ConfigFile file : of(ConfigFile.Type.SERVER)) {
            try {
                String text = file.fileText();
                if (text != null) {
                    payloads.add(new ConfigSyncPayload(file.fileName(), text));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read config file " + file.fileName() + " for a client", e);
            }
        }
        return payloads;
    }

    /**
     * A client received a server file: its values are used while connected. Ignored when the server is this
     * client's own (its values are the very same objects).
     */
    public static void receiveSynced(ConfigSyncPayload payload, boolean localServer) {
        if (localServer) {
            return;
        }
        ConfigFile file = byFileName(payload.fileName());
        if (file == null || file.type() != ConfigFile.Type.SERVER) {
            Enhanced.LOGGER.warn("The server sent an unknown config file {}", payload.fileName());
            return;
        }
        try {
            file.acceptSynced(payload.contents());
            Enhanced.LOGGER.info("Using the server's config file {} while connected", payload.fileName());
        } catch (Toml.ParseException e) {
            Enhanced.LOGGER.error("The server's config file {} is not readable: {}", payload.fileName(),
                    e.getMessage());
        }
    }
}
