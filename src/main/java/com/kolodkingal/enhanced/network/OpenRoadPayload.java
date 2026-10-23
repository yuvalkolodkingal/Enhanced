package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.screen.WitchesRoadScreen;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: a Witches' Road door opened for this seeker; show the "Seekest thou the Road?" screen.
 *
 * @param trials whether the screen asks him to walk the Road (the trials) rather than to choose Billy or Tommy
 * @param done   the trials he has won
 * @param total  the trials there are
 * @param ending whether he is at the end of the Road, in the Road itself ({@code door} is the door of the hall): the choice of Billy or
 *               Tommy, with a button to leave the Road without choosing
 */
public record OpenRoadPayload(BlockPos door, boolean trials, int done, int total, boolean ending) implements CustomPacketPayload {
    public static final Type<OpenRoadPayload> TYPE = new Type<>(Enhanced.id("open_road"));

    public static final StreamCodec<ByteBuf, OpenRoadPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenRoadPayload::door,
            ByteBufCodecs.BOOL, OpenRoadPayload::trials,
            ByteBufCodecs.VAR_INT, OpenRoadPayload::done,
            ByteBufCodecs.VAR_INT, OpenRoadPayload::total,
            ByteBufCodecs.BOOL, OpenRoadPayload::ending,
            OpenRoadPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the screen class is only ever loaded there. */
    static void handle(OpenRoadPayload payload, PacketContext context) {
        WitchesRoadScreen.open(payload.door(), payload.trials(), payload.done(), payload.total(), payload.ending());
    }
}
