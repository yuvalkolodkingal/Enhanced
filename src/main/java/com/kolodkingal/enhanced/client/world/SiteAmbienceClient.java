package com.kolodkingal.enhanced.client.world;

import com.kolodkingal.enhanced.client.stones.StoneFx;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.world.SiteAmbience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The stones' sites' ambience on the client (3.6): the server tells the player which site he is inside ({@link
 * StoneFxPayload#SITE_AMBIENCE}, see {@link SiteAmbience}); its loop ({@code ambient.sanctum} 24 s, {@code
 * end_tunnel_hum} 16 s, both seamless) plays as one looping sound for as long as he stays, fades in, and fades out when
 * he leaves, changes world, or no news came for {@link #SILENCE} ticks.
 */
public final class SiteAmbienceClient {
    /** Ticks without news after which the loop fades out (the server tells a player inside every 60 ticks). */
    static final int SILENCE = 140;
    /** The loop's full volume (the files are mastered at about -24 LUFS). */
    static final float VOLUME = 0.7F;
    /** How much the volume moves a tick (fade in and out over about two seconds). */
    static final float FADE = 0.02F;

    private static int site = SiteAmbience.NONE;
    private static long heardAt;
    @Nullable
    private static Loop loop;

    /** From the client setup: the drawer of the news. */
    public static void setup() {
        StoneFx.register(StoneFxPayload.SITE_AMBIENCE, SiteAmbienceClient::told);
    }

    private static void told(StoneFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        site = payload.arg();
        heardAt = mc.level.getGameTime();
    }

    private static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (site != SiteAmbience.NONE && mc.level.getGameTime() - heardAt > SILENCE) {
            site = SiteAmbience.NONE;
        }
        if (loop != null && (loop.isStopped() || loop.fadedOut())) {
            loop = null;
        }
        if (loop != null) {
            loop.fading = loop.site != site || loop.level != mc.level;
        }
        Holder<SoundEvent> sound = SiteAmbience.sound(site);
        if (sound != null && loop == null) {
            loop = new Loop(sound.value(), site, mc.level);
            mc.getSoundManager().play(loop);
        }
    }

    /** One site's loop: all round the listener, fading in, and out once told to. */
    private static final class Loop extends AbstractTickableSoundInstance {
        final int site;
        final ClientLevel level;
        /** Fading out (it left the site, or the site changed); set again each tick. */
        boolean fading;

        Loop(SoundEvent sound, int site, ClientLevel level) {
            super(sound, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.site = site;
            this.level = level;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
        }

        /** Whether it has faded out (and is stopped): a new loop may start. */
        boolean fadedOut() {
            return fading && isStopped();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != level) {
                stop();
                return;
            }
            float target = fading ? 0.0F : VOLUME;
            volume = Mth.clamp(volume + Mth.clamp(target - volume, -FADE, FADE), 0.0F, 1.0F);
            if (fading && volume <= 0.01F) {
                stop();
            }
        }
    }

    /** The client's ticks and its leaving a world (the game bus). */
    @Subscriber(side = Side.CLIENT)
    public static final class Events {
        @Subscribe
        public static void onClientTick(ClientTickEvent.Post event) {
            tick(Minecraft.getInstance());
        }

        @Subscribe
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            if (loop != null) {
                Minecraft.getInstance().getSoundManager().stop(loop);
                loop = null;
            }
            site = SiteAmbience.NONE;
        }

        private Events() {
        }
    }

    private SiteAmbienceClient() {
    }
}
