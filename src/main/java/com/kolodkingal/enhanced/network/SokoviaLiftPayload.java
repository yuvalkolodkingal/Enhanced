package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: Sokovia begins to rise, or to come down, or the lift is over. No block data travels in it (about 60
 * bytes): each client reads the island's cells from the world it already has, with the same shape the server uses, and
 * draws it rising. A client that starts tracking the fight in the middle is sent the current one.
 *
 * @param phase     {@link #RISE}, {@link #LOWER} or {@link #END}
 * @param bossId    the entity id of the key the island's clients track
 * @param centre    the middle of the island, on the ground
 * @param radius    its radius
 * @param height    how far it rises
 * @param coneDepth how deep the rock cone under it is at its middle
 * @param seed      the fight's seed: the rim and the cone come out the same on every client
 * @param ticks     how long the rise or the fall takes
 */
public record SokoviaLiftPayload(int phase, int bossId, BlockPos centre, int radius, int height, int coneDepth, long seed, int ticks)
        implements CustomPacketPayload {
    public static final int RISE = 0;
    public static final int LOWER = 1;
    public static final int END = 2;

    public static final Type<SokoviaLiftPayload> TYPE = new Type<>(Enhanced.id("sokovia_lift"));

    public static final StreamCodec<ByteBuf, SokoviaLiftPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.phase);
                ByteBufCodecs.VAR_INT.encode(buf, p.bossId);
                PayloadCodecs.BLOCK_POS.encode(buf, p.centre);
                ByteBufCodecs.VAR_INT.encode(buf, p.radius);
                ByteBufCodecs.VAR_INT.encode(buf, p.height);
                ByteBufCodecs.VAR_INT.encode(buf, p.coneDepth);
                ByteBufCodecs.VAR_LONG.encode(buf, p.seed);
                ByteBufCodecs.VAR_INT.encode(buf, p.ticks);
            },
            buf -> new SokoviaLiftPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    PayloadCodecs.BLOCK_POS.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The Ultron lift lane builds and draws the island here. */
    static void handle(SokoviaLiftPayload payload, IPayloadContext context) {
    }
}
