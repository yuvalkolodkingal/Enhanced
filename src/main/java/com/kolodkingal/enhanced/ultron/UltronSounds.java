package com.kolodkingal.enhanced.ultron;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * The voices of the Ultron fight, named for what they are for ({@code ultron_awaken}, {@code ultron_bolt}, ...). The
 * server stage plays them through here; until the client stage synthesises and registers the real ones ({@code
 * ultron_*} and {@code sentry_*} in {@code ModSounds}), each stands in with a vanilla sound, so the whole fight can be
 * played and heard. Replacing one is a change to this file alone.
 */
public enum UltronSounds {
    /** The ritual: servos, scrape, a rising electrical hum and a high-pitched noise. */
    AWAKEN(() -> SoundEvents.BEACON_ACTIVATE, 1.5F, 0.6F),
    /** Every plasma bolt, the boss's and the Sentries'. */
    BOLT(() -> SoundEvents.BREEZE_SHOOT, 0.7F, 1.6F),
    /** The tell of the Volley, the Punch and a Sentry's shot: servos whine. */
    SERVO(() -> SoundEvents.PISTON_EXTEND, 0.8F, 1.7F),
    /** The Finger Beams: an electric hum. */
    BEAM(() -> SoundEvents.BEACON_AMBIENT, 1.0F, 1.8F),
    /** The Gravity Pull and the Dive's landing: a whomp. */
    GRAVITY(() -> SoundEvents.WARDEN_SONIC_CHARGE, 1.2F, 0.7F),
    /** The ground tears loose. */
    LIFT_START(() -> SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.5F),
    /** Sokovia rumbles. */
    RUMBLE(() -> SoundEvents.AMBIENT_CAVE.value(), 1.0F, 0.5F),
    /** Data uploads: a bit-crushed chirp. */
    UPLOAD(() -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.9F),
    /** A line of speech: a robot voice. */
    SPEAK(() -> SoundEvents.IRON_GOLEM_HURT, 1.0F, 0.5F),
    /** A body is destroyed and he changes bodies. */
    TRANSFER(() -> SoundEvents.BEACON_DEACTIVATE, 1.4F, 0.7F),
    /** The final destruction: a long power-down. */
    DEATH(() -> SoundEvents.BEACON_DEACTIVATE, 1.6F, 0.4F),
    /** A Sentry or drone destroyed. */
    SENTRY_DEATH(() -> SoundEvents.IRON_GOLEM_DEATH, 0.9F, 1.5F),
    /** A hit of the Punch. */
    PUNCH(() -> SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.6F),
    /** The hero's Overcharge: a rising whine, then a boom. */
    OVERCHARGE(() -> SoundEvents.WARDEN_SONIC_CHARGE, 1.2F, 1.5F),
    // What the clients loop (the sounds above are played once, by the server). The music is a client loop that plays
    // while a body of Ultron is within 96 blocks of the camera; the beam loop follows the body while the Finger Beams
    // hum and fire; the hover loop is a Sentry's thrusters and the hero's anti-gravity; the rumble is Sokovia's.
    /** The boss's theme: a long loop, streamed. */
    THEME(() -> SoundEvents.MUSIC_DISC_PRECIPICE.value(), 1.0F, 1.0F),
    /** The Finger Beams: an electric hum that follows him. */
    BEAM_LOOP(() -> SoundEvents.BEACON_AMBIENT, 1.0F, 1.6F),
    /** Thrusters: a Sentry's, and the hero's flight (pitched down). */
    HOVER_LOOP(() -> SoundEvents.ELYTRA_FLYING, 0.4F, 1.4F),
    /** Sokovia rumbles: the warning of the lift and the move itself. */
    RUMBLE_LOOP(() -> SoundEvents.AMBIENT_BASALT_DELTAS_LOOP.value(), 1.0F, 0.6F);

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
