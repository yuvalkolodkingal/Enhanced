package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

/**
 * The handshake of the mod's messages (port P0, PLAN2 3.3): before a player joins, client and server compare the
 * versions of their channels, {@link ModNetwork#PROTOCOL_VERSION} (required: different versions cannot play together)
 * and {@link ModNetwork#MUSIC_VERSION} (optional: different or missing, the player plays and hears the built-in
 * music).
 *
 * <p>On NeoForge the loader does this itself (each channel is a {@code PayloadRegistrar} of its version) and this
 * message is not sent. A loader without a negotiation of its own (Fabric) sends {@code enhanced:hello} both ways in the
 * configuration phase and asks {@link #check} what to do, so a refused player reads the very words NeoForge shows.
 *
 * @param protocol the version of the required channel
 * @param music    the version of the music channel, or {@code ""} when the side has none
 */
public record HelloPayload(String protocol, String music) implements CustomPacketPayload {
    public static final Type<HelloPayload> TYPE = new Type<>(Enhanced.id("hello"));
    /** A version is short; a longer one is refused when read (the peer is not trusted). */
    static final int MAX_VERSION = 32;
    public static final StreamCodec<ByteBuf, HelloPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_VERSION), HelloPayload::protocol,
            ByteBufCodecs.stringUtf8(MAX_VERSION), HelloPayload::music,
            HelloPayload::new);
    /** The name NeoForge puts before a channel's failure (the mod's display name). */
    private static final String MOD_NAME = "Enhanced";

    /** This side's versions. */
    public static HelloPayload local() {
        return new HelloPayload(ModNetwork.PROTOCOL_VERSION, ModNetwork.MUSIC_VERSION);
    }

    @Override
    public Type<HelloPayload> type() {
        return TYPE;
    }

    /**
     * What the server makes of a client.
     *
     * @param refusal the reason the client is disconnected with, or null when it may join
     * @param music   whether the server may send it the music messages
     */
    public record Verdict(@Nullable Component refusal, boolean music) {
        public boolean accepted() {
            return refusal == null;
        }
    }

    /**
     * The server's verdict on a client that said {@code client} ({@code null}: it never answered, so it has no Enhanced
     * at all). The reasons are NeoForge's own translation keys, with its English words as the fallback.
     */
    public static Verdict check(HelloPayload server, @Nullable HelloPayload client) {
        if (client == null) {
            return new Verdict(failure(Component.translatableWithFallback(
                    "neoforge.network.negotiation.failure.missing.server.client",
                    "This channel is missing on the client side, but required on the server!")), false);
        }
        if (!client.protocol().equals(server.protocol())) {
            return new Verdict(failure(Component.translatableWithFallback(
                    "neoforge.network.negotiation.failure.version.mismatch",
                    "The client wants the payload to be version: %s, but the server wants it to be version: %s!",
                    client.protocol(), server.protocol())), false);
        }
        return new Verdict(null, !server.music().isEmpty() && server.music().equals(client.music()));
    }

    private static Component failure(Component reason) {
        return Component.translatableWithFallback("neoforge.network.negotiation.failure.mod",
                "Channel of mod \"%1$s\" failed to connect: %2$s", MOD_NAME, reason);
    }
}
