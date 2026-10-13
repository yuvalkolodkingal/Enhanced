package com.kolodkingal.enhanced.apocalypse;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The sound of a critical hit on the boss Apocalypse (3.1, item 9): a heavy glassy crack with a low gong under it, about
 * half a second, heard by everyone near the hit. The boss plays it for every critical hit whose factor is above 1 (a
 * Scarlet Witch's or a Phoenix's mind power, at most once per attacker every 10 ticks), on his shield at the hit point and
 * on his body at his chest.
 *
 * <p>Until its own Ogg file lands, it plays a vanilla stand-in, the player's critical hit made louder and lower. The event
 * {@code enhanced:apocalypse_crit} is meanwhile neither registered nor in {@code sounds.json}, so every sound entry keeps
 * its file ({@code DataTests.everySoundHasItsFile}); the art lane adds the file, registers the event in the last region of
 * {@code ModSounds} and in {@code sounds.json} in the same commit, and points {@link #play} at it. Its subtitle,
 * {@code subtitles.enhanced.apocalypse_crit}, is in both lang files already.
 */
public final class CritSound {
    /** How loud the stand-in is. */
    static final float STAND_IN_VOLUME = 1.5F;
    /** How high the stand-in is (lower than the player's own crit: a heavier blow). */
    static final float STAND_IN_PITCH = 0.7F;

    /** Plays the crack at {@code pos} for everyone near (server side; on a client it does nothing). */
    public static void play(Level level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, STAND_IN_VOLUME, STAND_IN_PITCH);
    }

    private CritSound() {
    }
}
