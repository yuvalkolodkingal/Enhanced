package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.deadpool.DeadpoolFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client (3.7): a moment of Deadpool everyone near should see (lane L3's): the katanas, the Parry and a
 * deflection, a shot and its tracer, the reload, a ricochet, the belt's blink out and in and its glitch, the Drop-In,
 * the Taunt (arg: the line's index), the smoke, Dogpool coming and going, a chimichanga eaten. The client hands each
 * kind to the drawer lane L3 registered ({@code DeadpoolFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId Deadpool (or Dogpool), or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record DeadpoolFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (the payload stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int SLASH = 0;
    public static final int PARRY_ON = 1;
    public static final int PARRY_OFF = 2;
    public static final int DEFLECT = 3;
    public static final int SHOT = 4;
    public static final int RELOAD = 5;
    public static final int RICOCHET = 6;
    public static final int BLINK_OUT = 7;
    public static final int BLINK_IN = 8;
    public static final int GLITCH = 9;
    public static final int DROP_IN = 10;
    public static final int TAUNT = 11;
    public static final int SMOKE = 12;
    public static final int DOGPOOL_IN = 13;
    public static final int DOGPOOL_OUT = 14;
    public static final int EAT = 15;

    public static final Type<DeadpoolFxPayload> TYPE = new Type<>(Enhanced.id("deadpool_fx"));

    public static final StreamCodec<ByteBuf, DeadpoolFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeadpoolFxPayload::kind,
            ByteBufCodecs.VAR_INT, DeadpoolFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), DeadpoolFxPayload::points,
            ByteBufCodecs.VAR_INT, DeadpoolFxPayload::arg,
            DeadpoolFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(DeadpoolFxPayload payload, IPayloadContext context) {
        DeadpoolFx.receive(payload);
    }
}
