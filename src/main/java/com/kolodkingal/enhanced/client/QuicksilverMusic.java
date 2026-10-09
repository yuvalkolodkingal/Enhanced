package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Quicksilver's Walkman. While a Quicksilver with his Walkman on is running Quicksilver Time nearby, the theme plays
 * for everyone close enough to be caught in the slow motion, and fades out when he stops.
 */
public final class QuicksilverMusic {
    @Nullable
    private static Theme playing;

    static void tick(Minecraft mc) {
        LocalPlayer listener = mc.player;
        boolean shouldPlay = listener != null && mc.level != null && speedsterNearby(listener);
        if (shouldPlay) {
            if (playing == null || !mc.getSoundManager().isActive(playing)) {
                playing = new Theme();
                mc.getMusicManager().stopPlaying();
                mc.getSoundManager().play(playing);
            }
            playing.fadeIn();
        } else if (playing != null) {
            playing.fadeOut();
            if (!mc.getSoundManager().isActive(playing)) {
                playing = null;
            }
        }
    }

    private static boolean speedsterNearby(LocalPlayer listener) {
        double range = SlowMotion.radius() + 16;
        for (Player player : listener.level().players()) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.QUICKSILVER && data.slowMo() && data.walkman()
                    && player.distanceToSqr(listener) < range * range) {
                return true;
            }
        }
        return false;
    }

    static void stop() {
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing = null;
        }
    }

    /** The looping theme, heard the same everywhere (not positional), with a short fade in and out. */
    private static final class Theme extends AbstractTickableSoundInstance {
        private boolean fadingOut;

        Theme() {
            super(ModSounds.QUICKSILVER_THEME.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.05F;
        }

        void fadeIn() {
            fadingOut = false;
        }

        void fadeOut() {
            fadingOut = true;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                volume -= 0.04F;
                if (volume <= 0.0F) {
                    stop();
                }
            } else if (volume < 1.0F) {
                volume = Math.min(1.0F, volume + 0.1F);
            }
        }
    }

    private QuicksilverMusic() {
    }
}
