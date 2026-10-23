package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.MagnetStreams;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → Magneto and everyone who sees him: what one tick of his Magnetic Pull (Sneak + R holding nothing, 3.1) took,
 * so that every client draws each thing flying to his hand as its item, along a line of force (D-M 8.2). One payload per
 * tick of a pull that took something, and a start-only one when the first tick took nothing yet, so the field's pulse
 * plays at once. About 28 bytes a stream and at most {@link #MAX_STREAMS}: under 2 KiB.
 *
 * <p>Only what comes to him has a stream: a body tugged, an illusion dissolved, gear that crumbled or a piece dropped at a
 * player's feet send none (they are seen where they happen).
 *
 * @param magneto his entity id (the streams fly to his hand)
 * @param first   whether this is the pull's first payload: the clients play the field's pulse, growing to {@code radius}
 * @param radius  the pull's radius
 * @param streams what was taken this tick, at most {@link #MAX_STREAMS}
 */
public record MagnetPullPayload(int magneto, boolean first, float radius, List<Stream> streams) implements CustomPacketPayload {
    public static final int MAX_STREAMS = 64;

    /**
     * One thing taken.
     *
     * @param from  where it starts: the middle of the block or of the creature it came from
     * @param item  what flies: for an ore its metal, for a block the block, for a creature the first piece of gear it gave
     * @param count how many little icons fly in a row, 1 to 3
     */
    public record Stream(Vec3 from, Item item, int count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Stream> STREAM_CODEC = StreamCodec.composite(
                PayloadCodecs.VEC3, Stream::from,
                ByteBufCodecs.registry(Registries.ITEM), Stream::item,
                ByteBufCodecs.VAR_INT, Stream::count,
                Stream::new);
    }

    public static final Type<MagnetPullPayload> TYPE = new Type<>(Enhanced.id("magnet_pull"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagnetPullPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MagnetPullPayload::magneto,
            ByteBufCodecs.BOOL, MagnetPullPayload::first,
            ByteBufCodecs.FLOAT, MagnetPullPayload::radius,
            Stream.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_STREAMS)), MagnetPullPayload::streams,
            MagnetPullPayload::new);

    public MagnetPullPayload {
        streams = List.copyOf(streams);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The pulse and the streams are drawn here (the client class is loaded only when this runs, on a client). */
    static void handle(MagnetPullPayload payload, PacketContext context) {
        MagnetStreams.accept(payload);
    }
}
