package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.hulk.HulkFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → client (3.6): a moment of the Hulk everyone near should see (lane L7's): the transformation, the Smash, the
 * Grab Smash, the Clap, the Roar, the leaps, the boulder, the Gamma Charge, the Gamma Emitter's beam. The client hands
 * each kind to the drawer lane L7 registered ({@code HulkFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId the Hulk (or -1 for the Gamma Emitter), or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record HulkFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (the payload stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int TRANSFORM_OUT = 0;
    public static final int TRANSFORM_IN = 1;
    public static final int SWITCH = 2;
    public static final int SMASH = 3;
    public static final int GRAB_SLAM = 4;
    public static final int CLAP = 5;
    public static final int ROAR = 6;
    public static final int LEAP = 7;
    public static final int LAND = 8;
    public static final int BOUND = 9;
    public static final int RIP = 10;
    public static final int BOULDER_HIT = 11;
    public static final int CHARGE = 12;
    public static final int GAMMA_BEAM = 13;

    public static final Type<HulkFxPayload> TYPE = new Type<>(Enhanced.id("hulk_fx"));

    public static final StreamCodec<ByteBuf, HulkFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HulkFxPayload::kind,
            ByteBufCodecs.VAR_INT, HulkFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), HulkFxPayload::points,
            ByteBufCodecs.VAR_INT, HulkFxPayload::arg,
            HulkFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HulkFxPayload payload, PacketContext context) {
        HulkFx.receive(payload);
    }
}
