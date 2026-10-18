package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.music.ServerMusicClient;
import com.kolodkingal.enhanced.music.MusicFiles;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client (3.5, optional): one piece of a song the client asked for: the song's SHA-256, the piece's number, how many
 * pieces there are, the song's size, and at most {@link MusicFiles#CHUNK_BYTES} bytes of it. The client puts the pieces
 * together, checks the hash and keeps the song in config/enhanced/music_cache/{@code <hash>}.ogg.
 */
public record MusicChunkPayload(String hash, int index, int total, int size, byte[] data) implements CustomPacketPayload {
    public static final Type<MusicChunkPayload> TYPE = new Type<>(Enhanced.id("music_chunk"));

    public static final StreamCodec<ByteBuf, MusicChunkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), MusicChunkPayload::hash,
            ByteBufCodecs.VAR_INT, MusicChunkPayload::index,
            ByteBufCodecs.VAR_INT, MusicChunkPayload::total,
            ByteBufCodecs.VAR_INT, MusicChunkPayload::size,
            ByteBufCodecs.byteArray(MusicFiles.CHUNK_BYTES), MusicChunkPayload::data,
            MusicChunkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(MusicChunkPayload payload, IPayloadContext context) {
        ServerMusicClient.onChunk(payload.hash(), payload.index(), payload.total(), payload.size(), payload.data());
    }
}
