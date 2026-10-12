package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Every sound the Thanos fight makes, by what it is for (DESIGN section 15.15.1): each cue is a sound event of the
 * mod's own, synthesized for him ({@code thanos_roar}, {@code thanos_step}, {@code gauntlet_clench} ...), played a
 * little louder or quieter and higher or lower by the places that call it. The portals, a dissolving phantom and the
 * shield turning the sword home reuse sounds of the other heroes. The six stones' voices are {@link StoneKind#voice()}.
 */
public enum ThanosSounds {
    /** The charge of the two items, and the portal opening. */
    SUMMON(() -> ModSounds.THANOS_SUMMON.get(), 1.0F, 1.0F),
    /** One bell of the charge, once a second. */
    SUMMON_BELL(() -> ModSounds.THANOS_BELL.get(), 1.0F, 1.0F),
    /** He lands. */
    ARRIVE(() -> ModSounds.THANOS_ARRIVE.get(), 1.4F, 1.0F),
    /** The roar: the arrival, each stone, the enrage. */
    ROAR(() -> ModSounds.THANOS_ROAR.get(), 1.6F, 1.0F),
    /** A footfall. */
    STEP(() -> ModSounds.THANOS_STEP.get(), 0.9F, 1.0F),
    /** A blade swings. */
    SWING(() -> ModSounds.THANOS_SWING.get(), 1.2F, 1.0F),
    /** His blow lands. */
    HIT(() -> ModSounds.THANOS_HIT.get(), 1.2F, 1.0F),
    /** He is hit hard. */
    HURT(() -> ModSounds.THANOS_HURT.get(), 1.0F, 1.0F),
    /** He is defeated. */
    DEATH(() -> ModSounds.THANOS_DEATH.get(), 1.6F, 1.0F),
    /** The Gauntlet closes: the tell of every stone move. */
    CLENCH(() -> ModSounds.GAUNTLET_CLENCH.get(), 1.2F, 1.0F),
    /** A stone fuses into the Gauntlet. */
    SURGE(() -> ModSounds.THANOS_STONE_SURGE.get(), 1.4F, 1.0F),
    /** The Power Stone's blast and the Pulse. */
    POWER_BLAST(() -> ModSounds.THANOS_POWER_BLAST.get(), 1.0F, 1.0F),
    /** The Infinity Beam's charge, five tones in turn: played once, at the start of the tell. */
    INFINITY_CHARGE(() -> ModSounds.THANOS_INFINITY_CHARGE.get(), 1.2F, 1.0F),
    /** The Infinity Beam. */
    INFINITY_BEAM(() -> ModSounds.THANOS_INFINITY_BEAM.get(), 1.5F, 1.0F),
    /** Time runs back. */
    REWIND(() -> ModSounds.TIME_REWIND.get(), 1.3F, 1.0F),
    /** Chains of souls. */
    SOUL_DRAIN(() -> ModSounds.SOUL_DRAIN.get(), 1.0F, 1.0F),
    /** A soul is taken into the Soul World. */
    SOULWORLD(() -> ModSounds.SOUL_WORLD.get(), 1.4F, 1.0F),
    /** A bubble of the Reality Stone pops. */
    REALITY_POP(() -> ModSounds.REALITY_POP.get(), 1.0F, 1.0F),
    /** A meteor falls. */
    METEOR_WHISTLE(() -> ModSounds.THANOS_METEOR_WHISTLE.get(), 1.0F, 1.0F),
    /** The Barrier rises. */
    BARRIER_UP(() -> ModSounds.THANOS_BARRIER_UP.get(), 1.2F, 1.0F),
    /** A portal opens or closes (the Tesseract's own sound, higher). */
    PORTAL(() -> ModSounds.TESSERACT.get(), 1.0F, 1.3F),
    /** A phantom dissolves (Loki's dissolve, lower). */
    DISSOLVE(() -> ModSounds.LOKI_DISSOLVE.get(), 1.0F, 0.7F),
    /** The shield turns the sword home (Mjolnir's impact, lower). */
    DEFLECT(() -> ModSounds.MJOLNIR_IMPACT.get(), 1.0F, 0.8F);

    private final Supplier<SoundEvent> sound;
    private final float volume;
    private final float pitch;

    ThanosSounds(Supplier<SoundEvent> sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    /** The sound event that plays for this cue. */
    public SoundEvent event() {
        return sound.get();
    }

    /** Plays the cue for everyone near {@code at}. */
    public void play(Level level, Vec3 at) {
        play(level, at, 1.0F, 1.0F);
    }

    /** Plays the cue louder or quieter ({@code volume}) and higher or lower ({@code pitch}) than its usual. */
    public void play(Level level, Vec3 at, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound.get(), SoundSource.HOSTILE, this.volume * volume, this.pitch * pitch);
    }
}
