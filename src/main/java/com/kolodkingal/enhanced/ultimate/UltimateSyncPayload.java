package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client (3.17): whether Ultimate Mode is on, sent to a player who logs in and to everyone when it changes, so
 * the client draws and predicts the ultimate versions (Solar Jewel Vision, the Demiurge) as the server has them.
 */
public record UltimateSyncPayload(boolean on) implements CustomPacketPayload {
    public static final Type<UltimateSyncPayload> TYPE = new Type<>(Enhanced.id("ultimate_sync"));
    public static final StreamCodec<ByteBuf, UltimateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, UltimateSyncPayload::on,
            UltimateSyncPayload::new);

    /** The setting as it is now. */
    public static UltimateSyncPayload now() {
        return new UltimateSyncPayload(Ultimate.on());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UltimateSyncPayload payload, PacketContext context) {
        Ultimate.set(payload.on());
    }
}
