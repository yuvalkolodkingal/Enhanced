package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.road.RoadMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays the music of the Witches' Road stage the server says the player is at ({@link RoadMusic}): the stage's loop fades
 * in over two seconds, crossfades into the next stage's over two seconds, and fades out over three when he leaves the
 * Road. It is a music-volume sound (the Music slider controls it; silent at 0) and the game's own music waits while it
 * plays. The music is an original ballad, not the show's song.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class RoadMusicPlayer {
    /** How fast a loop comes in (two seconds), and goes when the next stage's takes over (two) or the Road is left (three). */
    private static final float FADE_IN = 1.0F / 40.0F;
    private static final float CROSS_OUT = 1.0F / 40.0F;
    private static final float FADE_OUT = 1.0F / 60.0F;

    private static String stage = RoadMusic.NONE;
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (current != null || !FADING.isEmpty()) {
                stop();
            }
            return;
        }
        SoundEvent wanted = RoadMusic.soundOf(stage);
        if (current != null && (wanted == null || !current.getLocation().equals(wanted.getLocation()))) {
            current.leaving = wanted == null ? FADE_OUT : CROSS_OUT;
            FADING.add(current);
            current = null;
        }
        if (current == null && wanted != null) {
            current = new Loop(wanted);
            mc.getSoundManager().play(current);
            Enhanced.LOGGER.debug("The Witches' Road plays {} ({})", wanted.getLocation(), stage);
        }
        FADING.removeIf(Loop::isStopped);
        if (isPlaying() && mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }
    }

    /** The game's own music waits while the Road's plays. */
    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        if (isPlaying() && event.getMusic() != null) {
            event.setMusic(null);
        }
    }

    @SubscribeEvent
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
    private static final class Loop extends AbstractTickableSoundInstance {
        private float fade;
        /** How fast it fades out per tick, once it is on its way out (0 while it is the stage's). */
        float leaving;

        Loop(SoundEvent sound) {
            super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
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
