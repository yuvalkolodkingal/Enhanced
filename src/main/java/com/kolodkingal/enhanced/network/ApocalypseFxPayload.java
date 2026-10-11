package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: one effect of Apocalypse (the boss or the hero) that everyone nearby should see, the one small
 * payload that carries all of them (waves, spikes, tornadoes, rings, portals, the hits and the breaking of his shield,
 * his armour falling away, the arena beginning and ending). The client's effects class switches on {@code kind}.
 *
 * @param kind what to draw (the Apocalypse lane lists the kinds)
 * @param id   the entity (the boss, the hero, a hazard, a fight) it is about
 * @param a    the first position, or the origin
 * @param b    the second position, or a direction
 * @param c    the first number (ticks, a strength, a speed, a radius times ten: the kind decides)
 * @param d    the second number
 */
public record ApocalypseFxPayload(byte kind, int id, Vec3 a, Vec3 b, int c, int d) implements CustomPacketPayload {
    public static final Type<ApocalypseFxPayload> TYPE = new Type<>(Enhanced.id("apocalypse_fx"));

    public static final StreamCodec<ByteBuf, ApocalypseFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, ApocalypseFxPayload::kind,
            ByteBufCodecs.VAR_INT, ApocalypseFxPayload::id,
            PayloadCodecs.VEC3, ApocalypseFxPayload::a,
            PayloadCodecs.VEC3, ApocalypseFxPayload::b,
            ByteBufCodecs.VAR_INT, ApocalypseFxPayload::c,
            ByteBufCodecs.VAR_INT, ApocalypseFxPayload::d,
            ApocalypseFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The Apocalypse lane draws the effect here. */
    static void handle(ApocalypseFxPayload payload, IPayloadContext context) {
    }
}
