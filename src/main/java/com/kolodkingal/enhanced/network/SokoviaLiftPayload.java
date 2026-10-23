package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.sokovia.SokoviaClient;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: Sokovia begins to rise, or to come down, or its move is over. No block data travels in it (about 70
 * bytes): each client reads the island's cells from the world it already has, with the same shape the server uses, and
 * draws it moving. A client that starts tracking the fight in the middle is sent the current state.
 *
 * <ul>
 *     <li>{@link #RISE}: the island is copied from the ground at {@code centre} and rises {@code height} blocks over
 *     {@code ticks} ticks, the first of them at the game time {@code start}; with {@code hide}, the client also hides
 *     the ground the island was copied from (a picture only: the server's ground never changes).</li>
 *     <li>{@link #LOWER}: the island in the sky comes down the same way. It is sent when the descent begins, with a
 *     {@code start} that is far in the future (the island hangs where it is while the server takes down its blocks),
 *     and again with the real {@code start} when it begins to fall.</li>
 *     <li>{@link #END}: the move is over and the mesh goes. With {@code hide} the island is up and the hidden ground
 *     stays hidden; without it, the ground is as it was and everything is shown again.</li>
 * </ul>
 *
 * @param phase     {@link #RISE}, {@link #LOWER} or {@link #END}
 * @param bossId    the entity id of the Vibranium Key that stands in the middle of the island and rises with it, or -1
 * @param centre    the middle of the island, on the ground (the block above the clicked one)
 * @param radius    its radius
 * @param height    how far it rises
 * @param coneDepth how deep the rock cone under it is at its middle
 * @param seed      the fight's seed: the rim and the cone come out the same on every client
 * @param ticks     how long the rise or the fall takes
 * @param skin      how many layers of ground it carries
 * @param cover     how far above the ground it carries things (grass, trees)
 * @param hide      see above
 * @param start     the game time the move begins (the first tick of it)
 */
public record SokoviaLiftPayload(int phase, int bossId, BlockPos centre, int radius, int height, int coneDepth, long seed, int ticks,
                                 int skin, int cover, boolean hide, long start) implements CustomPacketPayload {
    public static final int RISE = 0;
    public static final int LOWER = 1;
    public static final int END = 2;

    /** The ground's default depth and cover, for a payload that does not say. */
    private static final int DEFAULT_SKIN = 5;
    private static final int DEFAULT_COVER = 16;

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
                ByteBufCodecs.VAR_INT.encode(buf, p.skin);
                ByteBufCodecs.VAR_INT.encode(buf, p.cover);
                ByteBufCodecs.BOOL.encode(buf, p.hide);
                ByteBufCodecs.VAR_LONG.encode(buf, p.start);
            },
            buf -> new SokoviaLiftPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    PayloadCodecs.BLOCK_POS.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)));

    /** A move that starts when it is received, with the ground's default depth and cover, and hiding the ground. */
    public SokoviaLiftPayload(int phase, int bossId, BlockPos centre, int radius, int height, int coneDepth, long seed, int ticks) {
        this(phase, bossId, centre, radius, height, coneDepth, seed, ticks, DEFAULT_SKIN, DEFAULT_COVER, true, 0L);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The island is drawn here (the client class is loaded only when this runs, on a client). */
    static void handle(SokoviaLiftPayload payload, PacketContext context) {
        SokoviaClient.receive(payload);
    }
}
