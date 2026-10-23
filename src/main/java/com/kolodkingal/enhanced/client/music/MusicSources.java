package com.kolodkingal.enhanced.client.music;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * What plays for a piece of music the player or the world can replace (3.5), in this order: the player's own song in
 * config/enhanced/music ({@link CustomMusic}), then the world's ({@link ServerMusicClient}, unless he ignores it), then the
 * mod's own sound.
 */
public final class MusicSources {

    /** Where a song comes from. */
    public enum Kind {
        PERSONAL, SERVER, BUILT_IN
    }

    /**
     * The song chosen for a track: where it comes from, its file (null for the built-in one) and a key that is the same for
     * the same song (a loop goes on undisturbed when the next part of the Road plays the same file).
     */
    public record Source(String track, Kind kind, @Nullable Path file, String key) {
        public String describe() {
            return switch (kind) {
                case PERSONAL -> "your own song " + file.getFileName();
                case SERVER -> "the world's song " + key;
                case BUILT_IN -> "the built-in " + key;
            };
        }
    }

    /** The song for a track now, the built-in one being {@code builtIn}. */
    public static Source resolve(String track, SoundEvent builtIn) {
        Optional<Path> own = CustomMusic.find(track);
        if (own.isPresent()) {
            return new Source(track, Kind.PERSONAL, own.get(), "personal:" + own.get().getFileName());
        }
        Optional<ServerMusicClient.ServerSong> world = ServerMusicClient.songFor(track);
        if (world.isPresent()) {
            return new Source(track, Kind.SERVER, world.get().file(), world.get().name() + ".ogg (" + world.get().hash().substring(0, 12) + ")");
        }
        return new Source(track, Kind.BUILT_IN, null, builtIn.getLocation().toString());
    }

    /**
     * A looping piece of music, heard alike everywhere (not placed in the world), that plays its {@link Source}: a file of the
     * player's or the world's, or its own sound event's file. A file that cannot be played falls back to the built-in song.
     * The sound event must be a streamed one (its file is read as it plays).
     */
    public abstract static class Loop extends AbstractTickableSoundInstance {
        protected final Source source;

        protected Loop(SoundEvent sound, SoundSource category, Source source) {
            super(sound, category, SoundInstance.createUnseededRandom());
            this.source = source;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        public Source source() {
            return source;
        }

        // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
            if (source.file() == null) {
                Enhanced.LOGGER.info("Music {}: plays {}", source.track(), source.describe());
                return buffers.getStream(sound.getPath(), looping);
            }
            return CompletableFuture.supplyAsync(() -> {
                try {
                    AudioStream stream = CustomMusic.open(source.file(), looping);
                    Enhanced.LOGGER.info("Music {}: plays {}", source.track(), source.describe());
                    return stream;
                } catch (IOException | RuntimeException e) {
                    Enhanced.LOGGER.warn("Music {}: could not play {}; playing the built-in song instead", source.track(), source.describe(), e);
                }
                return buffers.getStream(sound.getPath(), looping).join();
            }, Util.nonCriticalIoPool());
        }
    }

    private MusicSources() {
    }
}
