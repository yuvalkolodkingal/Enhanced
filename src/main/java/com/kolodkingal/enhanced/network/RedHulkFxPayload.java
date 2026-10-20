package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.redhulk.RedHulkFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.8): a moment of Red Hulk everyone near should see (lane L2's): the smash, the grab, its slams and
 * throw, the heat wave and the roar, the jump's charge, launch and landing, the Anger Burst, the absorb, the change to
 * Ross and back. The client hands each kind to the drawer lane L2 registered ({@code RedHulkFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId Red Hulk, or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record RedHulkFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (an effect of ten points stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int SMASH = 0;
    public static final int GRAB = 1;
    public static final int SLAM = 2;
    public static final int THROW = 3;
    public static final int HEAT_WAVE = 4;
    public static final int ROAR = 5;
    public static final int JUMP_CHARGE = 6;
    public static final int JUMP = 7;
    public static final int LAND = 8;
    public static final int BURST = 9;
    public static final int ABSORB_ON = 10;
    public static final int ABSORB_HIT = 11;
    public static final int ABSORB_OFF = 12;
    public static final int TRANSFORM_IN = 13;
    public static final int TRANSFORM_OUT = 14;

    public static final Type<RedHulkFxPayload> TYPE = new Type<>(Enhanced.id("red_hulk_fx"));

    public static final StreamCodec<ByteBuf, RedHulkFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RedHulkFxPayload::kind,
            ByteBufCodecs.VAR_INT, RedHulkFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), RedHulkFxPayload::points,
            ByteBufCodecs.VAR_INT, RedHulkFxPayload::arg,
            RedHulkFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(RedHulkFxPayload payload, IPayloadContext context) {
        RedHulkFx.receive(payload);
    }
}
