package com.kolodkingal.enhanced.platform.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/** The loader's side of {@link Net} (SPI: gameplay code calls {@link Net}). */
public interface NetService {
    /** Declares a channel: the loader registers its messages when its registration time comes. */
    void declare(Net.Channel channel);

    void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads);

    void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... payloads);

    void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z, double radius,
                           CustomPacketPayload payload, CustomPacketPayload... payloads);

    void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads);

    void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads);

    void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads);

    void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload,
                                    CustomPacketPayload... payloads);
}
