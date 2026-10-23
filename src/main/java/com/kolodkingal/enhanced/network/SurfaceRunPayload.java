package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SpeedSurfaces;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server: what the local speedster (Quicksilver or Tommy) runs on, if anything (3.3). His own client moves
 * him up the wall or along the ceiling, and from his positions alone the server cannot tell a run up a wall from a
 * jump, nor a run down one from a fall. So the client says it: when the run begins, changes or ends, and again every
 * few ticks while it lasts. The server never takes it on trust (see {@link SpeedSurfaces#onReport} and its tick).
 *
 * @param kind   0 nothing, 1 a wall, 2 a ceiling
 * @param normal the direction the surface faces (from the block towards him), as its 3D data value
 */
public record SurfaceRunPayload(int kind, int normal) implements CustomPacketPayload {
    public static final int NONE = 0;
    public static final int WALL = 1;
    public static final int CEILING = 2;

    public static final Type<SurfaceRunPayload> TYPE = new Type<>(Enhanced.id("surface_run"));

    public static final StreamCodec<ByteBuf, SurfaceRunPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SurfaceRunPayload::kind,
            ByteBufCodecs.VAR_INT, SurfaceRunPayload::normal,
            SurfaceRunPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SurfaceRunPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SpeedSurfaces.onReport(player, payload.kind(), payload.normal());
        }
    }
}
