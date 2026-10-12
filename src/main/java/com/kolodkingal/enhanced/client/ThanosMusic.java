package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.ThanosClientView;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The music of the fight (DESIGN section 15.3.12): one loop for each stone he holds, layered, so the music grows as he
 * does. Each loop fades in over two seconds when its stone arrives and stays; all of them are loud within 48 blocks of
 * him, fade to silence at 96, and fade out over three seconds when he dies or leaves. The game's own music waits while
 * it plays. It is a music-volume sound, so the music slider controls it, and the client option {@code bossMusic}
 * switches it off.
 *
 * <p>The loops are {@link #THEMES}: five stems of 40 seconds in D minor, each the layer a stone adds (1: a drone and a
 * heartbeat; 2: low strings; 3: dissonant high choir; 4: a driving ostinato; 5: the bells of the call), made to sound
 * together in any combination.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ThanosMusic {
    /** The layer each stone count adds, first to fifth: they play together, so the music grows as he does. */
    public static final List<Supplier<SoundEvent>> THEMES = List.of(
            () -> ModSounds.THANOS_THEME_1.get(),
            () -> ModSounds.THANOS_THEME_2.get(),
            () -> ModSounds.THANOS_THEME_3.get(),
            () -> ModSounds.THANOS_THEME_4.get(),
            () -> ModSounds.THANOS_THEME_5.get());
    /** Within this many blocks the music is at full volume; past {@link #SILENT} it is silent. */
    private static final double FULL = 48.0;
    private static final double SILENT = 96.0;
    private static final float FADE_IN = 1.0F / 40.0F;
    private static final float FADE_OUT = 1.0F / 60.0F;

    private static final List<Layer> LAYERS = new ArrayList<>();
    /** How loud the whole of it is (0 to 1): 1 near him, falling away to 0 when he is gone. */
    private static float master;
    private static boolean playing;

    /**
     * Once per client tick: starts the layers a stone count asks for, and fades the music with the distance and with
     * him.
     */
    public static void tick(Minecraft mc) {
        ThanosEntity boss = mc.level == null || mc.player == null ? null : ThanosClientView.nearestBoss(mc.player.position());
        boolean wanted = boss != null && boss.isAlive() && boss.state() != ThanosState.DYING && EnhancedClientConfig.bossMusic();
        float target = 0.0F;
        int stones = 0;
        if (wanted) {
            double distance = boss.position().distanceTo(mc.player.position());
            target = (float) Mth.clamp(1.0 - (distance - FULL) / (SILENT - FULL), 0.0, 1.0);
            stones = boss.stones();
            // He is beginning to fall: the music goes with him over three seconds (the dying state fades it to
            // nothing).
        }
        if (wanted && target > 0.0F || master > 0.0F) {
            master = master < target ? Math.min(target, master + 0.05F) : Math.max(target, master - FADE_OUT);
        }
        LAYERS.removeIf(Layer::isStopped);
        if (master > 0.0F && stones > 0) {
            for (int i = 0; i < Math.min(stones, THEMES.size()); i++) {
                final int index = i;
                if (LAYERS.stream().noneMatch(layer -> layer.index == index)) {
                    Layer layer = new Layer(index);
                    LAYERS.add(layer);
                    mc.getSoundManager().play(layer);
                }
            }
        }
        for (Layer layer : LAYERS) {
            layer.wanted = index(layer, stones) && wanted;
        }
        playing = master > 0.0F && !LAYERS.isEmpty();
        if (playing && mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }
    }

    private static boolean index(Layer layer, int stones) {
        return layer.index < stones;
    }

    /** The game's own music waits while the fight's plays. */
    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Music chosen = event.getMusic();
        if (playing && chosen != null) {
            event.setMusic(null);
        }
    }

    /** Stops the music at once (leaving the world). */
    public static void stop() {
        LAYERS.forEach(Layer::end);
        LAYERS.clear();
        master = 0.0F;
        playing = false;
    }

    /** One stone's loop. Not placed in the world: the volume is set from the distance to him. */
    private static final class Layer extends AbstractTickableSoundInstance {
        final int index;
        boolean wanted = true;
        private float fade;

        Layer(int index) {
            super(THEMES.get(index).get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.index = index;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001F;
            this.pitch = 1.0F;
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
            fade = wanted ? Math.min(1.0F, fade + FADE_IN) : Math.max(0.0F, fade - FADE_OUT);
            volume = Math.max(0.0001F, 0.9F * fade * master);
            if (!wanted && fade <= 0.0F || master <= 0.0F && fade <= 0.0F) {
                stop();
            }
        }
    }

    private ThanosMusic() {
    }
}
