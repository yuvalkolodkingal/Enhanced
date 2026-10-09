package com.kolodkingal.enhanced.client.music;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lets the player use their own song for Quicksilver's Walkman: any .mp3, .ogg or .wav file placed in
 * {@code config/enhanced/music} (or built into a personal copy of the mod from the git-ignored {@code personal/}
 * folder) is played instead of the built-in theme. Nothing is downloaded and no song ships with the public mod.
 */
public final class CustomMusic {
    private static final String README = """
            Quicksilver's Walkman
            =====================

            Put a song file here (.mp3, .ogg or .wav) and Quicksilver's Walkman plays it
            whenever he runs in Quicksilver Time, instead of the built-in tune.

            For example, copy your own copy of "Sweet Dreams (Are Made of This)" here as
            sweet_dreams.mp3. If there is more than one song, the first one in alphabetical
            order is used. Changes are picked up the next time the music starts.
            """;

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve(Enhanced.MODID).resolve("music");
    }

    /** Creates the folder (with a short explanation) so players can find it. */
    public static void prepareFolder() {
        try {
            Path folder = folder();
            Files.createDirectories(folder);
            Path readme = folder.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Could not create {}", folder(), e);
        }
    }

    /** Where a personal build keeps the song from the git-ignored personal/ folder. */
    private static final String PERSONAL = "sounds/quicksilver/personal";

    /**
     * Opens the player's own song, if there is one: first a file in config/enhanced/music, then a song built into a
     * personal copy of the mod. Empty means: use the built-in theme.
     */
    public static Optional<AudioStream> openSong(boolean looping) throws IOException {
        Optional<Path> file = find();
        if (file.isPresent()) {
            Enhanced.LOGGER.info("Quicksilver's Walkman plays {}", file.get().getFileName());
            return Optional.of(open(typeOf(file.get()), new BufferedInputStream(Files.newInputStream(file.get())), looping));
        }
        var resources = Minecraft.getInstance().getResourceManager()
                .listResources(PERSONAL, id -> id.getNamespace().equals(Enhanced.MODID) && typeOf(Path.of(id.getPath())) != null);
        Optional<ResourceLocation> bundled = resources.keySet().stream().min(Comparator.comparing(ResourceLocation::getPath));
        if (bundled.isPresent()) {
            InputStream in = new BufferedInputStream(resources.get(bundled.get()).open());
            return Optional.of(open(typeOf(Path.of(bundled.get().getPath())), in, looping));
        }
        return Optional.empty();
    }

    /** The player's song, if they put one in the folder. */
    public static Optional<Path> find() {
        Path folder = folder();
        if (!Files.isDirectory(folder)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> typeOf(p) != null)
                    .min(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Nullable
    private static String typeOf(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp3")) {
            return "mp3";
        }
        if (name.endsWith(".ogg")) {
            return "ogg";
        }
        if (name.endsWith(".wav")) {
            return "wav";
        }
        return null;
    }

    /** Opens a song as a looping (or one-shot) stream the sound engine can play. */
    private static AudioStream open(@Nullable String type, InputStream in, boolean looping) throws IOException {
        LoopingAudioStream.AudioStreamProvider provider = switch (type == null ? "" : type) {
            case "mp3" -> Mp3AudioStream::new;
            case "wav" -> PcmAudioStream::fromWav;
            case "ogg" -> JOrbisAudioStream::new;
            default -> throw new IOException("Unsupported audio file");
        };
        return looping ? new LoopingAudioStream(provider, in) : provider.create(in);
    }

    private CustomMusic() {
    }
}
