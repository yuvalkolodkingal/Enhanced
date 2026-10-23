package com.kolodkingal.enhanced.platform.event.client;

import com.kolodkingal.enhanced.platform.event.Cancellable;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import org.jetbrains.annotations.Nullable;

/** The music the game wants to play: play other music (setMusic). */
public class SelectMusicEvent implements Cancellable {
    private final @Nullable Music originalMusic;
    private final @Nullable SoundInstance playingMusic;
    private @Nullable Music music;
    private boolean canceled;

    public SelectMusicEvent(@Nullable Music originalMusic, @Nullable SoundInstance playingMusic) {
        this.originalMusic = originalMusic;
        this.playingMusic = playingMusic;
        this.music = originalMusic;
    }

    @Nullable
    public Music getOriginalMusic() {
        return originalMusic;
    }

    @Nullable
    public SoundInstance getPlayingMusic() {
        return playingMusic;
    }

    @Nullable
    public Music getMusic() {
        return music;
    }

    public void setMusic(@Nullable Music music) {
        this.music = music;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
