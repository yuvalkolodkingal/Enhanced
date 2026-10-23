package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.XavierControl;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, to Professor X alone (3.3): open the screen that chooses his version ({@link #CHOOSE}), his mind has
 * entered a body ({@link #LINK_START}: his camera goes to its eyes), has left it ({@link #LINK_END}), or a stronger
 * telepath has thrown him out ({@link #THROWN_OUT}).
 *
 * @param kind     one of the constants
 * @param entityId the body he entered ({@link #LINK_START}), else -1
 * @param ticks    how long he may stay in it ({@link #LINK_START}), else 0
 */
public record XavierPayload(int kind, int entityId, int ticks) implements CustomPacketPayload {
    public static final int CHOOSE = 0;
    public static final int LINK_START = 1;
    public static final int LINK_END = 2;
    public static final int THROWN_OUT = 3;

    public static final Type<XavierPayload> TYPE = new Type<>(Enhanced.id("xavier"));

    public static final StreamCodec<ByteBuf, XavierPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, XavierPayload::kind,
            ByteBufCodecs.VAR_INT, XavierPayload::entityId,
            ByteBufCodecs.VAR_INT, XavierPayload::ticks,
            XavierPayload::new);

    public static XavierPayload of(int kind) {
        return new XavierPayload(kind, -1, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the client class is only ever loaded there. */
    static void handle(XavierPayload payload, PacketContext context) {
        XavierControl.handle(payload.kind(), payload.entityId(), payload.ticks());
    }
}
