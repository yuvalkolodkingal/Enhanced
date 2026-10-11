package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client, to Spider-Man alone: his spider-sense tingles. The client outlines these threats through walls for a
 * moment and draws the ripples on his screen.
 *
 * @param ids    the entities that threaten him (at most 64)
 * @param strong whether a hit is less than 12 ticks away
 */
public record SpiderSensePayload(List<Integer> ids, boolean strong) implements CustomPacketPayload {
    public static final Type<SpiderSensePayload> TYPE = new Type<>(Enhanced.id("spider_sense"));

    public static final StreamCodec<ByteBuf, SpiderSensePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), SpiderSensePayload::ids,
            ByteBufCodecs.BOOL, SpiderSensePayload::strong,
            SpiderSensePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The Spider-Man lane shows the warning here. */
    static void handle(SpiderSensePayload payload, IPayloadContext context) {
    }
}
