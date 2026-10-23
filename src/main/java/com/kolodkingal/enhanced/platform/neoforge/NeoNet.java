package com.kolodkingal.enhanced.platform.neoforge;

import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.platform.net.NetService;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's messages on NeoForge: each declared {@link Net.Channel} becomes a {@link PayloadRegistrar} of its version
 * (optional or not) when NeoForge asks for payloads; sending is {@link PacketDistributor}.
 */
final class NeoNet implements NetService {
    private final List<Net.Channel> channels = new ArrayList<>();
    private boolean registered;

    @Override
    public synchronized void declare(Net.Channel channel) {
        if (registered) {
            throw new IllegalStateException("Channel " + channel.version() + " declared after the payloads were registered");
        }
        channels.add(channel);
    }

    /** Registers every declared message, in the order they were declared (the mod bus's payload event). */
    synchronized void register(RegisterPayloadHandlersEvent event) {
        registered = true;
        for (Net.Channel channel : channels) {
            PayloadRegistrar registrar = event.registrar(channel.version());
            if (channel.optional()) {
                registrar = registrar.optional();
            }
            for (Net.Message<?> message : channel.messages()) {
                register(registrar, message);
            }
        }
    }

    private static <T extends CustomPacketPayload> void register(PayloadRegistrar registrar, Net.Message<T> message) {
        Net.Handler<T> handler = message.handler();
        if (message.clientbound()) {
            registrar.playToClient(message.type(), message.codec(), (payload, context) ->
                    handler.handle(payload, new Context(context)));
        } else {
            registrar.playToServer(message.type(), message.codec(), (payload, context) ->
                    handler.handle(payload, new Context(context)));
        }
    }

    /** NeoForge's payload context as the mod's. */
    record Context(IPayloadContext context) implements PacketContext {
        @Override
        public Player player() {
            return context.player();
        }

        @Override
        public void enqueueWork(Runnable work) {
            context.enqueueWork(work);
        }

        @Override
        public boolean isClientbound() {
            return context.flow().isClientbound();
        }

        @Override
        public void disconnect(Component reason) {
            context.disconnect(reason);
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.sendToServer(payload, payloads);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.sendToPlayer(player, payload, payloads);
    }

    @Override
    public void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.sendToPlayersInDimension(level, payload, payloads);
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z,
                                  double radius, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.sendToPlayersNear(level, excluded, x, y, z, radius, payload, payloads);
    }

    @Override
    public void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.sendToAllPlayers(payload, payloads);
    }

    @Override
    public void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload, payloads);
    }

    @Override
    public void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload,
                                                   CustomPacketPayload... payloads) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload, payloads);
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload,
                                           CustomPacketPayload... payloads) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, payload, payloads);
    }
}
