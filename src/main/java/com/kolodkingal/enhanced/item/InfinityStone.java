package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * The six Infinity Stones, each with a voice of its own: a short sound for when it is used (a few takes of it, so it
 * is never quite the same twice) and a quiet hum that can loop for as long as someone holds it.
 *
 * <ul>
 *   <li>The Space Stone, in the Tesseract: a deep, swelling cosmic hum.</li>
 *   <li>The Mind Stone, in Loki's Scepter and in Vision's forehead: a bright, shimmering, slightly eerie
 *       chime-hum.</li>
 *   <li>The Power Stone: a heavy violet roar.</li>
 *   <li>The Reality Stone: a liquid, bending, backwards-running swell.</li>
 *   <li>The Soul Stone: a breathy, mournful choir tone.</li>
 *   <li>The Time Stone: ticking, rushing, rewinding shimmer.</li>
 * </ul>
 *
 * The Space and Mind Stones are in the mod today (see {@link TesseractItem}, {@link LokisScepterItem}, and
 * PowerLoopSounds for the hum while one is held). The other four have their voices registered and nothing else yet:
 * they are for the Thanos boss, who gathers them one by one.
 *
 * <p>The voices are mastered well under the mod's other sounds, and are played at about {@link #UNDER} of full volume
 * where they accompany one: a stone goes underneath the portal, the awakening or the chime it is heard with, never
 * on top of it.
 */
public enum InfinityStone {
    SPACE("space", ModSounds.SPACE_STONE, ModSounds.SPACE_STONE_HUM),
    MIND("mind", ModSounds.MIND_STONE, ModSounds.MIND_STONE_HUM),
    POWER("power", ModSounds.POWER_STONE, ModSounds.POWER_STONE_HUM),
    REALITY("reality", ModSounds.REALITY_STONE, ModSounds.REALITY_STONE_HUM),
    SOUL("soul", ModSounds.SOUL_STONE, ModSounds.SOUL_STONE_HUM),
    TIME("time", ModSounds.TIME_STONE, ModSounds.TIME_STONE_HUM);

    /** The volume of a stone's voice where it plays together with another sound. */
    public static final float UNDER = 0.6F;

    private final String id;
    private final Supplier<SoundEvent> use;
    private final Supplier<SoundEvent> hum;

    InfinityStone(String id, Supplier<SoundEvent> use, Supplier<SoundEvent> hum) {
        this.id = id;
        this.use = use;
        this.hum = hum;
    }

    /** The stone's name in sound names: {@code space} for {@code space_stone} and {@code space_stone_hum}. */
    public String id() {
        return id;
    }

    /** The short sound of the stone being used. */
    public SoundEvent useSound() {
        return use.get();
    }

    /** The quiet loop of the stone being held. */
    public SoundEvent humSound() {
        return hum.get();
    }

    /** Plays the stone's use sound for everyone near, where it is used. Call it on the server. */
    public void use(Level level, double x, double y, double z, float volume, float pitch) {
        level.playSound(null, x, y, z, use.get(), SoundSource.PLAYERS, volume, pitch);
    }

    /** {@link #use(Level, double, double, double, float, float)} at a point. */
    public void use(Level level, Vec3 at, float volume, float pitch) {
        use(level, at.x, at.y, at.z, volume, pitch);
    }
}
