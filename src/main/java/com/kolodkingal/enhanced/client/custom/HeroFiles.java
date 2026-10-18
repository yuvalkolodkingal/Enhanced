package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.HeroLooks;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * The Hero Creator's files on this computer (3.3): heroes exported to {@code enhanced/heroes/<name>.json} in the game
 * folder (and to the clipboard), imported back from there or from the clipboard, and skins: the PNGs of
 * {@code enhanced/skins/}, a PNG picked with the system's file dialog or dropped on the window, and a copy of a hero's
 * skin written there to be painted over.
 */
public final class HeroFiles {
    /** The longest list of files the screen shows. */
    public static final int MAX_LISTED = 40;

    public static Path heroesDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("enhanced").resolve("heroes");
    }

    public static Path skinsDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("enhanced").resolve("skins");
    }

    /** Writes the hero to {@code enhanced/heroes/<name>.json} (replacing one of the same name) and returns the file. */
    public static Path export(CustomHero hero) throws IOException {
        Path dir = heroesDir();
        Files.createDirectories(dir);
        Path file = dir.resolve(CustomHeroJson.fileName(hero.name()) + ".json");
        Path tmp = dir.resolve(file.getFileName() + ".tmp");
        Files.writeString(tmp, CustomHeroJson.write(hero), StandardCharsets.UTF_8);
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return file;
    }

    /** The heroes saved in {@code enhanced/heroes/}, newest first. */
    public static List<Path> heroes() {
        return list(heroesDir(), ".json");
    }

    /** The skins in {@code enhanced/skins/}, newest first. */
    public static List<Path> skins() {
        return list(skinsDir(), ".png");
    }

    private static List<Path> list(Path dir, String suffix) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(suffix) && Files.isRegularFile(p))
                    .sorted(Comparator.comparingLong(HeroFiles::modified).reversed()).limit(MAX_LISTED).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /** Reads a hero file (never more than the reader takes). */
    public static CustomHeroJson.Read read(Path file) {
        try {
            if (Files.size(file) > CustomHeroJson.MAX_CHARS * 2L) {
                return new CustomHeroJson.Read(null, List.of("message.enhanced.hero_creator.import.too_big"));
            }
            return CustomHeroJson.read(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return new CustomHeroJson.Read(null, List.of("message.enhanced.hero_creator.import.unreadable"));
        }
    }

    /** The bytes of a skin file, or null if it is missing or much too big to be one. */
    @Nullable
    public static byte[] readSkin(Path file) {
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > CustomHero.MAX_SKIN_WIRE * 4L) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            return null;
        }
    }

    public static void copy(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    public static String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    /**
     * Writes the skin of one of the mod's looks to {@code enhanced/skins/} (to be painted over in a drawing program and then
     * picked or dropped back) and returns the file.
     */
    public static Optional<Path> copyOfLook(HeroLooks.Look look) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(look.skin());
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try (InputStream in = resource.get().open()) {
            Path dir = skinsDir();
            Files.createDirectories(dir);
            Path file = dir.resolve(look.power().getSerializedName() + "_" + look.id() + ".png");
            Files.write(file, in.readAllBytes());
            return Optional.of(file);
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Could not copy the skin {}", look.skin(), e);
            return Optional.empty();
        }
    }

    /** Opens a folder in the system's file browser (made first if it is not there). */
    public static void open(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // (opened anyway: the browser says it is missing)
        }
        net.minecraft.Util.getPlatform().openFile(dir.toFile());
    }

    /**
     * The system's "open file" dialog for a PNG, on a thread of its own (it blocks until the player chooses); the future
     * gives the file, or nothing if he cancelled or the system has no dialog.
     */
    public static CompletableFuture<Optional<Path>> pickSkin(String title) {
        return CompletableFuture.supplyAsync(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png"));
                filters.flip();
                String chosen = TinyFileDialogs.tinyfd_openFileDialog(title, skinsDir().toAbsolutePath() + java.io.File.separator, filters,
                        "PNG", false);
                return chosen == null ? Optional.<Path>empty() : Optional.of(Path.of(chosen));
            } catch (Throwable e) {
                Enhanced.LOGGER.warn("No file dialog on this system", e);
                return Optional.<Path>empty();
            }
        });
    }

    /** The PNG files among dropped files. */
    public static List<Path> pngs(List<Path> dropped) {
        List<Path> out = new ArrayList<>();
        for (Path p : dropped) {
            if (p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
                out.add(p);
            }
        }
        return out;
    }

    private HeroFiles() {
    }
}
