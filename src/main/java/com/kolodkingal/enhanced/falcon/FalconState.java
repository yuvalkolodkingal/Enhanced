package com.kolodkingal.enhanced.falcon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Falcon's state (3.8): the attachment {@code ModAttachments.FALCON} (saved, not kept on death, synced to him:
 * Redwing's time and repairs on his HUD). Immutable: every change makes a new record. Written by the 3.8 skeleton; lane
 * L5 drives it.
 *
 * @param redwing          his drone Redwing, while it is out
 * @param redwingUntil     the game time Redwing flies back into the pack at
 * @param redwingDownUntil the game time a shot-down Redwing is repaired at (0: it is fine)
 * @param flags            lane L5's bits
 */
public record FalconState(Optional<UUID> redwing, long redwingUntil, long redwingDownUntil, int flags) {
    public static final FalconState EMPTY = new FalconState(Optional.empty(), 0L, 0L, 0);

    public static final Codec<FalconState> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("redwing").forGetter(FalconState::redwing),
            Codec.LONG.optionalFieldOf("redwing_until", 0L).forGetter(FalconState::redwingUntil),
            Codec.LONG.optionalFieldOf("redwing_down_until", 0L).forGetter(FalconState::redwingDownUntil),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(FalconState::flags)
    ).apply(i, FalconState::new));

    public static final StreamCodec<ByteBuf, FalconState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), FalconState::redwing,
            ByteBufCodecs.VAR_LONG, FalconState::redwingUntil,
            ByteBufCodecs.VAR_LONG, FalconState::redwingDownUntil,
            ByteBufCodecs.VAR_INT, FalconState::flags,
            FalconState::new);

    public FalconState withRedwing(Optional<UUID> id, long until) {
        return new FalconState(id, until, redwingDownUntil, flags);
    }

    public FalconState withRedwingDownUntil(long time) {
        return new FalconState(redwing, redwingUntil, time, flags);
    }

    public FalconState withFlags(int value) {
        return new FalconState(redwing, redwingUntil, redwingDownUntil, value);
    }
}
