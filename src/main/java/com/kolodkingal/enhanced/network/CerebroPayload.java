package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.CerebroView;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client, to Professor X alone (3.3), once a second while Cerebro searches: the minds it found, wherever they
 * are in his dimension (far beyond what his client knows of the world), for the markers and the list on his screen.
 *
 * @param minds     what was found, at most {@link #MAX}, the nearest first
 * @param ticksLeft how much longer Cerebro searches
 */
public record CerebroPayload(List<Mind> minds, int ticksLeft) implements CustomPacketPayload {
    /** The most minds in one message. */
    public static final int MAX = 32;

    /** One mind: a player's name, their hero (a {@code Power} ordinal) and where they stand. */
    public record Mind(String name, int power, Vec3 pos) {
        static final StreamCodec<ByteBuf, Mind> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Mind::name,
                ByteBufCodecs.VAR_INT, Mind::power,
                PayloadCodecs.VEC3, Mind::pos,
                Mind::new);
    }

    public static final Type<CerebroPayload> TYPE = new Type<>(Enhanced.id("cerebro"));

    public static final StreamCodec<ByteBuf, CerebroPayload> STREAM_CODEC = StreamCodec.composite(
            Mind.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), CerebroPayload::minds,
            ByteBufCodecs.VAR_INT, CerebroPayload::ticksLeft,
            CerebroPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client. */
    static void handle(CerebroPayload payload, IPayloadContext context) {
        CerebroView.accept(payload.minds(), payload.ticksLeft());
    }
}
