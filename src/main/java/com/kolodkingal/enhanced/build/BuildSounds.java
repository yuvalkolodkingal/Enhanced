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
 * What Build Magic sounds like (D-B 7.3): eight moments, each an event of its own with a synthesised Ogg file
 * ({@code enhanced:build_on_wanda}, {@code build_on_billy}, {@code build_cast_wanda}, {@code build_cast_billy},
 * {@code build_hum}, {@code build_fizzle}, {@code build_undo}, {@code build_rank_up}), and two that are not: a spell of the
 * Scarlet Witch of {@link #HUGE_CAST} blocks or more rises with the Hex of her gesture, and the done sound is a vanilla
 * one. Heard within 16 blocks of where each is played: the caster for the cast (and for the hum, which the front of a
 * working spell repeats where it is, so a valley-sized spell is heard where it happens), except the fizzle, which only
 * the builder hears. The eight subtitles are in both lang files.
 *
 * <p>Every method plays on the server, for everyone near (on a client {@code level.playSound} with no player does nothing).
 */
public final class BuildSounds {
    /** One moment's sound: the event, how loud, and the pitch (between {@code lowPitch} and {@code highPitch}). */
    private record Voice(Supplier<SoundEvent> sound, float volume, float lowPitch, float highPitch) {
        Voice(Supplier<SoundEvent> sound, float volume, float pitch) {
            this(sound, volume, pitch, pitch);
        }

        float pitch(RandomSource random) {
            return lowPitch == highPitch ? lowPitch : lowPitch + random.nextFloat() * (highPitch - lowPitch);
        }
    }

    /** "Reality bends": the Scarlet Witch enters build mode, a breathy rising minor chord with a shimmer. */
    private static final Voice ON_WANDA = new Voice(ModSounds.BUILD_ON_WANDA, 0.8F, 1.0F);
    /** "A spellbook opens": Billy enters build mode, a paper flutter and two glassy notes. */
    private static final Voice ON_BILLY = new Voice(ModSounds.BUILD_ON_BILLY, 0.8F, 1.0F);
    /** "Chaos magic rewrites reality": one of her spells starts, a soft whoosh opening into a shimmering chord. */
    private static final Voice CAST_WANDA = new Voice(ModSounds.BUILD_CAST_WANDA, 0.8F, 1.0F);
    /** Her spell of {@link #HUGE_CAST} blocks or more starts: the rising Hex of her gesture, which has its own file. */
    private static final Voice CAST_WANDA_HUGE = new Voice(ModSounds.HEX_RISE, 0.8F, 1.0F);
    /** "A spell crackles": one of Billy's spells starts, a quick electric crackle and a bright chime. */
    private static final Voice CAST_BILLY = new Voice(ModSounds.BUILD_CAST_BILLY, 0.8F, 1.0F);
    /** "Magic weaves": every 20 ticks at the front of the Scarlet Witch's working spell, a low warm hum (1.0 s). */
    private static final Voice HUM_WANDA = new Voice(ModSounds.BUILD_HUM, 0.5F, 0.9F, 1.0F);
    /** The same at the front of Billy's, higher. */
    private static final Voice HUM_BILLY = new Voice(ModSounds.BUILD_HUM, 0.5F, 1.3F, 1.5F);
    /** "A spell fizzles": a refusal, for the builder alone, a short "pfft" with a falling tone. */
    private static final Voice FIZZLE = new Voice(ModSounds.BUILD_FIZZLE, 0.6F, 1.0F);
    /** "Reality snaps back": an undo or a redo starts, the cast chord backwards ending in a soft snap. */
    private static final Voice UNDO = new Voice(ModSounds.BUILD_UNDO, 0.8F, 1.0F);
    /** "Billy learns new magic": he ranks up, five rising notes and a sparkle. */
    private static final Voice RANK_UP = new Voice(ModSounds.BUILD_RANK_UP, 0.9F, 1.0F);
    /** A spell is done: a vanilla sound, with no event of its own. */
    private static final Voice DONE = new Voice(() -> SoundEvents.AMETHYST_CLUSTER_PLACE, 0.6F, 1.0F);

    /** How far a sound of Build Magic carries, in blocks (a sound of volume 1 or less). */
    public static final double HEARD = 16.0;

    /** From this many blocks the Scarlet Witch's cast sounds bigger (as her gesture is, {@code WANDA_HEX_RISE}). */
    public static final int HUGE_CAST = 100_000;

    /** The builder enters build mode, at the builder. */
    public static void buildModeOn(ServerPlayer builder) {
        play(builder.level(), builder.position(), isBilly(builder) ? ON_BILLY : ON_WANDA);
    }

    /** A spell of {@code blocks} blocks by {@code hero} starts, at {@code at}: the caster, never the middle of a box he is far from. */
    public static void cast(Level level, Vec3 at, Power hero, long blocks) {
        play(level, at, hero == Power.BILLY ? CAST_BILLY : blocks >= HUGE_CAST ? CAST_WANDA_HUGE : CAST_WANDA);
    }

    /** The hum of {@code hero}'s working spell, at {@code at}: its front, and the caster (he hears his own spell weave). */
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

    private static void play(Level level, Vec3 at, Voice sound) {
        level.playSound(null, at.x, at.y, at.z, sound.sound.get(), SoundSource.PLAYERS, sound.volume, sound.pitch(level.getRandom()));
    }

    private BuildSounds() {
    }
}
