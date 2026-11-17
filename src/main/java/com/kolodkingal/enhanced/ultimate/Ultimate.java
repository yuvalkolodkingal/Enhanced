package com.kolodkingal.enhanced.ultimate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.UltimateConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ultimate Mode (3.17): one switch for every hero's ultimate version. It replaced the separate switches of before
 * (3.16's {@code vision.solar_jewel.enabled}): with it on, every hero can awaken an ultimate form ({@link UltimateForm},
 * the Ultimate Form key), Source Code Vision runs on the Solar Jewel, and Billy is the Demiurge.
 *
 * <p>Where it is decided: {@code [ultimate] enabled} in the server config, unless the world has its own setting, made by
 * {@code /enhanced ultimate on|off} and kept in {@code <world>/serverconfig/enhanced-ultimate.json} (like the SMP
 * balance's file). The server sends it to every player ({@link UltimateSyncPayload}) so the client reads it as the
 * server does.
 */
public final class Ultimate {
    private Ultimate() {
    }

    /** The world's own setting (null: none, the config's), or the server's on a client. */
    @Nullable
    private static volatile Boolean world;
    /** Where this world's setting is kept (null: no server here). */
    @Nullable
    private static Path file;

    /** Whether Ultimate Mode is on now (both sides). */
    public static boolean on() {
        Boolean w = world;
        if (w != null) {
            return w;
        }
        return EnhancedConfig.SPEC.isLoaded() && UltimateConfig.ENABLED.get();
    }

    /** The world's own setting, or null when the config decides. */
    @Nullable
    public static Boolean worldSetting() {
        return world;
    }

    /** Sets the world's own setting (null: back to the config's); a server saves it with {@link #save}. */
    public static void set(@Nullable Boolean on) {
        world = on;
    }

    /** Back to the default (the server stopped, or the client left its server). */
    public static void reset() {
        file = null;
        world = null;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Reads the world's setting (the server is starting); no file: the config decides. */
    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("enhanced-ultimate.json");
        world = null;
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (json != null && json.has("enabled")) {
                world = json.get("enabled").getAsBoolean();
            }
        } catch (IOException | RuntimeException e) {
            Enhanced.LOGGER.error("Could not read {}; Ultimate Mode follows the server config", file, e);
        }
    }

    /** Writes the world's setting (removes the file when the config decides). */
    static void save() {
        if (file == null) {
            return;
        }
        try {
            if (world == null) {
                Files.deleteIfExists(file);
                return;
            }
            JsonObject json = new JsonObject();
            json.addProperty("enabled", world);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not write {}", file, e);
        }
    }
}
