package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Every sound the Thanos fight makes, by what it is for. Until the sound lane synthesizes the fight's own sounds
 * (DESIGN section 15.5.4: {@code thanos_roar}, {@code thanos_swing}, {@code gauntlet_clench} ...), each cue is played
 * with a sound the mod or the game already has, lowered and slowed to suit a Titan; when a cue gets its own event only
 * the line of its constant here changes. The six stones' voices are {@link StoneKind#voice()}.
 */
public enum ThanosSounds {
    /** The charge of the two items, and the portal opening. */
    SUMMON(() -> ModSounds.TESSERACT.get(), 1.0F, 0.55F),
    /** One bell of the charge, once a second. */
    SUMMON_BELL(() -> SoundEvents.BELL_RESONATE, 1.0F, 0.6F),
    /** He lands. */
    ARRIVE(() -> ModSounds.BIFROST_ARRIVE.get(), 1.4F, 0.6F),
    /** The roar: the arrival, each stone, the enrage. */
    ROAR(() -> SoundEvents.RAVAGER_ROAR, 1.6F, 0.45F),
    /** A footfall. */
    STEP(() -> SoundEvents.IRON_GOLEM_STEP, 0.9F, 0.45F),
    /** A blade swings. */
    SWING(() -> SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F),
    /** His blow lands. */
    HIT(() -> SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.55F),
    /** He is hit hard. */
    HURT(() -> SoundEvents.IRON_GOLEM_HURT, 1.0F, 0.5F),
    /** He is defeated. */
    DEATH(() -> SoundEvents.WARDEN_DEATH, 1.6F, 0.6F),
    /** The Gauntlet closes: the tell of every stone move. */
    CLENCH(() -> SoundEvents.CHAIN_PLACE, 1.2F, 0.6F),
    /** A stone fuses into the Gauntlet. */
    SURGE(() -> SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.6F),
    /** The Power Stone's blast and the Pulse. */
    POWER_BLAST(() -> SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.7F),
    /** The Infinity Beam's charge, a tone at a time. */
    INFINITY_CHARGE(() -> SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.5F),
    /** The Infinity Beam. */
    INFINITY_BEAM(() -> ModSounds.MIND_STONE_BEAM.get(), 1.5F, 0.55F),
    /** Time runs back. */
    REWIND(() -> SoundEvents.BELL_BLOCK, 1.3F, 0.5F),
    /** Chains of souls. */
    SOUL_DRAIN(() -> SoundEvents.CHAIN_BREAK, 1.0F, 0.6F),
    /** A soul is taken into the Soul World. */
    SOULWORLD(() -> SoundEvents.SOUL_ESCAPE.value(), 1.4F, 0.7F),
    /** A bubble of the Reality Stone pops. */
    REALITY_POP(() -> SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0F, 1.4F),
    /** A meteor falls. */
    METEOR_WHISTLE(() -> SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 0.5F),
    /** The Barrier rises. */
    BARRIER_UP(() -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.6F),
    /** A portal opens or closes. */
    PORTAL(() -> ModSounds.TESSERACT.get(), 1.0F, 1.3F),
    /** A phantom dissolves. */
    DISSOLVE(() -> ModSounds.LOKI_DISSOLVE.get(), 1.0F, 0.7F),
    /** The shield turns the sword home. */
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
