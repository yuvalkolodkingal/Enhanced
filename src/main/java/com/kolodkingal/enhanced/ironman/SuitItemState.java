package com.kolodkingal.enhanced.ironman;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A suit item's own state (3.8): the data component {@code enhanced:suit} (saved with the item, sent to clients: the
 * item property {@code enhanced:folded} picks the Mark 5's suitcase model). Written by the 3.8 skeleton; lane L3
 * drives it.
 *
 * @param folded the Mark 5 is folded into its suitcase (in the hand); false: unfolded (worn)
 */
public record SuitItemState(boolean folded) {
    public static final SuitItemState FOLDED = new SuitItemState(true);
    public static final SuitItemState UNFOLDED = new SuitItemState(false);

    public static final Codec<SuitItemState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("folded", true).forGetter(SuitItemState::folded)
    ).apply(i, SuitItemState::new));

    public static final StreamCodec<ByteBuf, SuitItemState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SuitItemState::folded,
            SuitItemState::new);
}
