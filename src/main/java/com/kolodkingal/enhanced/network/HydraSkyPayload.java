package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.world.WhatIfSky;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client (3.19.1): whether the city of What If... Hydra Won? is free (its smog lifts). */
public record HydraSkyPayload(boolean freed) implements CustomPacketPayload {
    public static final Type<HydraSkyPayload> TYPE = new Type<>(Enhanced.id("hydra_sky"));

    public static final StreamCodec<ByteBuf, HydraSkyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HydraSkyPayload::freed,
            HydraSkyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HydraSkyPayload payload, PacketContext context) {
        WhatIfSky.hydraFreed(payload.freed());
    }
}
