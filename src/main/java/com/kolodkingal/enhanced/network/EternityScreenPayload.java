package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.eternity.EternityFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.8): Eternity speaks; open the wish screen (lane L8's). The server checks every wish again ({@link
 * EternityWishPayload}): the locks are only what the picker shows.
 *
 * @param mayWish     whether he has a wish left ({@code [eternity] wishesPerPlayer})
 * @param loveAllowed whether "Become Love" is offered ({@code [eternity] loveWish})
 * @param locks       the items the picker shows locked and why (the stones' ledger, Gungnir, Mjolnir), at most
 *                    {@link #MAX_LOCKS}
 */
public record EternityScreenPayload(boolean mayWish, boolean loveAllowed, List<Lock> locks)
        implements CustomPacketPayload {
    public static final int MAX_LOCKS = 64;

    public static final Type<EternityScreenPayload> TYPE = new Type<>(Enhanced.id("eternity_screen"));

    public static final StreamCodec<ByteBuf, EternityScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EternityScreenPayload::mayWish,
            ByteBufCodecs.BOOL, EternityScreenPayload::loveAllowed,
            Lock.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LOCKS)), EternityScreenPayload::locks,
            EternityScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the screen classes are only ever loaded there. */
    static void handle(EternityScreenPayload payload, IPayloadContext context) {
        EternityFx.open(payload);
    }

    /**
     * An item the picker shows locked.
     *
     * @param item      the item's id
     * @param reasonKey the lang key of why (at most 64 characters)
     */
    public record Lock(ResourceLocation item, String reasonKey) {
        public static final StreamCodec<ByteBuf, Lock> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Lock::item,
                ByteBufCodecs.stringUtf8(64), Lock::reasonKey,
                Lock::new);
    }
}
