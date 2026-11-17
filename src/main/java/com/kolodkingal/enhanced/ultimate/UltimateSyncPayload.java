package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client (3.17): whether Ultimate Mode is on, sent to a player who logs in and to everyone when it changes, so
 * the client draws and predicts the ultimate versions (Solar Jewel Vision, the Demiurge) as the server has them; and the
 * worlds the Nexus Being holds now ({@link NexusBeing}), whose skies turn red with her runes.
 *
 * @param on    Ultimate Mode
 * @param nexus the ids of the dimensions a Nexus Being holds
 */
public record UltimateSyncPayload(boolean on, java.util.List<String> nexus) implements CustomPacketPayload {
    public static final Type<UltimateSyncPayload> TYPE = new Type<>(Enhanced.id("ultimate_sync"));
    public static final StreamCodec<ByteBuf, UltimateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, UltimateSyncPayload::on,
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(64)), UltimateSyncPayload::nexus,
            UltimateSyncPayload::new);

    /** The setting as it is now. */
    public static UltimateSyncPayload now() {
        return new UltimateSyncPayload(Ultimate.on(), NexusBeing.heldIds());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UltimateSyncPayload payload, PacketContext context) {
        Ultimate.set(payload.on());
        NexusBeing.setHeldOnClient(payload.nexus());
    }
}
