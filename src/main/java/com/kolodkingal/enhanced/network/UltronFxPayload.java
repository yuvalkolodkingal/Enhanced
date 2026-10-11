package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: one effect of Ultron (the boss or the hero) that everyone nearby should see: a tell decal on the
 * ground, a beam, a ring, the scrap that assembles his body.
 *
 * @param kind     what to draw (the Ultron lane lists the kinds)
 * @param entityId the caster, or -1
 * @param at       where it is, or where it starts
 * @param to       where it ends (a line), or zero
 * @param radius   its size
 * @param ticks    how long it lasts, or the time to impact
 */
public record UltronFxPayload(int kind, int entityId, Vec3 at, Vec3 to, float radius, int ticks) implements CustomPacketPayload {
    public static final Type<UltronFxPayload> TYPE = new Type<>(Enhanced.id("ultron_fx"));

    public static final StreamCodec<ByteBuf, UltronFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UltronFxPayload::kind,
            ByteBufCodecs.VAR_INT, UltronFxPayload::entityId,
            PayloadCodecs.VEC3, UltronFxPayload::at,
            PayloadCodecs.VEC3, UltronFxPayload::to,
            ByteBufCodecs.FLOAT, UltronFxPayload::radius,
            ByteBufCodecs.VAR_INT, UltronFxPayload::ticks,
            UltronFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The Ultron lane draws the effect here. */
    static void handle(UltronFxPayload payload, IPayloadContext context) {
    }
}
