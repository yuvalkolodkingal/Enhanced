package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.SelectMusicEvent;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * The boss's music: a loop that plays for as long as a body of Ultron is within 96 blocks of the camera, fades in over two
 * seconds, ducks the game's own music (which waits and starts again after it), and fades out over five seconds after the
 * last body is gone. It needs no packet (the bodies are entities the client sees), is in the music category of the sound
 * options (so it is silent with that slider at 0) and is not a tune anyone owns: the track is {@code ultron_theme}
 * ({@link UltronSounds#THEME}), streamed from its file.
 */
@Subscriber(side = Side.CLIENT)
public final class UltronMusic {
    /** How near a body must be to be heard, in blocks. */
    private static final double RANGE = 96.0;
    private static final float VOLUME = 0.6F;
    /** The volume gained a tick while it fades in (two seconds), and lost while it fades out (five). */
    private static final float FADE_IN = VOLUME / 40.0F;
    private static final float FADE_OUT = VOLUME / 100.0F;

    @Nullable
    private static Theme playing;

    /** Once per client tick: starts the theme when a body is near, and lets it fade when none is. */
    public static void tick(Minecraft mc) {
        boolean near = mc.player != null && mc.level != null && bodyNear(mc);
        if (near) {
            if (playing == null || !mc.getSoundManager().isActive(playing)) {
                mc.getMusicManager().stopPlaying();
                playing = new Theme();
                mc.getSoundManager().play(playing);
            }
            playing.fading = false;
        } else if (playing != null) {
            playing.fading = true;
            if (!mc.getSoundManager().isActive(playing)) {
                playing = null;
            }
        }
    }

    private static boolean bodyNear(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof UltronEntity && entity.isAlive() && entity.distanceToSqr(mc.player) <= RANGE * RANGE) {
                return true;
            }
        }
        return false;
    }

    /** Whether the theme is playing or fading: the game's own music keeps quiet meanwhile. */
    public static boolean holds() {
        return playing != null;
    }

    /** Stops the theme at once (the client left the world). */
    public static void stop() {
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing = null;
        }
    }

    @Subscribe
    public static void onSelectMusic(SelectMusicEvent event) {
        if (holds()) {
            event.setMusic(null);
        }
    }

    /** The looping theme, heard the same everywhere (not positional), with its fades. */
    private static final class Theme extends AbstractTickableSoundInstance {
        boolean fading;

        Theme() {
            super(UltronSounds.THEME.event(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.01F;
        }

        @Override
        public void tick() {
            if (fading) {
                volume = Math.max(0.0F, volume - FADE_OUT);
                if (volume <= 0.0F) {
                    stop();
                }
            } else {
                volume = Math.min(VOLUME, volume + FADE_IN);
            }
        }
    }

    private UltronMusic() {
    }
}
