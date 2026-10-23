package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.tower.TowerFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client (3.8): a moment of the Avengers Tower's armoury (lane L6's; the gantry's arms lane L7's): a suit
 * taken from a display or refused, a tech case opening and closing, the Mark 3's gantry bolting a suit on (arg: the
 * player's id). The client hands each kind to the drawer its lane registered ({@code TowerFx.register}).
 *
 * @param kind what happened (a constant of this record)
 * @param pos  the display or case
 * @param arg  a number (the meaning depends on the kind)
 */
public record TowerFxPayload(int kind, BlockPos pos, int arg) implements CustomPacketPayload {
    public static final int DISPLAY_TAKE = 0;
    public static final int DISPLAY_REFUSE = 1;
    public static final int CASE_OPEN = 2;
    public static final int CASE_CLOSE = 3;
    public static final int GANTRY = 4;

    public static final Type<TowerFxPayload> TYPE = new Type<>(Enhanced.id("tower_fx"));

    public static final StreamCodec<ByteBuf, TowerFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TowerFxPayload::kind,
            PayloadCodecs.BLOCK_POS, TowerFxPayload::pos,
            ByteBufCodecs.VAR_INT, TowerFxPayload::arg,
            TowerFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(TowerFxPayload payload, PacketContext context) {
        TowerFx.receive(payload);
    }
}
