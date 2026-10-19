package com.kolodkingal.enhanced.wolverine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Wolverine's state (3.7): the attachment {@code ModAttachments.WOLVERINE} (saved, not kept on death, synced to him
 * only). Immutable: every change makes a new record. Lane L2 drives it.
 *
 * @param berserkUntil the game time the Berserker Rage ends at (0: not raging)
 * @param flags        append-only bits (lane L2's)
 */
public record WolverineState(long berserkUntil, int flags) {
    public static final WolverineState EMPTY = new WolverineState(0L, 0);

    public static final Codec<WolverineState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("berserk_until", 0L).forGetter(WolverineState::berserkUntil),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(WolverineState::flags)
    ).apply(i, WolverineState::new));

    public static final StreamCodec<ByteBuf, WolverineState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, WolverineState::berserkUntil,
            ByteBufCodecs.VAR_INT, WolverineState::flags,
            WolverineState::new);

    public WolverineState withBerserkUntil(long time) {
        return new WolverineState(time, flags);
    }

    public WolverineState withFlags(int value) {
        return new WolverineState(berserkUntil, value);
    }
}
