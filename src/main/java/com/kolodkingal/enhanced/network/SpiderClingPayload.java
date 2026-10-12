package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SpiderServer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: what the local Spider-Man clings to, if anything. His own client moves him along the wall or the
 * ceiling, and vanilla sends the server nothing but his positions, from which the server cannot tell a climb, a crawl
 * down a wall and a fall apart. So the client says it: when the cling begins, changes or ends, and again every few ticks
 * while it lasts. The server never takes it on trust: it only believes a cling with a block within reach of that side of
 * his body (see {@link SpiderServer#onClingReport}).
 *
 * @param kind   0 nothing, 1 a wall, 2 a ceiling
 * @param normal the direction the surface faces (from the block towards him), as its 3D data value
 */
public record SpiderClingPayload(int kind, int normal) implements CustomPacketPayload {
    public static final int NONE = 0;
    public static final int WALL = 1;
    public static final int CEILING = 2;

    public static final Type<SpiderClingPayload> TYPE = new Type<>(Enhanced.id("spider_cling"));

    public static final StreamCodec<ByteBuf, SpiderClingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpiderClingPayload::kind,
            ByteBufCodecs.VAR_INT, SpiderClingPayload::normal,
            SpiderClingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SpiderClingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SpiderServer.onClingReport(player, payload.kind(), payload.normal());
        }
    }
}
