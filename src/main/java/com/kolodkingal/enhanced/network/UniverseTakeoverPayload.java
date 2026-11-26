package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.UniverseTakeoverClient;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client (3.19): what the universe the player stands in is now, sent only when it changes
 * ({@code UltimateWanda.everyPlayerTick}): free, taken by Ultimate Wanda (its sky red), or taken with her runes burning.
 */
public record UniverseTakeoverPayload(int state) implements CustomPacketPayload {
    public static final int CLEAR = 0;
    public static final int RED = 1;
    public static final int RUNES = 2;

    public static final Type<UniverseTakeoverPayload> TYPE = new Type<>(Enhanced.id("universe_takeover"));

    public static final StreamCodec<ByteBuf, UniverseTakeoverPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UniverseTakeoverPayload::state,
            UniverseTakeoverPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(UniverseTakeoverPayload payload, PacketContext context) {
        UniverseTakeoverClient.accept(payload.state());
    }
}
