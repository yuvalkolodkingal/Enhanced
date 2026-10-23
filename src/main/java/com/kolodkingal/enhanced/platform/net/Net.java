package com.kolodkingal.enhanced.platform.net;

import com.kolodkingal.enhanced.platform.Platform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The mod's messages (port P0, PLAN2 3.3): declaring them on a {@link Channel}, and sending them. The send methods have
 * the names and arguments of NeoForge's {@code PacketDistributor}, so a call site changes only its class name; on
 * NeoForge they are that class's methods.
 */
public final class Net {
    /** What a message does when it arrives (on the game thread of the side that received it). */
    @FunctionalInterface
    public interface Handler<T> {
        void handle(T payload, PacketContext context);
    }

    /** One message of a channel. */
    public record Message<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
                                                          StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                          Handler<T> handler, boolean clientbound) {
    }

    /**
     * A set of messages with a version: a client and a server whose versions differ cannot play together, unless the
     * channel is optional (then the server only sends its messages to a client that has it).
     */
    public static final class Channel {
        private final String version;
        private final boolean optional;
        private final List<Message<?>> messages = new ArrayList<>();

        private Channel(String version, boolean optional) {
            this.version = version;
            this.optional = optional;
        }

        public String version() {
            return version;
        }

        public boolean optional() {
            return optional;
        }

        /** The messages in the order they were declared. */
        public List<Message<?>> messages() {
            return Collections.unmodifiableList(messages);
        }

        /** A message the server sends to clients. */
        public <T extends CustomPacketPayload> Channel clientbound(CustomPacketPayload.Type<T> type,
                                                                   StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                   Handler<T> handler) {
            messages.add(new Message<>(type, codec, handler, true));
            return this;
        }

        /** A message clients send to the server. */
        public <T extends CustomPacketPayload> Channel serverbound(CustomPacketPayload.Type<T> type,
                                                                   StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                   Handler<T> handler) {
            messages.add(new Message<>(type, codec, handler, false));
            return this;
        }
    }

    /** A new channel of required messages with this version (declare its messages before the loader registers them). */
    public static Channel channel(String version) {
        Channel channel = new Channel(version, false);
        service().declare(channel);
        return channel;
    }

    /** A new optional channel: a peer without it still plays; its messages are only sent to peers that have it. */
    public static Channel optionalChannel(String version) {
        Channel channel = new Channel(version, true);
        service().declare(channel);
        return channel;
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        service().sendToServer(payload, payloads);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        service().sendToPlayer(player, payload, payloads);
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload,
                                                CustomPacketPayload... payloads) {
        service().sendToPlayersInDimension(level, payload, payloads);
    }

    public static void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z,
                                         double radius, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        service().sendToPlayersNear(level, excluded, x, y, z, radius, payload, payloads);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        service().sendToAllPlayers(payload, payloads);
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload,
                                                   CustomPacketPayload... payloads) {
        service().sendToPlayersTrackingEntity(entity, payload, payloads);
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload,
                                                          CustomPacketPayload... payloads) {
        service().sendToPlayersTrackingEntityAndSelf(entity, payload, payloads);
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload,
                                                  CustomPacketPayload... payloads) {
        service().sendToPlayersTrackingChunk(level, chunkPos, payload, payloads);
    }

    private static NetService service() {
        return Platform.get().net();
    }

    private Net() {
    }
}
