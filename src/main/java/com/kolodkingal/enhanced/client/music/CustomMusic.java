package com.kolodkingal.enhanced.client.music;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
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
 * Lets a player swap Quicksilver's song: any .mp3, .ogg or .wav file placed in {@code config/enhanced/music} is played
 * instead of the built-in "Sweet Dreams (Are Made of This)". Nothing is downloaded.
 */
public final class CustomMusic {
    private static final String README = """
            Quicksilver's Walkman
            =====================

            Quicksilver's Walkman plays "Sweet Dreams (Are Made of This)" whenever he runs
            in Quicksilver Time. To play a different song, put a song file here (.mp3, .ogg
            or .wav). If there is more than one, the first one in alphabetical order is
            used. Changes are picked up the next time the music starts. Remove the file to
            go back to Sweet Dreams.
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
            // Kept up to date: it is the mod's own file, and older versions described the song differently.
            if (!Files.exists(readme) || !Files.readString(readme, StandardCharsets.UTF_8).equals(README)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Could not create {}", folder(), e);
        }
    }

    /** Opens the song the player put in config/enhanced/music, if any. Empty means: play the built-in song. */
    public static Optional<AudioStream> openSong(boolean looping) throws IOException {
        Optional<Path> file = find();
        if (file.isEmpty()) {
            return Optional.empty();
        }
        Enhanced.LOGGER.info("Quicksilver's Walkman plays {}", file.get().getFileName());
        return Optional.of(open(typeOf(file.get()), new BufferedInputStream(Files.newInputStream(file.get())), looping));
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
