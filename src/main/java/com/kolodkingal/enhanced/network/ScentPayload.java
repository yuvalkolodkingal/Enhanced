package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.wolverine.Scents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client, to Wolverine alone (3.7, lane L2): what his nose tells him. The creatures his Sniff found glow
 * through walls in their scent's colour ({@link Scent}), and the scent trails near him (the last minute of where
 * players and monsters walked, or Track's one trail) are drawn as faint drifting motes for him only.
 *
 * @param scents what he smells, at most {@link #MAX_SCENTS}
 * @param trails the trails, at most {@link #MAX_TRAILS} of at most {@link #MAX_TRAIL_POINTS} points
 * @param ticks  how long the client shows them
 */
public record ScentPayload(List<Scent> scents, List<Trail> trails, int ticks) implements CustomPacketPayload {
    public static final int MAX_SCENTS = 64;
    public static final int MAX_TRAILS = 8;
    public static final int MAX_TRAIL_POINTS = 16;
    /** The kinds of a scent (its colour). */
    public static final byte MONSTER = 0;
    public static final byte ANIMAL = 1;
    public static final byte VILLAGER = 2;
    public static final byte PLAYER = 3;
    public static final byte POISONED = 4;
    /** The one creature Track follows. */
    public static final byte TRACKED = 5;

    /** One creature he smells: its entity id and its kind (a constant of {@link ScentPayload}). */
    public record Scent(int entityId, byte kind) {
        static final StreamCodec<ByteBuf, Scent> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Scent::entityId,
                ByteBufCodecs.BYTE, Scent::kind,
                Scent::new);
    }

    /** One trail: the points of a creature's last walk, the oldest first. */
    public record Trail(List<Vec3> points) {
        static final StreamCodec<ByteBuf, Trail> STREAM_CODEC = StreamCodec.composite(
                PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_TRAIL_POINTS)), Trail::points,
                Trail::new);
    }

    public static final Type<ScentPayload> TYPE = new Type<>(Enhanced.id("scent"));

    public static final StreamCodec<ByteBuf, ScentPayload> STREAM_CODEC = StreamCodec.composite(
            Scent.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SCENTS)), ScentPayload::scents,
            Trail.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TRAILS)), ScentPayload::trails,
            ByteBufCodecs.VAR_INT, ScentPayload::ticks,
            ScentPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client. */
    static void handle(ScentPayload payload, IPayloadContext context) {
        Scents.receive(payload);
    }
}
