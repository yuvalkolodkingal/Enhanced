package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The sound of a critical hit on the boss Apocalypse (3.1, item 9): a heavy glassy crack with a low gong under it, about
 * half a second, heard by everyone near the hit. The boss plays it for every critical hit whose factor is above 1 (a
 * Scarlet Witch's or a Phoenix's mind power, at most once per attacker every 10 ticks), on his shield at the hit point and
 * on his body at his chest.
 *
 * <p>The event {@code enhanced:apocalypse_crit} is registered in the last region of {@code ModSounds} and in
 * {@code sounds.json}; its file, {@code sounds/apocalypse_crit.ogg}, is synthesised from scratch like every sound of the
 * mod: a 3 ms broadband crack, a swarm of glass shards between 1 and 6.5 kHz and a low gong (73 Hz and its modes) under
 * them, 0.5 s. Next to the vanilla critical hit (a mid thwack, centroid about 600 Hz, 30 ms to start) and the shield's
 * tink (a 3 kHz glass tick, 0.14 s) it is sharper, glassier and the only one with a low ring, so it reads as his and as
 * a crit. Its subtitle, {@code subtitles.enhanced.apocalypse_crit}, is in both lang files.
 */
public final class CritSound {
    /** How loud it is (above 1: heard from further than 16 blocks, across the whole arena). */
    static final float VOLUME = 1.4F;

    /** Plays the crack at {@code pos} for everyone near (server side; on a client it does nothing). */
    public static void play(Level level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.APOCALYPSE_CRIT.get(), SoundSource.HOSTILE, VOLUME, 1.0F);
    }

    private CritSound() {
    }
}
