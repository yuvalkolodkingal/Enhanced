package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.multiverse.UniverseTravel;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Client → server (3.19.1): the name typed on the Multiverse screen; the server makes the universe of those words. */
public record UniverseNamePayload(String name) implements CustomPacketPayload {
    public static final int MAX = 64;
    public static final Type<UniverseNamePayload> TYPE = new Type<>(Enhanced.id("universe_name"));

    public static final StreamCodec<ByteBuf, UniverseNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX), UniverseNamePayload::name,
            UniverseNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(UniverseNamePayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive()) {
            UniverseTravel.chooseNamed(player, payload.name());
        }
    }
}
