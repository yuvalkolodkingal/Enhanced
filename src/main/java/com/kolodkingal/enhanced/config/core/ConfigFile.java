package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * One config file of the mod (port P0, lane L7): its spec, its name, and where it was read from. Reading, correcting,
 * backing up and writing work as NeoForge's {@code ConfigTracker} did for the mod until 3.8:
 * <ul>
 *   <li>a missing file is made: copied from {@code defaultconfigs/} if there is one there, else written with every
 *   default;</li>
 *   <li>a file that is not correct (a value missing, unknown, of the wrong type or out of its range, or a comment
 *   that is not the spec's) is backed up, corrected (each changed value with a warning in the log) and written
 *   back;</li>
 *   <li>a file that cannot be read (not TOML) is backed up and made again with every default;</li>
 *   <li>backups are {@code <name>-1.toml.bak} (the newest) to {@code <name>-5.toml.bak}.</li>
 * </ul>
 * A file read from disk is watched ({@link ConfigWatcher}): when it changes it is read again.
 */
public final class ConfigFile {
    /** Which side a file belongs to: the client's own settings, or the world's (synced to clients). */
    public enum Type {
        CLIENT, SERVER;

        public String extension() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int MAX_BACKUPS = 5;

    private final Type type;
    private final ConfigSpec spec;
    private final String fileName;
    /** The file the values were read from (null: not loaded, or values with no file such as a synced config). */
    @Nullable
    private Path path;
    /** The text last read from or written to {@link #path} (a change by someone else differs from it). */
    @Nullable
    private String lastText;

    /** A config file; {@link Configs#register} makes the mod's (one made here is not read by the game: tests). */
    public ConfigFile(Type type, ConfigSpec spec, String fileName) {
        this.type = type;
        this.spec = spec;
        this.fileName = fileName;
    }

    public Type type() {
        return type;
    }

    public ConfigSpec spec() {
        return spec;
    }

    /** Its file name, e.g. {@code enhanced-server.toml}. */
    public String fileName() {
        return fileName;
    }

    /** The file its values were read from, or null (not loaded, or synced from a server). */
    @Nullable
    public synchronized Path path() {
        return path;
    }

    // ---- loading ----

    /**
     * Reads the file from {@code overrideDir} if it is there, else from {@code configDir}, and puts its values in use;
     * then watches it.
     */
    public synchronized void open(Path configDir, @Nullable Path overrideDir) {
        if (spec.isLoaded() && path != null) {
            Enhanced.LOGGER.warn("Opening a config that was already loaded at path {}", fileName);
        }
        Path base = configDir;
        if (overrideDir != null && Files.exists(overrideDir.resolve(fileName))) {
            Enhanced.LOGGER.info("Found config file override in path {}", overrideDir.resolve(fileName));
            base = overrideDir;
        }
        Path file = base.resolve(fileName);
        load(file);
        Enhanced.LOGGER.debug("Loaded TOML config file {}", file);
        ConfigWatcher.watch(file, changed -> reloadIfChanged(file));
    }

    /** Reads {@code file} (making, correcting or remaking it as needed) and puts its values in use. */
    public synchronized void load(Path file) {
        Toml.Table table = read(file, spec, fileName);
        path = file;
        lastText = readQuietly(file);
        spec.accept(table, this);
    }

    /**
     * The values of {@code file} for {@code spec}, the file made, corrected or remade on the way (see the class).
     * Throws only when even a new default file cannot be written.
     */
    public static Toml.Table read(Path file, ConfigSpec spec, String fileName) {
        try {
            Toml.Table table = parse(file);
            if (!spec.isCorrect(table)) {
                Enhanced.LOGGER.warn("Configuration file {} is not correct. Correcting", file);
                backUp(file);
                spec.correct(table, ConfigFile::warnCorrected, null);
                write(file, table);
            }
            return table;
        } catch (NoSuchFileException e) {
            try {
                makeDefault(file, spec, fileName);
                return parse(file);
            } catch (IOException | Toml.ParseException failure) {
                throw new RuntimeException("Failed to create default config file " + fileName, failure);
            }
        } catch (IOException | Toml.ParseException e) {
            Enhanced.LOGGER.warn("Failed to load config {}: {}. Attempting to recreate", fileName, e.toString());
            try {
                backUp(file);
                Files.delete(file);
                makeDefault(file, spec, fileName);
                return parse(file);
            } catch (Throwable failure) {
                e.addSuppressed(failure);
                throw new RuntimeException("Failed to recreate config file " + fileName, e);
            }
        }
    }

    /** Logs a value a correction changed (the path is only valid during the call). */
    static void warnCorrected(ConfigSpec.CorrectionAction action, List<String> path, @Nullable Object incorrect,
                              @Nullable Object corrected) {
        String key = String.join(".", path);
        switch (action) {
            case ADD -> Enhanced.LOGGER.warn("Config key {} was missing: added with its default, {}", key, corrected);
            case REMOVE -> Enhanced.LOGGER.warn("Config key {} is not an option: removed (it was {})", key, incorrect);
            default -> Enhanced.LOGGER.warn("Incorrect key {} was corrected from {} to {}", key, incorrect, corrected);
        }
    }

    /** Puts values received from the server in use (no file: they are only the server's while connected). */
    public synchronized void acceptSynced(String text) throws Toml.ParseException {
        if (spec.isLoaded()) {
            Enhanced.LOGGER.debug("Overwriting the loaded config {} with the server's", fileName);
        }
        Toml.Table table = Toml.parse(text);
        if (path != null) {
            ConfigWatcher.unwatch(path);
        }
        path = null;
        lastText = null;
        spec.accept(table, null);
    }

    /** Puts every default in use, with no file (a server that sends none). */
    public synchronized void acceptDefaults() {
        path = null;
        lastText = null;
        spec.accept(spec.defaults(), null);
    }

    /** Stops using the values (and watching the file). */
    public synchronized void unload() {
        if (!spec.isLoaded()) {
            return;
        }
        if (path != null) {
            ConfigWatcher.unwatch(path);
        }
        path = null;
        lastText = null;
        spec.accept(null, null);
    }

    /** Writes {@code table} to the file it was read from (nothing for values with no file). */
    synchronized void save(Toml.Table table) {
        if (path == null) {
            return;
        }
        String text = Toml.write(table);
        try {
            writeText(path, text);
            lastText = text;
        } catch (IOException e) {
            Enhanced.LOGGER.error("Failed to write config file {}", path, e);
        }
    }

    /** The file as it is now on disk: what a client is sent ({@code null}: values with no file). */
    @Nullable
    public synchronized String fileText() throws IOException {
        return path == null ? null : Files.readString(path, StandardCharsets.UTF_8);
    }

    /** The watcher's call: reads the file again if it is not what was last read or written. */
    private synchronized void reloadIfChanged(Path changed) {
        if (path == null || !path.equals(changed) || !spec.isLoaded()) {
            return;
        }
        String now = readQuietly(changed);
        if (now != null && now.equals(lastText)) {
            return;
        }
        Enhanced.LOGGER.info("Config file {} changed, re-loading", fileName);
        load(changed);
    }

    // ---- files ----

    private static Toml.Table parse(Path file) throws IOException, Toml.ParseException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        // (a byte order mark, which some Windows editors write, is not part of the file's TOML)
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        return Toml.parse(text);
    }

    @Nullable
    private static String readQuietly(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static void makeDefault(Path file, ConfigSpec spec, String fileName) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path defaults = defaultConfigs(file).resolve(fileName);
        if (Files.exists(defaults)) {
            Enhanced.LOGGER.info("Loading default config file from path {}", defaults);
            Files.copy(defaults, file);
        } else {
            write(file, spec.defaults());
        }
    }

    /** {@code defaultconfigs/} of the game folder: the files a modpack ships as the first config of a new game. */
    private static Path defaultConfigs(Path file) {
        return Configs.gameDir().resolve("defaultconfigs");
    }

    static void write(Path file, Toml.Table table) throws IOException {
        writeText(file, Toml.write(table));
    }

    /** Writes a file whole or not at all (a new file moved over the old one). */
    static void writeText(Path file, String text) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path temp = dir.resolve(file.getFileName() + ".new.tmp");
        Files.writeString(temp, text, StandardCharsets.UTF_8);
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Copies {@code file} to {@code <name>-1.toml.bak}, the older backups moved up by one (the fifth dropped). */
    static void backUp(Path file) {
        Path dir = file.toAbsolutePath().getParent();
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        String extension = (dot < 0 ? "" : name.substring(dot + 1)) + ".bak";
        try {
            for (int i = MAX_BACKUPS; i > 0; i--) {
                Path old = dir.resolve(base + "-" + i + "." + extension);
                if (Files.exists(old)) {
                    if (i >= MAX_BACKUPS) {
                        Files.delete(old);
                    } else {
                        Files.move(old, dir.resolve(base + "-" + (i + 1) + "." + extension));
                    }
                }
            }
            Files.copy(file, dir.resolve(base + "-1." + extension));
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Failed to back up config file {}", file, e);
        }
    }
}
