package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.client.render.ApocalypseBosses;
import com.kolodkingal.enhanced.client.render.ApocalypseFxMath;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.SelectMusicEvent;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The music of the fight against En Sabah Nur (DESIGN section 17, 3.12): a loop ({@code apocalypse_theme}: a drone, a frame
 * drum and a ney-like lead in a Phrygian-dominant mode, a choir pad) that plays while he is within 96 blocks of the camera,
 * and from the third phase (his armour on its way to falling, the true form) a second, fuller loop ({@code
 * apocalypse_theme_final}) that takes over from it. No packet is needed: it follows the boss the client can see. It fades in
 * over two seconds, ducks the game's own music while it plays, and fades out over five seconds after he is gone (or the
 * client has left the world). It is a music-volume sound, so the Music slider controls it (silent at 0), and it is not the
 * film's score.
 */
@Subscriber(side = Side.CLIENT)
public final class ApocalypseMusic {
    /** Within this many blocks of him the music is heard: full to {@link #FULL}, falling to nothing at the edge. */
    public static final double REACH = ApocalypseFxMath.MUSIC_REACH;
    /** How fast the whole of it comes (two seconds) and goes (five), per tick. */
    private static final float FADE_IN = 1.0F / 40.0F;
    private static final float FADE_OUT = 1.0F / 100.0F;
    /** How fast one theme gives way to the other (two seconds). */
    private static final float CROSS = 1.0F / 40.0F;

    @Nullable
    private static Loop theme;
    @Nullable
    private static Loop finale;
    /** How loud the whole of it is (0 to 1): 1 near him, falling away to 0 when he is gone. */
    private static float master;
    private static boolean playing;

    /** Which theme a state of the fight asks for: the fuller one from the third phase. */
    public static boolean wantsFinale(int phase, boolean trueForm) {
        return ApocalypseFxMath.wantsFinale(phase, trueForm);
    }

    /**
     * How loud the music should be, 0 to 1, for a boss {@code distance} blocks from the camera: all of it within 64
     * blocks, none at 96.
     */
    public static float volumeAt(double distance) {
        return ApocalypseFxMath.musicVolume(distance);
    }

    /** Once per client tick: starts the loops he asks for and fades them with the distance and with him. */
    public static void tick(Minecraft mc) {
        ApocalypseEntity boss = mc.level == null || mc.player == null ? null : ApocalypseBosses.nearest(mc.player.position());
        boolean wanted = boss != null && boss.isAlive() && boss.state() != ApocalypseState.WITHDRAWING && !boss.shownByViewer() && volumeAt(boss.distanceTo(mc.player)) > 0.0F;
        float target = wanted ? volumeAt(boss.distanceTo(mc.player)) : 0.0F;
        boolean finalPhase = wanted && wantsFinale(boss.phase(), boss.isTrueForm());
        if (target > 0.0F || master > 0.0F) {
            master = master < target ? Math.min(target, master + FADE_IN) : Math.max(target, master - (wanted ? FADE_IN : FADE_OUT));
        }
        if (master > 0.0F) {
            if (theme == null || theme.isStopped()) {
                theme = new Loop(ModSounds.APOCALYPSE_THEME, finalPhase ? 0.0F : 1.0F);
                mc.getSoundManager().play(theme);
            }
            if (finalPhase && (finale == null || finale.isStopped())) {
                finale = new Loop(ModSounds.APOCALYPSE_THEME_FINAL, 0.0F);
                mc.getSoundManager().play(finale);
            }
        }
        if (theme != null) {
            theme.wanted = !finalPhase && wanted;
        }
        if (finale != null) {
            finale.wanted = finalPhase;
        }
        playing = master > 0.0F && (theme != null && !theme.isStopped() || finale != null && !finale.isStopped());
        if (playing && mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }
    }

    /** The game's own music waits while the fight's plays. */
    @Subscribe
    public static void onSelectMusic(SelectMusicEvent event) {
        Music chosen = event.getMusic();
        if (playing && chosen != null) {
            event.setMusic(null);
        }
    }

    /** Whether the fight's music is playing now. */
    public static boolean isPlaying() {
        return playing;
    }

    /** Stops the music at once (leaving the world). */
    public static void stop() {
        if (theme != null) {
            theme.end();
        }
        if (finale != null) {
            finale.end();
        }
        theme = null;
        finale = null;
        master = 0.0F;
        playing = false;
    }

    /** One loop. Not placed in the world: its volume is set from the distance to him and from which theme is wanted. */
    private static final class Loop extends AbstractTickableSoundInstance {
        boolean wanted = true;
        private float fade;

        Loop(Supplier<SoundEvent> sound, float start) {
            super(sound.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.fade = start;
        }

        void end() {
            stop();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            fade = wanted ? Math.min(1.0F, fade + CROSS) : Math.max(0.0F, fade - CROSS);
            volume = Math.max(0.0001F, 0.9F * fade * master);
            if (master <= 0.0F && fade <= 0.0F || !wanted && fade <= 0.0F && master <= 0.0F) {
                stop();
            }
        }
    }

    private ApocalypseMusic() {
    }
}
