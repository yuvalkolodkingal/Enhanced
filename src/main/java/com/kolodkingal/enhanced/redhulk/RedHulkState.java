package com.kolodkingal.enhanced.redhulk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Red Hulk's state (3.8): the attachment {@code ModAttachments.RED_HULK} (saved, not kept on death, synced to everyone
 * who sees him: his heat glow follows his anger). Immutable: every change makes a new record. Written by the 3.8
 * skeleton; lane L2 drives it.
 *
 * @param anger       0-100
 * @param lastFight   the game time he last took or dealt damage (the anger cools after {@code coolDelayTicks})
 * @param absorbUntil the game time Energy Absorb ends at (0: not absorbing)
 * @param flags       {@link #FALL_SAFE} and lane L2's other bits
 */
public record RedHulkState(float anger, long lastFight, long absorbUntil, int flags) {
    /** The next fall damage is cancelled (one landing: the Super Jump's). */
    public static final int FALL_SAFE = 1;
    public static final RedHulkState EMPTY = new RedHulkState(0.0F, 0L, 0L, 0);

    public static final Codec<RedHulkState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("anger", 0.0F).forGetter(RedHulkState::anger),
            Codec.LONG.optionalFieldOf("last_fight", 0L).forGetter(RedHulkState::lastFight),
            Codec.LONG.optionalFieldOf("absorb_until", 0L).forGetter(RedHulkState::absorbUntil),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(RedHulkState::flags)
    ).apply(i, RedHulkState::new));

    public static final StreamCodec<ByteBuf, RedHulkState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RedHulkState::anger,
            ByteBufCodecs.VAR_LONG, RedHulkState::lastFight,
            ByteBufCodecs.VAR_LONG, RedHulkState::absorbUntil,
            ByteBufCodecs.VAR_INT, RedHulkState::flags,
            RedHulkState::new);

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public RedHulkState withAnger(float value, long fight) {
        return new RedHulkState(value, fight, absorbUntil, flags);
    }

    public RedHulkState withAbsorbUntil(long time) {
        return new RedHulkState(anger, lastFight, time, flags);
    }

    public RedHulkState with(int flag, boolean on) {
        return new RedHulkState(anger, lastFight, absorbUntil, on ? flags | flag : flags & ~flag);
    }
}
