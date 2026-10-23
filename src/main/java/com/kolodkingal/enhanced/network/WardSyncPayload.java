package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.WardFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Server → client: the awake wards of one dimension (sent to whoever is in it when they change, and to a player who logs in,
 * changes dimension or respawns), so the client can draw their domes. Replaces what the client knew of that dimension.
 */
public record WardSyncPayload(ResourceKey<Level> dimension, List<Dome> domes) implements CustomPacketPayload {
    /** The most wards one dimension sends (a hundred players with two each). */
    public static final int MAX_DOMES = 256;

    public static final Type<WardSyncPayload> TYPE = new Type<>(Enhanced.id("ward_sync"));

    /** One ward as the client draws it. */
    public record Dome(double x, double y, double z, int radius, int color) {
        public static final StreamCodec<ByteBuf, Dome> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Dome::x,
                ByteBufCodecs.DOUBLE, Dome::y,
                ByteBufCodecs.DOUBLE, Dome::z,
                ByteBufCodecs.VAR_INT, Dome::radius,
                ByteBufCodecs.INT, Dome::color,
                Dome::new);
    }

    public static final StreamCodec<ByteBuf, WardSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), WardSyncPayload::dimension,
            Dome.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_DOMES)), WardSyncPayload::domes,
            WardSyncPayload::new);

    public WardSyncPayload {
        domes = List.copyOf(domes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on a client, so the client class is only ever loaded there. */
    static void handle(WardSyncPayload payload, PacketContext context) {
        WardFx.accept(payload);
    }
}
