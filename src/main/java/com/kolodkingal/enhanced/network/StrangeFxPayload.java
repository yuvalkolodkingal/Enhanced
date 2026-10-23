package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.strange.StrangeFx;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server → client (3.6): a spell of Doctor Strange everyone near should see (lane L3's): the whip, the bands, the
 * shields, the portals, the Astral Projection, the Mirror Dimension. The client hands each kind to the drawer lane L3
 * registered ({@code StrangeFx.register}).
 *
 * @param kind     what happened (a constant of this record)
 * @param entityId the caster, or -1
 * @param points   where (the meaning depends on the kind), at most {@link #MAX_POINTS}
 * @param arg      a number (the meaning depends on the kind)
 */
public record StrangeFxPayload(int kind, int entityId, List<Vec3> points, int arg) implements CustomPacketPayload {
    /** At most this many points (the payload stays under 256 bytes). */
    public static final int MAX_POINTS = 16;
    public static final int WHIP = 0;
    public static final int BANDS = 1;
    public static final int SHIELD = 2;
    public static final int DOME = 3;
    public static final int PORTAL_OPEN = 4;
    public static final int PORTAL_CLOSE = 5;
    public static final int ASTRAL_OUT = 6;
    public static final int ASTRAL_IN = 7;
    public static final int MIRROR_ON = 8;
    public static final int MIRROR_OFF = 9;

    public static final Type<StrangeFxPayload> TYPE = new Type<>(Enhanced.id("strange_fx"));

    public static final StreamCodec<ByteBuf, StrangeFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StrangeFxPayload::kind,
            ByteBufCodecs.VAR_INT, StrangeFxPayload::entityId,
            PayloadCodecs.VEC3.apply(ByteBufCodecs.list(MAX_POINTS)), StrangeFxPayload::points,
            ByteBufCodecs.VAR_INT, StrangeFxPayload::arg,
            StrangeFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(StrangeFxPayload payload, PacketContext context) {
        StrangeFx.receive(payload);
    }
}
