package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.captain.CaptainFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.8): a moment of Captain America everyone near should see (lane L1's): the shield thrown and
 * caught, a ricochet, a block and a reflection, the slam and the leap, the charge and the bash, Assemble and Inspire,
 * the combo's punches and kick, the flip. The client hands each kind to the drawer lane L1 registered ({@code
 * CaptainFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId Captain America (or his thrown shield), or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record CaptainFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (an effect of ten points stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int THROW = 0;
    public static final int CATCH = 1;
    public static final int RICOCHET = 2;
    public static final int BLOCK = 3;
    public static final int REFLECT = 4;
    public static final int SLAM = 5;
    public static final int LEAP = 6;
    public static final int CHARGE = 7;
    public static final int BASH = 8;
    public static final int ASSEMBLE = 9;
    public static final int INSPIRE = 10;
    public static final int PUNCH = 11;
    public static final int KICK = 12;
    public static final int FLIP = 13;

    public static final Type<CaptainFxPayload> TYPE = new Type<>(Enhanced.id("captain_fx"));

    public static final StreamCodec<ByteBuf, CaptainFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CaptainFxPayload::kind,
            ByteBufCodecs.VAR_INT, CaptainFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), CaptainFxPayload::points,
            ByteBufCodecs.VAR_INT, CaptainFxPayload::arg,
            CaptainFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(CaptainFxPayload payload, IPayloadContext context) {
        CaptainFx.receive(payload);
    }
}
