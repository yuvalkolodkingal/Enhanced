package com.kolodkingal.enhanced.client.nano;

import com.kolodkingal.enhanced.client.music.MusicSources;
import com.kolodkingal.enhanced.nano.NanoUltronEntity;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.SelectMusicEvent;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * The music of the fight with Nano Ultron (3.12), while he is near and alive: the player's own song if he put one in
 * {@code config/enhanced/music} as {@code nano_ultron.mp3} (or .ogg, .wav), the world's if it has one, else Hela's
 * fight music. It fades in and out, and the game's own music waits meanwhile.
 */
@Subscriber(side = Side.CLIENT)
public final class NanoMusic {
    public static final String TRACK = com.kolodkingal.enhanced.music.MusicTracks.NANO_ULTRON;
    @Nullable
    private static Loop current;

    private NanoMusic() {
    }

    @Subscribe
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean fight = mc.level != null && mc.player != null && !mc.level.getEntitiesOfClass(NanoUltronEntity.class,
                mc.player.getBoundingBox().inflate(48), e -> e.isAlive()).isEmpty();
        if (fight && (current == null || current.isStopped())) {
            SoundEvent sound = ModSounds.MUSIC_HELA_3.get();
            current = new Loop(sound, MusicSources.resolve(TRACK, sound));
            mc.getSoundManager().play(current);
        } else if (!fight && current != null) {
            current.leaving = true;
            if (current.isStopped()) {
                current = null;
            }
        }
        if (current != null && !current.isStopped() && mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }
    }

    @Subscribe
    public static void onSelectMusic(SelectMusicEvent event) {
        if (current != null && !current.isStopped() && event.getMusic() != null) {
            event.setMusic(null);
        }
    }

    private static final class Loop extends MusicSources.Loop {
        private float fade;
        boolean leaving;

        Loop(SoundEvent sound, MusicSources.Source source) {
            super(sound, SoundSource.MUSIC, source);
            this.volume = 0.001F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            fade = leaving ? Math.max(0.0F, fade - 1.0F / 60.0F) : Math.min(1.0F, fade + 1.0F / 40.0F);
            volume = Math.max(0.0001F, 0.9F * fade);
            if (leaving && fade <= 0.0F) {
                stop();
            }
        }
    }
}
