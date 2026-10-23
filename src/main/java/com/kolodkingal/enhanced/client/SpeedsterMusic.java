package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.client.music.MusicSources;
import com.kolodkingal.enhanced.client.music.ServerMusicClient;
import com.kolodkingal.enhanced.music.MusicTracks;
import com.kolodkingal.enhanced.music.SpeedsterTracks;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The speedsters' Walkman (3.5; Quicksilver's alone until then). While a speedster with his Walkman on is near, his song
 * plays for everyone close enough to be caught in his slow motion ({@link SpeedsterTracks}: Quicksilver in Quicksilver Time;
 * Speed running with his Super Speed, or in his slow time), and fades out when he stops. Quicksilver's song is his own theme
 * (or, with the client option quicksilverSong, the player's own copy of "Sweet Dreams": 3.8.4), Speed's his; the
 * player's own song or the world's plays instead if there is one ({@link MusicSources}). The nearest speedster's song
 * plays (the listener's own first), and a change of song crossfades. It is a Jukebox/Note Blocks sound, and the game's
 * own music waits while it plays.
 */
public final class SpeedsterMusic {
    /** How long Speed still counts as running after he stops for a moment (2 seconds), so his song does not stutter. */
    static final int RUN_LINGER_TICKS = 40;
    /** How long a chosen song is trusted before the folders are looked at again, while it is not playing (5 seconds). */
    private static final int RESOLVE_TICKS = 100;

    @Nullable
    private static Theme playing;
    /** Songs on their way out (another speedster's song took over). */
    private static final List<Theme> FADING = new ArrayList<>();
    /** Ticks the game's own music still waits after the song ended, like its usual pause between tracks. */
    private static int musicPause;
    /** Whether the current song was actually audible (not muted by the volume sliders, and its file loaded). */
    private static boolean heard;
    /** The game time each speedster was last seen running. */
    private static final Map<UUID, Long> LAST_RUN = new HashMap<>();
    private static int generationSeen;
    @Nullable
    private static MusicSources.Source lastSource;
    private static int lastResolved;
    private static int ticks;
    /** Whether the player was told this game that "Sweet Dreams" is no longer built in (3.8.4). */
    private static boolean sweetDreamsHinted;

    static void tick(Minecraft mc) {
        ticks++;
        LocalPlayer listener = mc.player;
        String want = listener != null && mc.level != null ? wanted(listener) : null;
        int generation = ServerMusicClient.generation();
        boolean changed = generation != generationSeen;
        generationSeen = generation;
        FADING.removeIf(theme -> !mc.getSoundManager().isActive(theme));
        if (want != null) {
            if (playing != null && (!want.equals(playing.source().track())
                    || changed && !source(want, true).key().equals(playing.source().key()))) {
                // Another song now (another speedster, or the world's song just arrived): crossfade.
                retire(playing);
                playing = null;
            }
            if (playing == null || !mc.getSoundManager().isActive(playing)) {
                playing = new Theme(source(want, false));
                mc.getSoundManager().play(playing);
                hintSweetDreams(listener, playing.source());
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

    private static void retire(Theme theme) {
        theme.retire();
        FADING.add(theme);
    }

    /** The song for a track, looked up again only every few seconds (or at once when {@code fresh}). */
    private static MusicSources.Source source(String track, boolean fresh) {
        SoundEvent builtIn = builtIn(track);
        if (fresh || lastSource == null || !lastSource.track().equals(track) || ticks - lastResolved > RESOLVE_TICKS
                || lastSource.kind() == MusicSources.Kind.BUILT_IN && !lastSource.key().equals(builtIn.getLocation().toString())) {
            lastSource = MusicSources.resolve(track, builtIn);
            lastResolved = ticks;
        }
        return lastSource;
    }

    /** The mod's own song for a track (Quicksilver's is always his own since 3.8.4). */
    static SoundEvent builtIn(String track) {
        return SpeedsterTracks.builtInSong(track);
    }

    /**
     * 3.8.4: "Sweet Dreams" is no longer in the mod. When a player who chose it hears Quicksilver's own song instead
     * (he has no copy in config/enhanced/music), he is told once per game where to put his own copy.
     */
    private static void hintSweetDreams(LocalPlayer listener, MusicSources.Source source) {
        if (!sweetDreamsHinted && MusicTracks.QUICKSILVER.equals(source.track())
                && source.kind() == MusicSources.Kind.BUILT_IN && EnhancedClientConfig.sweetDreams()) {
            sweetDreamsHinted = true;
            listener.displayClientMessage(Component.translatable("message.enhanced.quicksilver.sweet_dreams_missing")
                    .withStyle(ChatFormatting.GRAY), false);
        }
    }

    /**
     * Whether the game's music must stay quiet (see ClientEvents.onSelectMusic): always while the song plays, and for
     * the game's usual pause between tracks after it ends, cut short like the game does for music that wants to start
     * sooner (boss music comes back at once).
     */
    static boolean holdsGameMusic(Music music) {
        if (playing != null || !FADING.isEmpty()) {
            return true;
        }
        musicPause = Math.min(musicPause, music.getMaxDelay());
        return musicPause > 0;
    }

    /** The song the listener should hear: his own Walkman's, or else the nearest speedster's in range; null for none. */
    @Nullable
    private static String wanted(LocalPlayer listener) {
        double range = SlowMotion.radius() + 16;
        long now = listener.level().getGameTime();
        String best = null;
        double bestDistance = range * range;
        for (Player player : listener.level().players()) {
            PowerData data = Powers.get(player);
            boolean running = data.is(Live.SPEEDING);
            if (running) {
                LAST_RUN.put(player.getUUID(), now);
            } else {
                Long last = LAST_RUN.get(player.getUUID());
                running = last != null && now >= last && now - last <= RUN_LINGER_TICKS;
            }
            String track = SpeedsterTracks.trackOf(data, running);
            if (track == null) {
                continue;
            }
            double distance = player == listener ? -1.0 : player.distanceToSqr(listener);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = track;
            }
        }
        return best;
    }

    static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (playing != null) {
            mc.getSoundManager().stop(playing);
            playing = null;
        }
        FADING.forEach(theme -> mc.getSoundManager().stop(theme));
        FADING.clear();
        LAST_RUN.clear();
        lastSource = null;
        musicPause = 0;
        heard = false;
    }

    /**
     * The looping song, heard the same everywhere (not positional), with a short fade in and out. After fading out it keeps
     * playing silently for a while, so if the speedster starts again soon the song simply continues (unless another song took
     * over: then it stops once silent).
     */
    private static final class Theme extends MusicSources.Loop {
        /** How long the song keeps running silently before it really stops (10 seconds). */
        private static final int SILENT_GRACE_TICKS = 200;
        private boolean fadingOut;
        private boolean retired;
        private int silentTicks;

        Theme(MusicSources.Source source) {
            super(builtIn(source.track()), SoundSource.RECORDS, source);
            this.volume = 0.05F;
        }

        void fadeIn() {
            fadingOut = false;
        }

        void fadeOut() {
            fadingOut = true;
        }

        void retire() {
            retired = true;
            fadingOut = true;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                if (volume > 0.0F) {
                    volume = Math.max(0.0F, volume - 0.04F);
                } else if (retired || ++silentTicks > SILENT_GRACE_TICKS) {
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

    private SpeedsterMusic() {
    }
}
