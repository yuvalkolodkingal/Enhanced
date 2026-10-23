package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: a continuous beam (3.2, see {@code ContinuousBeam}) is fired by this player, or has stopped. The
 * server sends it when the beam starts, every few ticks while it lasts (so a client that comes into range later, or
 * misses one, still draws it) and when it stops; a client lets a beam lapse if it hears nothing for a moment. The
 * client works out where the beam ends by itself (it looks along the shooter's view, as the server does), so the
 * payload says only whose it is, in which style and how far it reaches.
 *
 * @param shooter entity id of the one who fires
 * @param style   the id of its {@code BeamStyle}
 * @param range   blocks
 * @param on      false: the beam stopped
 */
public record BeamPayload(int shooter, String style, float range, boolean on) implements CustomPacketPayload {
    public static final Type<BeamPayload> TYPE = new Type<>(Enhanced.id("beam"));

    public static final StreamCodec<ByteBuf, BeamPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BeamPayload::shooter,
            ByteBufCodecs.STRING_UTF8, BeamPayload::style,
            ByteBufCodecs.FLOAT, BeamPayload::range,
            ByteBufCodecs.BOOL, BeamPayload::on,
            BeamPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the renderer class is only ever loaded there. */
    static void handle(BeamPayload payload, PacketContext context) {
        BeamRenderer.update(payload);
    }
}
