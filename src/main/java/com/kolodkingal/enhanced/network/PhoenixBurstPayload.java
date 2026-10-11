package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: the Phoenix Burst goes off, for everyone nearby to see (the firebird, the shell of fire, the
 * scorch). The client recomputes the shell's radius at each tick from {@code start}, {@code radius} and the config's
 * speed.
 *
 * @param caster the Phoenix's entity id
 * @param centre where the Burst is centred
 * @param yaw    the way she faces (the firebird flies out along it)
 * @param charge how charged it was, 0 to 1
 * @param radius how far the shell reaches
 * @param start  the game time it went off
 */
public record PhoenixBurstPayload(int caster, Vec3 centre, float yaw, float charge, float radius, long start)
        implements CustomPacketPayload {
    public static final Type<PhoenixBurstPayload> TYPE = new Type<>(Enhanced.id("phoenix_burst"));

    public static final StreamCodec<ByteBuf, PhoenixBurstPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PhoenixBurstPayload::caster,
            PayloadCodecs.VEC3, PhoenixBurstPayload::centre,
            ByteBufCodecs.FLOAT, PhoenixBurstPayload::yaw,
            ByteBufCodecs.FLOAT, PhoenixBurstPayload::charge,
            ByteBufCodecs.FLOAT, PhoenixBurstPayload::radius,
            ByteBufCodecs.VAR_LONG, PhoenixBurstPayload::start,
            PhoenixBurstPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The Phoenix lane draws the Burst here. */
    static void handle(PhoenixBurstPayload payload, IPayloadContext context) {
    }
}
