package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * What Build Magic sounds like (D-B 7.3): eight moments, each to become an event of its own ({@code enhanced:build_on_wanda},
 * {@code build_on_billy}, {@code build_cast_wanda}, {@code build_cast_billy}, {@code build_hum}, {@code build_fizzle},
 * {@code build_undo}, {@code build_rank_up}), and the done sound, a vanilla one. Heard within 16 blocks of where each is
 * played (the spell's front, so a valley-sized spell is heard where it happens), except the fizzle, which only the builder
 * hears.
 *
 * <p>Until a moment's synthesised Ogg file lands, it plays a stand-in from the sounds that already exist, and its event is
 * neither registered nor in {@code sounds.json}, so every sound entry keeps its file ({@code DataTests.everySoundHasItsFile}).
 * The art lane adds each file, registers the event in the last region of {@code ModSounds} ({@code // ===== 3.1 build =====})
 * and in {@code sounds.json} in the same commit, and points its moment here at it. The eight subtitles are in both lang files
 * already.
 *
 * <p>Every method plays on the server, for everyone near (on a client {@code level.playSound} with no player does nothing).
 */
public final class BuildSounds {
    /** One stand-in: the sound, how loud, and the pitch (between {@code lowPitch} and {@code highPitch}). */
    private record StandIn(Supplier<SoundEvent> sound, float volume, float lowPitch, float highPitch) {
        StandIn(Supplier<SoundEvent> sound, float volume, float pitch) {
            this(sound, volume, pitch, pitch);
        }

        float pitch(RandomSource random) {
            return lowPitch == highPitch ? lowPitch : lowPitch + random.nextFloat() * (highPitch - lowPitch);
        }
    }

    /** "Reality bends": the Scarlet Witch enters build mode, a breathy rising minor chord with a shimmer. */
    private static final StandIn ON_WANDA = new StandIn(ModSounds.BUILD_ON_WANDA, 0.8F, 1.0F);
    /** "A spellbook opens": Billy enters build mode. */
    private static final StandIn ON_BILLY = new StandIn(() -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
    /** "Chaos magic rewrites reality": one of her spells starts. */
    private static final StandIn CAST_WANDA = new StandIn(ModSounds.HEX_CROSS, 0.6F, 1.3F);
    /** Her spell of {@link #HUGE_CAST} blocks or more starts. */
    private static final StandIn CAST_WANDA_HUGE = new StandIn(ModSounds.HEX_RISE, 0.8F, 1.0F);
    /** "A spell crackles": one of Billy's spells starts. */
    private static final StandIn CAST_BILLY = new StandIn(ModSounds.HEX_BOLT, 0.6F, 1.5F);
    /** "Magic weaves": every 20 ticks at the front of the Scarlet Witch's working spell. */
    private static final StandIn HUM_WANDA = new StandIn(() -> SoundEvents.AMETHYST_BLOCK_RESONATE, 0.35F, 0.8F, 1.2F);
    /** The same at the front of Billy's, higher. */
    private static final StandIn HUM_BILLY = new StandIn(() -> SoundEvents.AMETHYST_BLOCK_RESONATE, 0.35F, 1.4F, 1.8F);
    /** "A spell fizzles": a refusal, for the builder alone. */
    private static final StandIn FIZZLE = new StandIn(() -> SoundEvents.FIRE_EXTINGUISH, 0.4F, 1.8F);
    /** "Reality snaps back": an undo or a redo starts. */
    private static final StandIn UNDO = new StandIn(ModSounds.TIME_REWIND, 0.7F, 1.5F);
    /** "Billy learns new magic": he ranks up. */
    private static final StandIn RANK_UP = new StandIn(() -> SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F);
    /** A spell is done: a vanilla sound, with no event of its own. */
    private static final StandIn DONE = new StandIn(() -> SoundEvents.AMETHYST_CLUSTER_PLACE, 0.6F, 1.0F);

    /** From this many blocks the Scarlet Witch's cast sounds bigger (as her gesture is, {@code WANDA_HEX_RISE}). */
    public static final int HUGE_CAST = 100_000;

    /** The builder enters build mode, at the builder. */
    public static void buildModeOn(ServerPlayer builder) {
        play(builder.level(), builder.position(), isBilly(builder) ? ON_BILLY : ON_WANDA);
    }

    /** A spell of {@code blocks} blocks by {@code hero} starts, at {@code at}. */
    public static void cast(Level level, Vec3 at, Power hero, long blocks) {
        play(level, at, hero == Power.BILLY ? CAST_BILLY : blocks >= HUGE_CAST ? CAST_WANDA_HUGE : CAST_WANDA);
    }

    /** The hum of {@code hero}'s working spell, at its front. */
    public static void hum(Level level, Vec3 at, Power hero) {
        play(level, at, hero == Power.BILLY ? HUM_BILLY : HUM_WANDA);
    }

    /** A refusal: only the builder hears it. */
    public static void fizzle(ServerPlayer builder) {
        builder.playNotifySound(FIZZLE.sound.get(), SoundSource.PLAYERS, FIZZLE.volume, FIZZLE.pitch(builder.getRandom()));
    }

    /** An undo or a redo starts, at {@code at}. */
    public static void undo(Level level, Vec3 at) {
        play(level, at, UNDO);
    }

    /** Billy ranks up: the fanfare, at him. */
    public static void rankUp(ServerPlayer billy) {
        play(billy.level(), billy.position(), RANK_UP);
    }

    /** A spell is done, at {@code at}. */
    public static void done(Level level, Vec3 at) {
        play(level, at, DONE);
    }

    private static boolean isBilly(ServerPlayer player) {
        return Powers.is(player, Power.BILLY);
    }

    private static void play(Level level, Vec3 at, StandIn sound) {
        level.playSound(null, at.x, at.y, at.z, sound.sound.get(), SoundSource.PLAYERS, sound.volume, sound.pitch(level.getRandom()));
    }

    private BuildSounds() {
    }
}
