package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.HelaMusicMath;
import com.kolodkingal.enhanced.hela.HelaEntity;
import com.kolodkingal.enhanced.hela.HelaState;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The music of the fight against Hela (3.5, PLAN 6.2): three stems of 48 seconds in E Phrygian, made to be played together
 * (lane L6: the drone and war drums; low strings and a choir; high brass and faster drums). All three start at the same
 * moment, so they stay in step for as long as they loop, the layers not yet due at nothing; layer {@code n} fades in when
 * the fight reaches phase {@code n} (her synced phase) and the whole fades with the distance to her (all of it within
 * {@link HelaMusicMath#FULL} blocks, none at {@link HelaMusicMath#REACH}). It is a music-volume sound (the Music slider), ducks the game's own
 * music, follows no packet (the boss the client can see), and is silent with the client option {@code bossMusic} off.
 * Original music, not the film's.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class HelaMusic {
    public static final double REACH = HelaMusicMath.REACH;
    private static final float FADE_IN = 1.0F / 40.0F;
    private static final float FADE_OUT = 1.0F / 100.0F;
    /** How fast a layer comes in or goes (two seconds: a bar of the music). */
    private static final float LAYER = 1.0F / 40.0F;

    private static final Layer[] LAYERS = new Layer[3];
    private static float master;
    private static int phase = 1;
    private static boolean playing;

    private static float volumeAt(double distance) {
        return HelaMusicMath.volumeAt(distance);
    }

    private static boolean layerWanted(int n, int phase) {
        return HelaMusicMath.layerWanted(n, phase);
    }

    @Nullable
    private static HelaEntity nearest(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        Vec3 at = mc.player.position();
        HelaEntity best = null;
        double bestD = REACH * REACH;
        for (HelaEntity hela : mc.level.getEntitiesOfClass(HelaEntity.class, new AABB(at, at).inflate(REACH))) {
            double d = hela.distanceToSqr(at);
            if (hela.isAlive() && d < bestD) {
                best = hela;
                bestD = d;
            }
        }
        return best;
    }

    /** Once per client tick (from Hela's client plug-in). */
    public static void tick(Minecraft mc) {
        HelaEntity hela = EnhancedClientConfig.bossMusic() ? nearest(mc) : null;
        boolean wanted = hela != null && hela.state() != HelaState.LEAVING && hela.state() != HelaState.DYING;
        float target = wanted ? volumeAt(hela.distanceTo(mc.player)) : 0.0F;
        if (wanted) {
            phase = Math.max(1, Math.min(3, hela.phase()));
        }
        if (target > 0.0F || master > 0.0F) {
            master = master < target ? Math.min(target, master + FADE_IN) : Math.max(target, master - (wanted ? FADE_IN : FADE_OUT));
        }
        if (master > 0.0F && (LAYERS[0] == null || LAYERS[0].isStopped())) {
            // All three at once, in step: the ones not due yet at nothing.
            stopLayers();
            for (int i = 0; i < 3; i++) {
                LAYERS[i] = new Layer(i == 0 ? ModSounds.MUSIC_HELA_1 : i == 1 ? ModSounds.MUSIC_HELA_2 : ModSounds.MUSIC_HELA_3,
                        layerWanted(i + 1, phase) ? 1.0F : 0.0F);
                mc.getSoundManager().play(LAYERS[i]);
            }
        }
        for (int i = 0; i < 3; i++) {
            if (LAYERS[i] != null) {
                LAYERS[i].wanted = layerWanted(i + 1, phase);
            }
        }
        playing = master > 0.0F && LAYERS[0] != null && !LAYERS[0].isStopped();
        if (playing && mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }
        if (master <= 0.0F && LAYERS[0] != null) {
            stopLayers();
        }
    }

    private static void stopLayers() {
        for (int i = 0; i < 3; i++) {
            if (LAYERS[i] != null) {
                LAYERS[i].end();
                LAYERS[i] = null;
            }
        }
    }

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Music chosen = event.getMusic();
        if (playing && chosen != null) {
            event.setMusic(null);
        }
    }

    public static boolean isPlaying() {
        return playing;
    }

    /** The layers' loudness now (for the debug overlay and checks): {@code [master, l1, l2, l3]}. */
    public static float[] levels() {
        return new float[]{master, LAYERS[0] == null ? 0 : LAYERS[0].fade, LAYERS[1] == null ? 0 : LAYERS[1].fade,
                LAYERS[2] == null ? 0 : LAYERS[2].fade};
    }

    /** Stops at once (leaving the world). */
    public static void stop() {
        stopLayers();
        master = 0.0F;
        phase = 1;
        playing = false;
    }

    /** One stem: looping, not placed in the world, its volume its layer's fade times the master. */
    private static final class Layer extends AbstractTickableSoundInstance {
        boolean wanted;
        float fade;

        Layer(Supplier<SoundEvent> sound, float start) {
            super(sound.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.fade = start;
            this.wanted = start > 0.0F;
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
            fade = wanted ? Math.min(1.0F, fade + LAYER) : Math.max(0.0F, fade - LAYER);
            volume = Math.max(0.0001F, 0.9F * fade * master);
        }
    }

    private HelaMusic() {
    }
}
