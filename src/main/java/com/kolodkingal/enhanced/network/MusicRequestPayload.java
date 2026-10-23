package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.music.ServerMusic;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server (3.5, optional): "send me the song with this SHA-256". The server answers only for a song its manifest
 * lists, one song at a time per player, in {@link MusicChunkPayload}s at its own pace.
 */
public record MusicRequestPayload(String hash) implements CustomPacketPayload {
    public static final Type<MusicRequestPayload> TYPE = new Type<>(Enhanced.id("music_request"));

    public static final StreamCodec<ByteBuf, MusicRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), MusicRequestPayload::hash,
            MusicRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(MusicRequestPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ServerMusic.request(player, payload.hash());
        }
    }
}
