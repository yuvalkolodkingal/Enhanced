package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's progress with the Infinity Stones (3.6): the attachment {@code ModAttachments.STONES} (saved, kept on
 * death, synced to that player only). Immutable: every change makes a new record, which the caller puts back with
 * {@code player.setData}. Bits are append only (they are saved).
 *
 * @param taken       a bit per {@link StoneKind} (by ordinal): the site stone he has taken (the ledger's {@code player}
 * mode, lane L1)
 * @param flags       the bits of the flags below
 * @param snapReadyAt the game time from which his Gauntlet may snap again
 */
public record StoneProgress(int taken, int flags, long snapReadyAt) {
    // The flags. Append only (the bit is saved): at most 31.
    /** He has talked to the Stonekeeper (lane L5). */
    public static final int MET_STONEKEEPER = 1;
    /** He gave a soul for the Soul Stone (lane L5). */
    public static final int SACRIFICED_FOR = 1 << 1;
    /** He was given for the Soul Stone (lane L5: the message on respawn). */
    public static final int WAS_SACRIFICED = 1 << 2;
    /** He snapped with all six stones (lane L1: the advancement). */
    public static final int SNAPPED = 1 << 3;
    /** How many flags there are (the bits above are 0 to {@code FLAG_COUNT - 1}). */
    public static final int FLAG_COUNT = 4;

    public static final StoneProgress EMPTY = new StoneProgress(0, 0, 0L);

    public static final Codec<StoneProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("taken", 0).forGetter(StoneProgress::taken),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(StoneProgress::flags),
            Codec.LONG.optionalFieldOf("snap_ready_at", 0L).forGetter(StoneProgress::snapReadyAt)
    ).apply(i, StoneProgress::new));

    public static final StreamCodec<ByteBuf, StoneProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoneProgress::taken,
            ByteBufCodecs.VAR_INT, StoneProgress::flags,
            ByteBufCodecs.VAR_LONG, StoneProgress::snapReadyAt,
            StoneProgress::new);

    /** Whether every bit of {@code flag} is set. */
    public boolean has(int flag) {
        return (flags & flag) == flag;
    }

    public StoneProgress with(int flag) {
        return new StoneProgress(taken, flags | flag, snapReadyAt);
    }

    public StoneProgress without(int flag) {
        return new StoneProgress(taken, flags & ~flag, snapReadyAt);
    }

    /** Whether he has taken this site stone. */
    public boolean hasTaken(StoneKind kind) {
        return (taken & bit(kind)) != 0;
    }

    public StoneProgress withTaken(StoneKind kind, boolean on) {
        return new StoneProgress(on ? taken | bit(kind) : taken & ~bit(kind), flags, snapReadyAt);
    }

    public StoneProgress withSnapReadyAt(long time) {
        return new StoneProgress(taken, flags, time);
    }

    /** The bit of a stone in {@link #taken} and in a Gauntlet's sockets ({@link GauntletContents#stones}). */
    public static int bit(StoneKind kind) {
        return 1 << kind.ordinal();
    }
}
