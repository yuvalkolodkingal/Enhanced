package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.screen.WitchesRoadScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: a Witches' Road door opened for this seeker; show the "Seekest thou the Road?" screen. */
public record OpenRoadPayload(BlockPos door) implements CustomPacketPayload {
    public static final Type<OpenRoadPayload> TYPE = new Type<>(Enhanced.id("open_road"));

    public static final StreamCodec<ByteBuf, OpenRoadPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenRoadPayload::door,
            OpenRoadPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the screen class is only ever loaded there. */
    static void handle(OpenRoadPayload payload, IPayloadContext context) {
        WitchesRoadScreen.open(payload.door());
    }
}
