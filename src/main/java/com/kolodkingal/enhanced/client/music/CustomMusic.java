package com.kolodkingal.enhanced.client.music;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.music.MusicTracks;
import com.kolodkingal.enhanced.platform.Platform;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The player's own music: song files (.mp3, .ogg or .wav) placed in {@code config/enhanced/music} play instead of the
 * mod's, for him only. Since 3.5 each piece of music has a name ({@code quicksilver.*}, {@code speed.*}, {@code
 * witches_road.*}, {@code witches_road_<part>.*}); any other song there is Quicksilver's, as before 3.5 (see
 * {@link MusicTracks#resolvePersonal}). Nothing is downloaded.
 */
public final class CustomMusic {
    private static final String README = """
            Your own music (Enhanced)
            =========================

            Songs put here play instead of the mod's music, for you only (.mp3, .ogg or .wav).
            Name each file after the music it replaces:

              quicksilver.mp3          Quicksilver's Walkman song (in Quicksilver Time)
              speed.mp3                Speed's Walkman song (Super Speed or his slow time)
              witches_road.mp3         the Witches' Road, all of it
              witches_road_<part>.mp3  one part of the Road only: entrance, parkour,
                                       structure_copy, ice_boat, choice or ending

            Any other song file here (say "my song.mp3") is Quicksilver's, as before 3.5;
            if there is more than one, the first in alphabetical order is used.

            Your songs win over the songs of a server or world (which it sends to you and
            keeps in config/enhanced/music_cache). Changes are picked up the next time the
            music starts. Remove a file to go back to the usual music.
            """;

    public static Path folder() {
        return Platform.get().configDir().resolve(Enhanced.MODID).resolve("music");
    }

    /** Creates the folder (with a short explanation) so players can find it. */
    public static void prepareFolder() {
        try {
            Path folder = folder();
            Files.createDirectories(folder);
            Path readme = folder.resolve("README.txt");
            // Kept up to date: it is the mod's own file, and older versions described the songs differently.
            if (!Files.exists(readme) || !Files.readString(readme, StandardCharsets.UTF_8).equals(README)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Could not create {}", folder(), e);
        }
    }

    /** The player's own song for a track, if he put one in the folder. */
    public static Optional<Path> find(String track) {
        Path folder = folder();
        if (!Files.isDirectory(folder)) {
            return Optional.empty();
        }
        List<String> names;
        try (Stream<Path> files = Files.list(folder)) {
            names = files.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).toList();
        } catch (IOException e) {
            return Optional.empty();
        }
        return MusicTracks.resolvePersonal(track, names).map(folder::resolve);
    }

    /** Opens a song file (by its extension) as a looping (or one-shot) stream the sound engine can play. */
    public static AudioStream open(Path file, boolean looping) throws IOException {
        String type = MusicTracks.extension(file.getFileName().toString());
        LoopingAudioStream.AudioStreamProvider provider = switch (type) {
            case "mp3" -> Mp3AudioStream::new;
            case "wav" -> PcmAudioStream::fromWav;
            case "ogg" -> JOrbisAudioStream::new;
            default -> throw new IOException("Unsupported audio file " + file.getFileName());
        };
        InputStream in = new BufferedInputStream(Files.newInputStream(file));
        try {
            return looping ? new LoopingAudioStream(provider, in) : provider.create(in);
        } catch (IOException | RuntimeException e) {
            // Not a song we can play: let go of the file (the caller falls back to the built-in song).
            in.close();
            throw e;
        }
    }

    private CustomMusic() {
    }
}
