package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * The voices of the Ultron fight, named for what they are for ({@code ultron_awaken}, {@code ultron_bolt}, ...): the
 * synthesised sounds of the mod ({@code ultron_*} and {@code sentry_*} in {@code ModSounds}), each with the loudness and
 * pitch it is played at. Only the Punch is a vanilla sound, an iron golem's blow: his fist is a fist, not a machine. Both
 * the server (a sound heard once) and the clients (the loops) play them through here.
 */
public enum UltronSounds {
    /** The ritual: servos, scrape, a rising electrical hum and a high-pitched noise. */
    AWAKEN(ModSounds.ULTRON_AWAKEN::get, 1.5F, 1.0F),
    /** Every plasma bolt, the boss's and the Sentries'. */
    BOLT(ModSounds.ULTRON_BOLT::get, 0.8F, 1.0F),
    /** The tell of the Volley, the Punch and a Sentry's shot: servos whine. */
    SERVO(ModSounds.ULTRON_SERVO::get, 0.8F, 1.0F),
    /** The Finger Beams: an electric hum (the start of the tell; the clients loop it after that). */
    BEAM(ModSounds.ULTRON_BEAM::get, 0.5F, 1.0F),
    /** The Gravity Pull and the Dive's landing: a whomp. */
    GRAVITY(ModSounds.ULTRON_GRAVITY::get, 1.2F, 1.0F),
    /** The ground tears loose. */
    LIFT_START(ModSounds.ULTRON_LIFT_START::get, 1.5F, 1.0F),
    /** Sokovia rumbles. */
    RUMBLE(ModSounds.ULTRON_RUMBLE::get, 1.2F, 1.0F),
    /** Data uploads: a bit-crushed chirp. */
    UPLOAD(ModSounds.ULTRON_UPLOAD::get, 0.9F, 1.0F),
    /** A line of speech: a robot voice. */
    SPEAK(ModSounds.ULTRON_SPEAK::get, 1.0F, 1.0F),
    /** A body is destroyed and he changes bodies. */
    TRANSFER(ModSounds.ULTRON_TRANSFER::get, 1.4F, 1.0F),
    /** The final destruction: a long power-down. */
    DEATH(ModSounds.ULTRON_DEATH::get, 1.6F, 1.0F),
    /** A Sentry or drone destroyed. */
    SENTRY_DEATH(ModSounds.SENTRY_DEATH::get, 0.9F, 1.0F),
    /** A hit of the Punch: the iron golem's blow, the one vanilla sound here. */
    PUNCH(() -> SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.6F),
    /** The hero's Overcharge: a rising whine, then a boom. */
    OVERCHARGE(ModSounds.ULTRON_OVERCHARGE::get, 1.1F, 1.0F),
    // What the clients loop (the sounds above are played once, by the server). The music is a client loop that plays
    // while a body of Ultron is within 96 blocks of the camera; the beam loop follows the body while the Finger Beams
    // hum and fire; the hover loop is a Sentry's thrusters and the hero's anti-gravity; the rumble is Sokovia's.
    /** The boss's theme: a long loop, streamed. */
    THEME(ModSounds.ULTRON_THEME::get, 1.0F, 1.0F),
    /** The Finger Beams: an electric hum that follows him. */
    BEAM_LOOP(ModSounds.ULTRON_BEAM::get, 0.9F, 1.0F),
    /** Thrusters: a Sentry's, and the hero's flight (pitched down). */
    HOVER_LOOP(ModSounds.SENTRY_HOVER::get, 1.0F, 1.0F),
    /** Sokovia rumbles: the warning of the lift and the move itself. */
    RUMBLE_LOOP(ModSounds.ULTRON_RUMBLE::get, 1.0F, 1.0F);

    private final Supplier<SoundEvent> event;
    private final float volume;
    private final float pitch;

    UltronSounds(Supplier<SoundEvent> event, float volume, float pitch) {
        this.event = event;
        this.volume = volume;
        this.pitch = pitch;
    }

    /** The loudness this sound is mastered at, for a loop that plays it (the scale a client multiplies its fade by). */
    public float volume() {
        return volume;
    }

    /** The pitch this sound is mastered at. */
    public float pitch() {
        return pitch;
    }

    /** The sound event to play (for a chat line's voice, or anything that takes an event). */
    public SoundEvent event() {
        return event.get();
    }

    /** Plays it at a point, in the hostile sounds, with its own volume and pitch. */
    public void play(ServerLevel level, Vec3 at) {
        play(level, at, 1.0F, 1.0F);
    }

    /** Plays it with the volume and pitch scaled. */
    public void play(ServerLevel level, Vec3 at, float volumeScale, float pitchScale) {
        play(level, at, SoundSource.HOSTILE, volumeScale, pitchScale);
    }

    /** Plays it in the given sound category (the hero's voices are the players'). */
    public void play(ServerLevel level, Vec3 at, SoundSource source, float volumeScale, float pitchScale) {
        level.playSound(null, at.x, at.y, at.z, event.get(), source, volume * volumeScale, pitch * pitchScale);
    }
}
