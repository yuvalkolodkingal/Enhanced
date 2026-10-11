package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client: one effect of Thanos (the boss or the hero) that everyone nearby should see, for a few ticks: a ring
 * on the ground that tells where a blow will land, a beam, a portal, a falling star, a dome, chains. The client draws
 * it for a short while.
 *
 * @param kind     what to draw (the Thanos lane lists the kinds)
 * @param color    its colour ({@code 0xRRGGBB}, -1 for the banded halo of the five stones)
 * @param entityId the caster, for where its hand is, or the victim; or -1
 * @param ticks    how long it lasts, or the time to impact
 * @param radius   its size
 * @param points   where (the meaning depends on the kind), at most 16
 */
public record ThanosFxPayload(int kind, int color, int entityId, int ticks, float radius, List<Vec3> points)
        implements CustomPacketPayload {
    public static final Type<ThanosFxPayload> TYPE = new Type<>(Enhanced.id("thanos_fx"));

    public static final StreamCodec<ByteBuf, ThanosFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThanosFxPayload::kind,
            ByteBufCodecs.INT, ThanosFxPayload::color,
            ByteBufCodecs.VAR_INT, ThanosFxPayload::entityId,
            ByteBufCodecs.VAR_INT, ThanosFxPayload::ticks,
            ByteBufCodecs.FLOAT, ThanosFxPayload::radius,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(16)), ThanosFxPayload::points,
            ThanosFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The Thanos lane draws the effect here. */
    static void handle(ThanosFxPayload payload, IPayloadContext context) {
    }
}
