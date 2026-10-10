package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.client.music.CustomMusic;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Quicksilver's Walkman. While a Quicksilver with his Walkman on has Quicksilver Time on nearby (running or not, but
 * not in Super Speed, which slows nothing), "Sweet Dreams (Are Made of This)" plays for everyone close enough to be
 * caught in the slow motion, and fades out when he turns it off.
 */
public final class QuicksilverMusic {
    @Nullable
    private static Theme playing;
    /** Ticks the game's own music still waits after the song ended, like its usual pause between tracks. */
    private static int musicPause;
    /** Whether the current song was actually audible (not muted by the volume sliders, and its file loaded). */
    private static boolean heard;

    static void tick(Minecraft mc) {
        LocalPlayer listener = mc.player;
        boolean shouldPlay = listener != null && mc.level != null && speedsterNearby(listener);
        if (shouldPlay) {
            if (playing == null || !mc.getSoundManager().isActive(playing)) {
                playing = new Theme();
                mc.getSoundManager().play(playing);
            }
            playing.fadeIn();
            heard |= mc.getSoundManager().isActive(playing);
        } else if (playing != null) {
            playing.fadeOut();
            if (!mc.getSoundManager().isActive(playing)) {
                playing = null;
                if (heard) {
                    Music music = mc.getSituationalMusic();
                    musicPause = Mth.nextInt(RandomSource.create(), music.getMinDelay(), Math.max(music.getMinDelay(), music.getMaxDelay()));
                }
                heard = false;
            }
        } else if (musicPause > 0) {
            musicPause--;
        }
    }

    /**
     * Whether the game's music must stay quiet (see ClientEvents.onSelectMusic): always while the song plays, and for
     * the game's usual pause between tracks after it ends, cut short like the game does for music that wants to start
     * sooner (boss music comes back at once).
     */
    static boolean holdsGameMusic(Music music) {
        if (playing != null) {
            return true;
        }
        musicPause = Math.min(musicPause, music.getMaxDelay());
        return musicPause > 0;
    }

    private static boolean speedsterNearby(LocalPlayer listener) {
        double range = SlowMotion.radius() + 16;
        for (Player player : listener.level().players()) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.QUICKSILVER && data.slowMo() && data.walkman()
                    && player.distanceToSqr(listener) < range * range) {
                return true;
            }
        }
        return false;
    }

    static void stop() {
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing = null;
        }
        musicPause = 0;
        heard = false;
    }

    /**
     * The looping theme, heard the same everywhere (not positional), with a short fade in and out. After fading out
     * it keeps playing silently for a while, so if Quicksilver Time comes back soon the song simply continues.
     */
    private static final class Theme extends AbstractTickableSoundInstance {
        /** How long the song keeps running silently before it really stops (10 seconds). */
        private static final int SILENT_GRACE_TICKS = 200;
        private boolean fadingOut;
        private int silentTicks;

        Theme() {
            super(ModSounds.QUICKSILVER_THEME.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.05F;
        }

        void fadeIn() {
            fadingOut = false;
        }

        void fadeOut() {
            fadingOut = true;
        }

        /** Plays the song from config/enhanced/music if the player put one there, otherwise Sweet Dreams. */
        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Optional<AudioStream> song = CustomMusic.openSong(looping);
                    if (song.isPresent()) {
                        return song.get();
                    }
                } catch (IOException | RuntimeException e) {
                    Enhanced.LOGGER.warn("Could not play your Quicksilver song; playing Sweet Dreams instead", e);
                }
                return buffers.getStream(sound.getPath(), looping).join();
            }, Util.nonCriticalIoPool());
        }

        @Override
        public void tick() {
            if (fadingOut) {
                if (volume > 0.0F) {
                    volume = Math.max(0.0F, volume - 0.04F);
                } else if (++silentTicks > SILENT_GRACE_TICKS) {
                    stop();
                }
            } else {
                silentTicks = 0;
                if (volume < 1.0F) {
                    volume = Math.min(1.0F, volume + 0.1F);
                }
            }
        }
    }

    private QuicksilverMusic() {
    }
}
