package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.ThorEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client: a moment of Thor's power everyone nearby should see. The client draws it for a short while.
 *
 * @param kind     what happened ({@link #ARC}, {@link #BIFROST_ARRIVAL}, {@link #BIFROST_MARK})
 * @param entityId Thor, or -1
 * @param points   where (the meaning depends on the kind)
 */
public record ThorFxPayload(int kind, int entityId, List<Vec3> points) implements CustomPacketPayload {
    /** Lightning arcs along {@code points}, from one to the next. */
    public static final int ARC = 0;
    /** The Bifrost's beam of light lands on {@code points[0]}. */
    public static final int BIFROST_ARRIVAL = 1;
    /** The Bifrost's circle of runes is burned into the ground at {@code points[0]}. */
    public static final int BIFROST_MARK = 2;

    public static final Type<ThorFxPayload> TYPE = new Type<>(Enhanced.id("thor_fx"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    public static final StreamCodec<ByteBuf, ThorFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThorFxPayload::kind,
            ByteBufCodecs.VAR_INT, ThorFxPayload::entityId,
            VEC3.apply(ByteBufCodecs.list(16)), ThorFxPayload::points,
            ThorFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ThorFxPayload payload, IPayloadContext context) {
        ThorEffects.add(payload);
    }
}
