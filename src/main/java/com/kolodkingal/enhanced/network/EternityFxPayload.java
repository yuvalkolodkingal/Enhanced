package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.eternity.EternityFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client (3.8): a moment of Eternity and Love (lane L8's): the altar charging and the journey, the arrival, a
 * wish granted, the way home, Love awakening, her Thunder Strike, her eyes lighting up and going out. The client hands
 * each kind to the drawer lane L8 registered ({@code EternityFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId the player (or Eternity), or -1
 * @param pos      the altar, or where it happened
 * @param arg      a number (the meaning depends on the kind)
 */
public record EternityFxPayload(int kind, int entityId, BlockPos pos, int arg) implements CustomPacketPayload {
    public static final int ALTAR_CHARGE = 0;
    public static final int ALTAR_GO = 1;
    public static final int ARRIVE = 2;
    public static final int WISH_GRANTED = 3;
    public static final int LEAVE = 4;
    public static final int LOVE_AWAKEN = 5;
    public static final int THUNDER_STRIKE = 6;
    public static final int EYES_ON = 7;
    public static final int EYES_OFF = 8;

    public static final Type<EternityFxPayload> TYPE = new Type<>(Enhanced.id("eternity_fx"));

    public static final StreamCodec<ByteBuf, EternityFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EternityFxPayload::kind,
            ByteBufCodecs.VAR_INT, EternityFxPayload::entityId,
            PayloadCodecs.BLOCK_POS, EternityFxPayload::pos,
            ByteBufCodecs.VAR_INT, EternityFxPayload::arg,
            EternityFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(EternityFxPayload payload, IPayloadContext context) {
        EternityFx.receive(payload);
    }
}
