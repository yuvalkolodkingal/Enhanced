package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.Thoughts;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Client side of Billy's The Noise ("it's so noisy in my head", WandaVision episode 7): every creature whose thoughts
 * he hears glows through walls, red if it means harm and blue otherwise, and their whispers fill his head (for him
 * alone), louder the more of them there are. Since 3.3 it is also Professor X's Hear Thoughts (glowing in his own
 * colour), and the thoughts have words over the heads ({@link ThoughtBubbles}).
 */
public final class TheNoise {
    /** The glow of a creature that means harm. */
    public static final int HOSTILE = 0xFF3B3B;

    private static Whispers whispers;
    /** Creatures in earshot, counted every half second (sets the whispers' volume). */
    private static int heard;

    public static boolean active() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        PowerData data = Powers.get(player);
        return (data.power() == Power.BILLY && data.toggle(Toggle.THE_NOISE))
                || (data.power() == Power.PROFESSOR_X && data.toggle(Toggle.HEAR_THOUGHTS) && !data.is(Live.MIND_LINKED));
    }

    /** How far the local listener hears (Billy: The Noise's range; Professor X: Hear Thoughts'). */
    public static double range() {
        LocalPlayer player = Minecraft.getInstance().player;
        return Thoughts.range(player == null ? Power.BILLY : Powers.power(player));
    }

    /** Whether Billy hears this creature right now (so it glows for him). */
    public static boolean reveals(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player || !(entity instanceof LivingEntity) || entity instanceof ArmorStand || entity.isSpectator()
                || !active()) {
            return false;
        }
        double range = range();
        return entity.distanceToSqr(player) < range * range;
    }

    /** The colour a creature he hears glows in: its mood. */
    public static int moodColor(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        return BillyAbilities.meansHarm(entity) ? HOSTILE : player != null && Powers.power(player) == Power.PROFESSOR_X ? Power.PROFESSOR_X.color : Power.BILLY.color;
    }

    static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean on = player != null && mc.level != null && player.isAlive() && active();
        if (!on) {
            heard = 0;
            return;
        }
        if (player.tickCount % 10 == 0) {
            double range = range();
            heard = mc.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), TheNoise::reveals).size();
        }
        if (whispers == null || whispers.isStopped()) {
            whispers = new Whispers();
            mc.getSoundManager().play(whispers);
        }
    }

    static void stop() {
        if (whispers != null) {
            whispers.end();
            whispers = null;
        }
        heard = 0;
    }

    /** The whispers in his head: not placed in the world, fading in and out with The Noise. */
    private static final class Whispers extends AbstractTickableSoundInstance {
        Whispers() {
            super(ModSounds.THE_NOISE.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
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
            // A soft hush with nobody around, a little louder for every creature heard (up to a dozen).
            float target = active() ? 0.12F + 0.05F * Math.min(heard, 12) : 0.0F;
            if (volume < target) {
                volume = Math.min(target, volume + 0.04F);
            } else if (volume > target) {
                volume = Math.max(target, volume - 0.04F);
            }
            if (target == 0.0F && volume <= 0.0F) {
                stop();
            }
        }
    }

    private TheNoise() {
    }
}
