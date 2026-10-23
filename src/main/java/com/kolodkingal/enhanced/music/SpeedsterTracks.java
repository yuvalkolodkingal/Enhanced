package com.kolodkingal.enhanced.music;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Which song a speedster's Walkman plays now (3.5), from his synced power data, so the server and every client agree:
 * <ul>
 *     <li>{@link MusicTracks#QUICKSILVER} while Quicksilver Time slows the world around him (as before 3.5; his Super Speed
 *     alone plays nothing);</li>
 *     <li>{@link MusicTracks#SPEED} while Speed runs with his Super Speed (G on and running), or while his slow time (R) slows
 *     the world;</li>
 *     <li>nothing when his Walkman is off, or otherwise.</li>
 * </ul>
 * A custom hero with these abilities plays the same; one with Quicksilver Time and either of Speed's plays Quicksilver's song
 * in Quicksilver Time.
 */
public final class SpeedsterTracks {

    /** The song for the speedster's state right now, or null. */
    @Nullable
    public static String trackOf(PowerData data) {
        return trackOf(data, data.is(Live.SPEEDING));
    }

    /**
     * The song for the speedster's state; {@code running}: whether he counts as running at Super Speed (the client counts a
     * short stop in his run as running, so the song does not stutter whenever he stops sprinting for a moment).
     */
    @Nullable
    public static String trackOf(PowerData data, boolean running) {
        if (!data.walkman()) {
            return null;
        }
        if (data.has(HeroAbility.QUICKSILVER_TIME) && data.slowMo()) {
            return MusicTracks.QUICKSILVER;
        }
        if (data.has(HeroAbility.TOMMY_SPEED) && running || data.has(HeroAbility.TOMMY_TIME) && data.slowMo()) {
            return MusicTracks.SPEED;
        }
        return null;
    }

    /**
     * The mod's own song for a Walkman track: Speed's, or Quicksilver's "Silver Hours". Since 3.8.4 that is also what
     * plays when the player chose quicksilverSong = sweet_dreams but has no copy of his own in config/enhanced/music.
     */
    public static SoundEvent builtInSong(String track) {
        return MusicTracks.SPEED.equals(track) ? ModSounds.MUSIC_SPEED.get() : ModSounds.MUSIC_QUICKSILVER.get();
    }

    private SpeedsterTracks() {
    }
}
