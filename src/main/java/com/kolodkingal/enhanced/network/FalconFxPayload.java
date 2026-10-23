package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.falcon.FalconFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → client (3.8): a moment of Falcon everyone near should see (lane L5's): the wrist blasters and rocket, the
 * dive and its hit, the climb, the wing shield and a deflection, the wing slash, Redwing out, in, zapping, striking
 * and marking. The client hands each kind to the drawer lane L5 registered ({@code FalconFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId Falcon (or Redwing), or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record FalconFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (an effect of ten points stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int BLAST = 0;
    public static final int ROCKET = 1;
    public static final int DIVE_START = 2;
    public static final int DIVE_HIT = 3;
    public static final int CLIMB = 4;
    public static final int SHIELD_ON = 5;
    public static final int SHIELD_OFF = 6;
    public static final int DEFLECT = 7;
    public static final int SLASH = 8;
    public static final int REDWING_OUT = 9;
    public static final int REDWING_IN = 10;
    public static final int REDWING_ZAP = 11;
    public static final int REDWING_STRIKE = 12;
    public static final int MARK = 13;

    public static final Type<FalconFxPayload> TYPE = new Type<>(Enhanced.id("falcon_fx"));

    public static final StreamCodec<ByteBuf, FalconFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FalconFxPayload::kind,
            ByteBufCodecs.VAR_INT, FalconFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), FalconFxPayload::points,
            ByteBufCodecs.VAR_INT, FalconFxPayload::arg,
            FalconFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(FalconFxPayload payload, PacketContext context) {
        FalconFx.receive(payload);
    }
}
