package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.music.ServerMusicClient;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server → client (3.5, optional): the songs in the world's music folder ({@code <world>/serverconfig/enhanced/music}), each
 * by the name of the music it replaces ({@code quicksilver}, {@code speed}, {@code witches_road}, {@code witches_road_<stage>},
 * {@code music}), its SHA-256 and its size. Sent at login, after /reload and after {@code /enhanced music reload}; an empty
 * list means: the built-in music. A client that lacks a song asks for it ({@link MusicRequestPayload}).
 */
public record MusicManifestPayload(List<Track> tracks) implements CustomPacketPayload {
    public static final Type<MusicManifestPayload> TYPE = new Type<>(Enhanced.id("music_manifest"));
    /** The most tracks one manifest lists (the Road's versions included). */
    public static final int MAX_TRACKS = 64;

    /** One song: the name of the music it replaces, its SHA-256 (hex) and its size in bytes. */
    public record Track(String name, String hash, int size) {
        public static final StreamCodec<ByteBuf, Track> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Track::name,
                ByteBufCodecs.stringUtf8(64), Track::hash,
                ByteBufCodecs.VAR_INT, Track::size,
                Track::new);
    }

    public static final StreamCodec<ByteBuf, MusicManifestPayload> STREAM_CODEC = StreamCodec.composite(
            Track.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TRACKS)), MusicManifestPayload::tracks,
            MusicManifestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the client class is only ever loaded there. */
    static void handle(MusicManifestPayload payload, PacketContext context) {
        ServerMusicClient.onManifest(payload.tracks());
    }
}
