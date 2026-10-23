package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.CerebroView;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → client, to Professor X alone (3.3), once a second while Cerebro searches: the minds it found, wherever they
 * are in his dimension (far beyond what his client knows of the world), for the markers and the list on his screen.
 * 3.7: amplified at the lab's Cerebro console (lane L5) it searches every dimension (each mind carries its own) and
 * marks the nearest Infinity Stone sites of his.
 *
 * @param minds     what was found, at most {@link #MAX}, the nearest first
 * @param ticksLeft how much longer Cerebro searches
 * @param amplified whether it is the lab's amplified Cerebro (3.7)
 * @param sites     the stone sites it marks (3.7), at most {@link #MAX_SITES}
 */
public record CerebroPayload(List<Mind> minds, int ticksLeft, boolean amplified, List<Site> sites)
        implements CustomPacketPayload {
    /** The most minds in one message. */
    public static final int MAX = 32;
    /** The most stone sites in one message (one of each kind). */
    public static final int MAX_SITES = 6;

    /** Cerebro of 3.3: no stone sites, not amplified. */
    public CerebroPayload(List<Mind> minds, int ticksLeft) {
        this(minds, ticksLeft, false, List.of());
    }

    /**
     * One mind: a player's name, their hero (a {@code Power} ordinal), where they stand, and (3.7) the dimension they
     * are in ({@code minecraft:the_nether}; empty: his own).
     */
    public record Mind(String name, int power, Vec3 pos, String dimension) {
        /** A mind in his own dimension (3.3). */
        public Mind(String name, int power, Vec3 pos) {
            this(name, power, pos, "");
        }

        static final StreamCodec<ByteBuf, Mind> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Mind::name,
                ByteBufCodecs.VAR_INT, Mind::power,
                PayloadCodecs.VEC3, Mind::pos,
                ByteBufCodecs.stringUtf8(128), Mind::dimension,
                Mind::new);
    }

    /** One Infinity Stone site (3.7): the stone's kind (a {@code StoneKind} ordinal) and where its site stands. */
    public record Site(int kind, BlockPos pos) {
        static final StreamCodec<ByteBuf, Site> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Site::kind,
                PayloadCodecs.BLOCK_POS, Site::pos,
                Site::new);
    }

    public static final Type<CerebroPayload> TYPE = new Type<>(Enhanced.id("cerebro"));

    public static final StreamCodec<ByteBuf, CerebroPayload> STREAM_CODEC = StreamCodec.composite(
            Mind.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), CerebroPayload::minds,
            ByteBufCodecs.VAR_INT, CerebroPayload::ticksLeft,
            ByteBufCodecs.BOOL, CerebroPayload::amplified,
            Site.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SITES)), CerebroPayload::sites,
            CerebroPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client. */
    static void handle(CerebroPayload payload, PacketContext context) {
        CerebroView.accept(payload);
    }
}
