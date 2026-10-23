package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.music.MusicSources;
import com.kolodkingal.enhanced.client.music.ServerMusicClient;
import com.kolodkingal.enhanced.music.MusicTracks;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.SelectMusicEvent;
import com.kolodkingal.enhanced.road.RoadMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays the music of the Witches' Road stage the server says the player is at ({@link RoadMusic}): the stage's loop fades
 * in over two seconds, crossfades into the next stage's over two seconds, and fades out over three when he leaves the
 * Road. It is a music-volume sound (the Music slider controls it; silent at 0) and the game's own music waits while it
 * plays. The music is an original ballad, not the show's song, in a version of its own for each part of the Road.
 * <p>
 * 3.5: the player's own song ({@code config/enhanced/music/witches_road_<part>.*} or {@code witches_road.*}) or the world's
 * ({@code witches_road_<part>.ogg}, {@code witches_road.ogg} or {@code music.ogg} in its music folder) plays instead
 * ({@link MusicSources}); when the next part plays the same file, it simply goes on.
 */
@Subscriber(side = Side.CLIENT)
public final class RoadMusicPlayer {
    /** How fast a loop comes in (two seconds), and goes when the next stage's takes over (two) or the Road is left (three). */
    private static final float FADE_IN = 1.0F / 40.0F;
    private static final float CROSS_OUT = 1.0F / 40.0F;
    private static final float FADE_OUT = 1.0F / 60.0F;

    private static String stage = RoadMusic.NONE;
    private static int generationSeen;
    @Nullable
    private static Loop current;
    /** Loops on their way out. */
    private static final List<Loop> FADING = new ArrayList<>();

    /** The server says the player is now at this stage of the Road ("" when off it). */
    public static void setStage(String next) {
        stage = next;
    }

    /** The stage whose music plays (or is about to). */
    public static String stage() {
        return stage;
    }

    @Subscribe
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (current != null || !FADING.isEmpty()) {
                stop();
            }
            return;
        }
        SoundEvent wanted = RoadMusic.soundOf(stage);
        String track = MusicTracks.road(stage);
        int generation = ServerMusicClient.generation();
        boolean changed = generation != generationSeen;
        generationSeen = generation;
        MusicSources.Source source = null;
        if (current != null && wanted != null && track != null && (changed || !track.equals(current.source().track()))) {
            source = MusicSources.resolve(track, wanted);
            if (source.key().equals(current.source().key())) {
                // The same song for this part too (one file for the whole Road): it goes on.
                current.retrack(source);
                source = null;
            }
        }
        if (current != null && (wanted == null || track == null || source != null || !track.equals(current.source().track()))) {
            current.leaving = wanted == null ? FADE_OUT : CROSS_OUT;
            FADING.add(current);
            current = null;
        }
        if (current == null && wanted != null && track != null) {
            current = new Loop(wanted, source != null ? source : MusicSources.resolve(track, wanted));
            mc.getSoundManager().play(current);
            Enhanced.LOGGER.debug("The Witches' Road plays {} ({})", current.source().describe(), stage);
        }
        FADING.removeIf(Loop::isStopped);
        if (isPlaying() && mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }
    }

    /** The game's own music waits while the Road's plays. */
    @Subscribe
    public static void onSelectMusic(SelectMusicEvent event) {
        if (isPlaying() && event.getMusic() != null) {
            event.setMusic(null);
        }
    }

    @Subscribe
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    /** Whether the Road's music is playing now. */
    public static boolean isPlaying() {
        return current != null && !current.isStopped() || FADING.stream().anyMatch(loop -> !loop.isStopped());
    }

    /** Stops the music at once (leaving the world). */
    public static void stop() {
        if (current != null) {
            current.end();
        }
        FADING.forEach(Loop::end);
        FADING.clear();
        current = null;
        stage = RoadMusic.NONE;
    }

    /** One stage's loop, heard everywhere alike (not placed in the world). */
    private static final class Loop extends MusicSources.Loop {
        private float fade;
        /** How fast it fades out per tick, once it is on its way out (0 while it is the stage's). */
        float leaving;
        /** The stage it now plays for (it may go on into the next part if that one plays the same song). */
        private MusicSources.Source playingFor;

        Loop(SoundEvent sound, MusicSources.Source source) {
            super(sound, SoundSource.MUSIC, source);
            this.playingFor = source;
            this.volume = 0.001F;
        }

        @Override
        public MusicSources.Source source() {
            return playingFor;
        }

        void retrack(MusicSources.Source next) {
            playingFor = next;
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
            fade = leaving > 0.0F ? Math.max(0.0F, fade - leaving) : Math.min(1.0F, fade + FADE_IN);
            volume = Math.max(0.0001F, 0.85F * fade);
            if (leaving > 0.0F && fade <= 0.0F) {
                stop();
            }
        }
    }

    private RoadMusicPlayer() {
    }
}
