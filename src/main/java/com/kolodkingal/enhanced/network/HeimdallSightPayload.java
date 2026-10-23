package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.HeimdallSightView;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Server → client (3.5): what Heimdall's All-Seeing Sight shows of the players out of his client's sight (every 10 ticks
 * while it is open, to him alone): in his dimension at any distance, and (with {@code heimdall.seeOtherRealms}) in the others.
 *
 * @param players at most {@link #MAX_PLAYERS}
 */
public record HeimdallSightPayload(List<Far> players) implements CustomPacketPayload {
    public static final int MAX_PLAYERS = 32;

    /**
     * One far player.
     *
     * @param dimension the id of his dimension
     * @param pos       where he stands
     */
    public record Far(UUID id, String name, ResourceLocation dimension, Vec3 pos) {
        public static final StreamCodec<ByteBuf, Far> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Far::id,
                ByteBufCodecs.stringUtf8(64), Far::name,
                ResourceLocation.STREAM_CODEC, Far::dimension,
                PayloadCodecs.VEC3, Far::pos,
                Far::new);
    }

    public static final Type<HeimdallSightPayload> TYPE = new Type<>(Enhanced.id("heimdall_sight"));

    public static final StreamCodec<ByteBuf, HeimdallSightPayload> STREAM_CODEC = StreamCodec.composite(
            Far.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PLAYERS)), HeimdallSightPayload::players,
            HeimdallSightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HeimdallSightPayload payload, PacketContext context) {
        HeimdallSightView.receive(payload);
    }
}
